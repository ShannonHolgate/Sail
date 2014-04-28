/*
 * Copyright (c) 2013. Shannon Holgate.
 *
 * Sail - A personal fund management web application.
 *
 * Makes use of the Play web framework for scala, MongoDB and the salat-Play plugin
 */

package controllers

import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._
import org.joda.time._
import play.api.{mvc, Play}
import models.User
import views.html
import play.i18n.Messages
import helpers._

/**
 * Controller for the Login flow.
 * Holds the form and logic to log the user in.
 */
object  Login extends Controller with Secured with Mailer{

  /** login form used on the Login screen to gather details */
  val loginForm = Form(
    tuple(
      "email" -> text,
      "password" -> text
    ) verifying (Messages.get("error.login.invalid"), result => result match {
      case (email, password) => User.authenticate(email, password).isDefined
    })
  )

  /** reset password form used on the Login screen */
  val resetRequestForm = Form(
    single(
      "email" -> text
    )verifying (Messages.get("error.user.none"), result => result match {
      case (email) => User.findByEmail(email).isDefined
    })
  )

  /**
   * Routes to the login index page and passes the login form to it
   * Makes user of the Login Restriction to ensure the user is not logged in
   *
   * @return    Result directing the flow to the Login page including the login form
   */
  def index = Action{ implicit request =>
    withLoginRestriction(request, Ok(html.login(loginForm,resetRequestForm)).flashing(request.flash))
  }

  /**
   * Takes the login attempt from the Login page, bound to the loginForm
   * and retrieves the user.
   * The user's email is then mapped to the security username cookie along
   * along with the login date time.
   *
   * @return    Result redirecting the user back to the root application index
   */
  def authenticate(date:String=DateTime.now().toString()) = Action { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.login(formWithErrors,resetRequestForm)),
      user => Redirect(routes.Application.index).withSession(Security.username -> user._1,
        configValues.timeoutSession -> date)
    )
  }

  /**
   * Creates a new session by deleting the play cookie
   * then adds a new flash session to the http request
   * to notify the user to logging out
   *
   * @return    Result returning the user to the login html page
   */
  def logout = Action {
    Redirect(routes.Login.index).withNewSession.flashing(
      configValues.logoutSuccess -> Messages.get("success.logout")
    )
  }

  /**
   * Requests a password reset for the email entered
   * Binds the form to ensure the email address exists
   * Updates the user object on the MongoDB to hold a new unique password
   * reset key then refreshes the page with confirmation in flash scope
   *
   * @return    Result refreshing the pages with confirmation or the form with
   *            errors
   * @see       User#requestReset(email:String)
   */
  def requestReset = Action { implicit request =>
    resetRequestForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.login(loginForm,formWithErrors)),
      email => {
        val (key,expire,name) = User.requestReset(email)
        if (key.isEmpty || expire.isEmpty || name.isEmpty) {
          /** In the very strange event of an ObjectId not being distinct, throw an error */
          val formWithErrors = resetRequestForm.fill(email).withGlobalError(Messages.get("error.user.none"))
          BadRequest(html.login(loginForm,formWithErrors))
        }
        else {
          /** Send the reset email to the user */
          sendResetEmail(email,name.get,key.get,expire.get, request)
          Redirect(routes.Login.index).flashing(configValues.resetRequest -> email)
        }
      }
    )
  }
}
