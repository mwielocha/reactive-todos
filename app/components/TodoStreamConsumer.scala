package components

import java.nio.charset.Charset
import javax.inject.{Inject, Singleton}

import akka.actor.Actor.Receive
import akka.actor._
import akka.cluster.sharding.{ClusterShardingSettings, ClusterSharding, ShardRegion}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink}
import com.spingo.op_rabbit.Directives._
import com.spingo.op_rabbit.stream.RabbitSource
import com.spingo.op_rabbit.{ConnectionParams, Delivery, RabbitControl}
import com.timcharper.acked.AckedFlow
import logging.LoggingComponent
import model.TodoEvent
import play.api.Configuration
import play.api.libs.json.Json

import scala.collection.mutable
import scala.concurrent.ExecutionContext

/**
 * Created by mwielocha on 04/06/15.
 */
@Singleton
class TodoStreamConsumer @Inject()(val configuration: Configuration,
                                   val todoRepository: TodoRepository)
                                  (implicit val actorSystem: ActorSystem, val ec: ExecutionContext) extends LoggingComponent {


  val region = ClusterSharding(actorSystem).start(
    typeName = "userManager",
    entityProps = Props[UserManager],
    settings = ClusterShardingSettings(actorSystem),
    extractEntityId = UserManager.extractEntityId,
    extractShardId = UserManager.extractShardId
  )

  private final val queueName = "todo-notification-queue"

  logger.info(s"Declaring queue $queueName")

  val rabbitControl = actorSystem.actorOf(Props(
    classOf[RabbitControl],
    ConnectionParams.fromConfig(
      configuration.underlying.getConfig("amqp"))
  ))

  val source = RabbitSource(
    rabbitControl,
    channel(qos = 3),
    consume(queue(
      queue = queueName,
      durable = true,
      exclusive = false,
      autoDelete = false)),
    body(as[String])
  )

  implicit val materializer = ActorMaterializer()

  private val deserializer = AckedFlow[String].map(Json.parse).map(_.as[TodoEvent])

  class MessageForwarderSink extends Actor with ActorLogging {
    override def receive: Receive = {
      case m => log.info(s"Stream: $m"); region.forward(m)
    }
  }

  val sink: Sink[TodoEvent, _] = Sink.actorRef(actorSystem.actorOf(Props(new MessageForwarderSink)), "")

  val flow = (source via deserializer).acked to sink
  logger.info("Starting the flow...")
  flow.run()

}

object UserManager {

  case class Register(userId: Long, out: ActorRef)
  case class Unregister(userId: Long, out: ActorRef)

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case m@Register(id, _) => id.toString -> m
    case m@Unregister(id, _) => id.toString -> m
    case e@TodoEvent(id, _) => id.toString -> e
  }

  val numberOfShards = 100

  val extractShardId: ShardRegion.ExtractShardId = {
    case Register(id, m) => (id % numberOfShards).toString
    case Unregister(id, m) => (id % numberOfShards).toString
    case e@TodoEvent(id, _) => (id % numberOfShards).toString
  }
}

class UserManager extends Actor with ActorLogging {

  val outputs = new mutable.HashMap[Long, List[ActorRef]]

  import UserManager._

  override def receive = {

    case Register(userId, out) =>
      val registered = outputs.getOrElse(userId, List.empty)
      outputs += userId -> (registered :+ out)
      log.info(s"Registered new listener for $userId")

    case Unregister(userId, out) =>
      val registered = outputs.getOrElse(userId, List.empty)
      outputs += userId -> registered.filterNot(_ == out)
      log.info(s"Unregistered listener for $userId")

    case TodoEvent(userId, Some(todo)) =>
      log.info(s"New todo detected: $todo")

      outputs.getOrElse(userId, List.empty).foreach {
        _ ! todo
      }

    case unknown => log.warning(s"Unknown message: $unknown")
  }
}
