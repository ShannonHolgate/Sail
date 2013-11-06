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
 * @see       Secured
 */
object Application extends Controller with Secured {

  /**
   * Ensures the user is logged in by checking the cookie
   * then routing to the appropriate page
   *
   * @return    the Result of the cookie check
   *            will route to the index screen if authorised or
   *            route to the login index screen if not
   * @see       models.Login
   */
  def index = withUser {
    user => implicit request => {
      val username = user.username
      Ok(views.html.index(username))
    }
  }
}