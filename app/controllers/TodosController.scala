package controllers

import java.util.UUID
import javax.inject.{Singleton, Inject}
import akka.actor.Actor.Receive
import akka.actor.{Props, ActorRef, Actor}
import model.Todo

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
    todoRepository.findUndoneByUser(userId, 100).map {
      todos => Ok(Json.toJson(todos))
    }
  }

  def add(userId: Long) = Action.async(parse.json) { implicit request =>
    todoRepository.addOrUpdate(userId, request.body.as[Todo]).map {
      todo => Ok(Json.toJson(todo))
    }
  }

  def done(userId: Long, id: UUID) = Action.async(parse.empty) { implicit request =>
    todoRepository.find(userId, id, done = false).flatMap {

      case Some(todo) =>
        for {
          _ <- todoRepository.delete(userId, todo.id.get, todo.done)
          updated <- todoRepository.addOrUpdate(userId, todo.copy(done = true))
        } yield Ok(Json.toJson(updated))

      case None => Future.successful(NotFound)
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
