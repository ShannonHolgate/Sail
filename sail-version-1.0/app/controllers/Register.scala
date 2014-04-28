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
import org.joda.time.DateTime
import models.User
import views.html
import play.i18n.Messages
import helpers._

/**
 * Controller for the Register flow.
 * Holds the form and login to register a new user
 */
object Register extends Controller with Secured with Mailer{

  /** Register form used on the Register page to gather registration information*/
  val registerForm = Form(
    tuple(
      "name" -> text,
      "email" -> text,
      "password" -> text
    ) verifying (Messages.get("error.user.exists"), result => result match {
      case (name,email,password) => User.findByEmail(email).isEmpty
    }) verifying(Messages.get("error.email.invalid"),result => result._2 match {
      case (email) => email.contains('@')
    }) verifying(Messages.get("error.name.length",3.toString),result => result._1 match {
      case (name) => name.length.>(3)
    })
  )

  /**
   * Routes to the Register index page and passes the register form to it
   * Makes user of the Login Restriction to ensure the user is not logged in
   *
   * @return    Result directing the flow to the Register page including the register form
   */
  def index = Action { implicit request =>
    withLoginRestriction(request, Ok(html.register(registerForm)).withNewSession)
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
        sendRegisteredEmail(user._2,user._1, request)
        Redirect(routes.Application.index).withSession(Security.username -> user._2,
          configValues.timeoutSession -> DateTime.now.toString())
      }
    )
  }
}

