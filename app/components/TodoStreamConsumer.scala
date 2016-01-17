package components

import java.nio.charset.Charset
import javax.inject.{Inject, Singleton}

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink}
import com.spingo.op_rabbit.Directives._
import com.spingo.op_rabbit.stream.RabbitSource
import com.spingo.op_rabbit.{ConnectionParams, Delivery, RabbitControl}
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

  private final val queueName = "todo-notification-queue"

  private val utf8 = Charset.forName("UTF-8")

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

  private val deserializer = Flow[String].map(Json.parse).map(_.as[TodoEvent])

  val streamConsumingActor = actorSystem.actorOf(Props(new StreamConsumingActor))

  val sink: Sink[TodoEvent, _] = Sink.actorRef[TodoEvent](streamConsumingActor, "")

  val flow = source.acked via deserializer to sink
  logger.info("Starting the flow...")
  flow.run()

  def register(userId: Long, actorRef: ActorRef) = {
    streamConsumingActor ! Register(userId, actorRef)
  }

  def unregister(userId: Long, actorRef: ActorRef) = {
    streamConsumingActor ! Unregister(userId, actorRef)
  }

  case class Register(userId: Long, out: ActorRef)
  case class Unregister(userId: Long, out: ActorRef)

  class StreamConsumingActor extends Actor {

    val outputs = new mutable.HashMap[Long, List[ActorRef]]

    override def receive = {

      case Register(userId, out) =>
        val registered = outputs.getOrElse(userId, List.empty)
        outputs += userId -> (registered :+ out)
        logger.info(s"Registered new listener for $userId")

      case Unregister(userId, out) =>
        val registered = outputs.getOrElse(userId, List.empty)
        outputs += userId -> registered.filterNot(_ == out)
        logger.info(s"Unregistered listener for $userId")

      case TodoEvent(userId, Some(todo)) =>
        logger.info(s"New todo detected: $todo")

        outputs.getOrElse(userId, List.empty).foreach {
          _ ! todo
        }

      case unknown => logger.warn(s"Unknown message: $unknown")
    }
  }
}
