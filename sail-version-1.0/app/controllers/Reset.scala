/*
 * Copyright (c) 2013. Shannon Holgate.
 *
 * Sail - A personal fund management web application.
 *
 * Makes use of the Play web framework for scala, MongoDB and the salat-Play plugin
 */

package controllers

import play.api.mvc._
import models.User
import play.api.data.Forms._
import views.html
import play.api.data.Form
import play.i18n.Messages

/**
 * Controller for the Reset Password flow.
 * Holds the form and logic to reset the users password.
 */
object Reset extends Controller with Secured{

  /** Reset password form verifying the email address and that the new passwords match */
  val resetForm= Form(
    tuple(
      "email" -> text,
      "password" -> text,
      "confirm" -> text
    ) verifying (Messages.get("error.email.incorrect"),result => result._1 match {
      case (email) => User.resetRequested(email)
    }) verifying(Messages.get("error.password.nomatch"),result => result match {
      case (email,password,confirm) => {
        password.equals(confirm)
      }
    })
  )

  /**
   * Routes to the reset index page and passes the reset password form to it
   *
   * @param key   String the password reset token from the url
   * @return      Result redirecting the user home if the key has expired
   *              or rendering the reset page
   */
  def index(key: String) = Action{ implicit request =>
    redirectHome(key).getOrElse({
      Ok(views.html.reset(resetForm,key)).withNewSession
    })
  }

  /**
   * Changes the password for a user
   * Takes the form and the key from the url to match against a user on
   * the MongoDB, then updates the password and removes the password reset key
   *
   * @param key   String the password reset token from the url
   * @return      Result redirecting the user back to the login page with success message
   *              or refreshes the reset page with the form error
   * @see         User#resetPassword(email:String, password:String, key:String)
   */
  def reset(key: String) = Action{ implicit request =>
    resetForm.bindFromRequest.fold(
      formWithErrors => {
        BadRequest(html.reset(formWithErrors,key))
      },
      reset => {
        val resetResult = User.resetPassword(reset._1,reset._2,key)
        val username:Option[String] = resetResult._1
        if (username.isDefined) Results.Redirect(routes.Login.index).flashing(configValues.resetSuccess -> username.get)
        else {
          /** The email address does not match the url key */
          val formWithErrors = resetForm.fill(reset).withGlobalError(resetResult._2.getOrElse(Messages.get("error.email.incorrect")))
          BadRequest(html.reset(formWithErrors,key))
        }
      }
    )
  }

  /**
   * Redirects the user to the home page when the url key has expired
   *
   * @param key   String the password reset token from the url
   * @return      Option[Result] either redirecting the user to the home page
   *              or letting the user continue
   * @see         User#findForPasswordReset(key:String)
   */
  def redirectHome(key: String):Option[Result] = {
    User.findForPasswordReset(key).getOrElse({
      /** The key has either expired or does not exist */
      return Option(Redirect(routes.Application.index))
    })
    None
  }
}
