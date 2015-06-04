package components

import javax.inject.{Singleton, Inject}

import akka.actor.{Props, ActorRef, Actor}
import akka.actor.Actor.Receive
import akka.stream.ActorFlowMaterializer
import akka.stream.actor.ActorSubscriber
import akka.stream.scaladsl.{Sink, Source, Flow}
import io.scalac.amqp.{Queue, Connection, Delivery}
import logging.LoggingComponent
import play.api.Configuration
import model.TodoEvent
import play.api.libs.json.Json
import play.libs.Akka
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable

/**
 * Created by mwielocha on 04/06/15.
 */
@Singleton
class TodoStreamConsumer @Inject() (configuration: Configuration, todoRepository: TodoRepository) extends LoggingComponent {

  private final val exchangeName = "todo-notification-exchange"
  private final val queueName = "todo-notification-queue"

  final val queue = Queue(queueName, durable = true)

  val connection = Connection(configuration.underlying)

  logger.info(s"Declaring queue $queueName")
  connection.queueDeclare(queue).flatMap { _ =>
    connection.queueBind(queueName, exchangeName, "")
  }

  implicit val actorSystem = Akka.system
  implicit val materializer = ActorFlowMaterializer(None, Some(exchangeName))

  private val dejsonizer = {
    Flow[Delivery].map { delivery =>
      val message = delivery.message.body.utf8String
      logger.info(s"New delivery: $message")
      Json.parse(message).as[TodoEvent]
    }
  }

  val streamConsumingActor = actorSystem.actorOf(Props(new StreamConsumingActor))

  val sink = Sink.actorRef(streamConsumingActor, "")

  val source = Source(connection.consume(queueName))

  val flow = source via dejsonizer to sink
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

      case unknow => logger.info(s"Unknown message: $unknow")
    }
  }
}
