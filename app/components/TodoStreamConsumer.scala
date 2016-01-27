package components

import java.nio.charset.Charset
import javax.inject.{Inject, Singleton}

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink}
import com.spingo.op_rabbit.Binding._
import com.spingo.op_rabbit.Directives._
import com.spingo.op_rabbit.stream.RabbitSource
import com.spingo.op_rabbit.{Exchange, ConnectionParams, Delivery, RabbitControl}
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

  val rabbitControl = actorSystem.actorOf(Props(
    classOf[RabbitControl],
    ConnectionParams.fromConfig(
      configuration.underlying.getConfig("amqp"))
  ))

  val source = RabbitSource(
    rabbitControl,
    channel(),
    consume(fanout(queue("", exclusive = true),
      Exchange.fanout("todo-notification-exchange"))),
    body(as[String])
  )

  implicit val materializer = ActorMaterializer()


}
