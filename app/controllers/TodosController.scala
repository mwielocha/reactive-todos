package controllers

import java.util.UUID
import javax.inject.{Singleton, Inject}
import model.Todo

import scala.concurrent.ExecutionContext.Implicits.global

import components.TodoRepository
import play.api.libs.json.Json
import play.api.mvc.{Controller, Action}

import scala.concurrent.Future

/**
 * Created by mwielocha on 04/06/15.
 */
@Singleton
class TodosController @Inject() (todoRepository: TodoRepository) extends Controller {

  def list(userId: Long) = Action.async {
    todoRepository.findUndoneByUser(userId, 10).map {
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
}
