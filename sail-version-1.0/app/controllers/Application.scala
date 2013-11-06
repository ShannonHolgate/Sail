/*
 * Copyright (c) 2013. Shannon Holgate.
 *
 * Sail - A personal fund management web application.
 *
 * Makes use of the Play web framework for scala, MongoDB and the salat-Play plugin
 */

package controllers

import play.api.mvc._

/**
 * Landing route controller.
 * Should hold little logic and send user to the next
 * controller where the flow will continue.
 *
 * Makes use of the Secured trait to ensure the user only has access
 * to the correct controller
 *
 * @see         Login#Secured
 */
object Application extends Controller with Secured {

  /**
   * Ensures the user is logged in by checking the cookie
   * then routing to the appropriate page
   *
   * @return    Result of the cookie check
   *            will route to the index screen if authorised or
   *            route to the login index screen if not
   * @see       Login
   */
  def index = withUser {
    user => implicit request => {
      val username = user.username
      Ok(views.html.index(username))
    }
  }
}