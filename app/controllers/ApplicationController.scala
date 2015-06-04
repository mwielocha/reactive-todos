package controllers

import play.api.mvc.{Action, Controller}

/**
 * Created by mwielocha on 04/06/15.
 */
trait ApplicationController {
  requires: Controller =>

  def index = Action {
    Ok("Hi")
  }
}

object ApplicationController extends Controller with ApplicationController