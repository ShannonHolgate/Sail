/*
 * Copyright (c) 2013. Shannon Holgate.
 *
 * Sail - A personal fund management web application.
 *
 * Makes use of the Play web framework for scala, MongoDB and the salat-Play plugin
 */

package controllers

import play.api.mvc.{Action, Security, Controller}
import play.api.data._
import play.api.data.Forms._
import models.User
import views.html
import org.joda.time.DateTime

/**
 * Controller for the Register flow.
 * Holds the form and login to register a new user
 */
object Register extends Controller{

  /** Register form used on the Register page to gather registration information*/
  val registerForm = Form(
    tuple(
      "username" -> text,
      "email" -> text,
      "password" -> text
    ) verifying ("User Already exists", result => result match {
      case (username,email,password) => User.findByEmail(email).isEmpty
    })
  )

  /**
   * Routes to the Register index page and passes the register form to it
   *
   * @return    Result directing the flow to the Register page including the register form
   */
  def index = Action { implicit request =>
    Ok(html.register(registerForm)).withNewSession
  }

  /**
   * Takes the registration attempt from the Registration page, bound to the registerForm
   * The user's email is then mapped to the security username cookie along
   * along with the login date time.
   *
   * @return    Result redirecting the user back to the root application index
   */
  def register = Action { implicit request =>
    registerForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.register(formWithErrors)),
      user => {
        User.create(user._1,user._2,user._3)
        Redirect(routes.Application.index).withSession(Security.username -> user._2, "connected" -> DateTime.now.toString())
      }
    )
  }
}

