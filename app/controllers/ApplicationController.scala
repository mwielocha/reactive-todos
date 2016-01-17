package controllers

import javax.inject.Inject

import components.TodoRepository
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import uuid.UUIDHelper

import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by mwielocha on 04/06/15.
 */
class ApplicationController @Inject() (todoRepository: TodoRepository) extends Controller {

  def index(userId: Long) = Action.async { implicit request =>
    todoRepository.findAll(userId, 1000).map { todos =>
      Ok(views.html.Application.todos(userId, todos))
    }
  }

  def uuid(count: Option[Int]) = Action {
    Ok(Json.toJson {
      (0 until count.getOrElse(1)).map {
        _ => UUIDHelper.newTimeUUID
      }
    })
  }
}