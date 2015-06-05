package controllers

import java.util.UUID
import javax.inject.{Singleton, Inject}
import akka.actor.Actor.Receive
import akka.actor.{Props, ActorRef, Actor}
import model.Todo
import play.api.Logger

import scala.concurrent.ExecutionContext.Implicits.global

import components.{TodoStreamConsumer, TodoRepository}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{WebSocket, Controller, Action}

import scala.concurrent.Future
import play.api.Play.current

/**
 * Created by mwielocha on 04/06/15.
 */
@Singleton
class TodosController @Inject() (todoRepository: TodoRepository, todoStreamConsumer: TodoStreamConsumer) extends Controller {

  def list(userId: Long) = Action.async {
    todoRepository.findAll(userId, 100).map {
      todos => Ok(Json.toJson(todos))
    }
  }

  def add(userId: Long) = Action.async(parse.json) { implicit request =>
    todoRepository.addOrUpdate(userId, request.body.as[Todo]).map {
      todo => Ok(Json.toJson(todo))
    }
  }

  def done(userId: Long, id: UUID) = Action.async(parse.empty) { implicit request =>
    Logger.info("UUID v: " + id.version())
    todoRepository.find(userId, id).flatMap {
      case None => Future.successful(NotFound)
      case Some(previous) =>
        todoRepository.addOrUpdate(userId, previous.copy(done = true)).map {
          updated => Ok(Json.toJson(updated))
        }
    }
  }

  def delete(userId: Long, id: UUID) = Action.async(parse.empty) { implicit request =>
    todoRepository.delete(userId, id).flatMap {
      _ => Future.successful(Ok)
    }
  }

  def socket(userId: Long) = WebSocket.acceptWithActor[JsValue, JsValue] { request => out =>
    Props(new WebSocketActor(userId, out))
  }

  class WebSocketActor(userId: Long, out: ActorRef) extends Actor {

    override def receive: Receive = {
      case todo: Todo =>
        out ! Json.toJson(todo)
    }

    override def preStart(): Unit = {
      todoStreamConsumer.register(userId, self)
    }

    override def postStop(): Unit = {
      todoStreamConsumer.unregister(userId, self)
    }
  }
}
