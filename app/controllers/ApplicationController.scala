package controllers

import javax.inject.Inject

import components.TodoRepository
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.mvc.{Action, Controller}

/**
 * Created by mwielocha on 04/06/15.
 */
class ApplicationController @Inject() (todoRepository: TodoRepository) extends Controller {
  requires: Controller =>

  def index(userId: Long) = Action.async { implicit request =>
    todoRepository.findAll(userId, 1000).map { todos =>
      Ok(views.html.Application.todos(userId, todos))
    }
  }
}