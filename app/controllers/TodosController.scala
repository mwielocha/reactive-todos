package controllers

import java.util.UUID
import javax.inject.{Inject, Singleton}

import akka.actor.{Actor, ActorRef, Props}
import components.{TodoRepository, TodoStreamConsumer}
import model.Todo
import play.api.Logger
import play.api.Play.current
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, Controller, WebSocket}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Created by mwielocha on 04/06/15.
 */
@Singleton
class TodosController @Inject() (todoRepository: TodoRepository, todoStreamConsumer: TodoStreamConsumer) extends Controller {

  def list(userId: Long) = Action.async {
    Future.successful(???)
  }

  def add(userId: Long) = Action.async(parse.json) { implicit request =>
    Future.successful(???)
  }

  def done(userId: Long, id: UUID) = Action.async(parse.empty) { implicit request =>
    Future.successful(???)
  }

  def delete(userId: Long, id: UUID) = Action.async(parse.empty) { implicit request =>
    Future.successful(???)
  }

  def socket(userId: Long) = WebSocket.acceptWithActor[JsValue, JsValue] { implicit request => out =>
    ???
  }
}
