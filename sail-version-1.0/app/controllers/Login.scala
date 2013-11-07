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
import models.User
import views.html
import org.joda.time.{Days, DateTime}
import play.api.Play.current
import play.api.Play
import play.Logger

/**
 * Controller for the Login flow.
 * Holds the form and logic to log the user in.
 * Holds the Security trait which is used by the root flow to ensure the user
 * is authenticated
 */
object Login extends Controller {

  /**login form used on the Login screen to gather details */
  val loginForm = Form(
    tuple(
      "email" -> text,
      "password" -> text
    ) verifying ("Invalid email or password", result => result match {
      case (email, password) => User.authenticate(email, password).isDefined
    })
  )

  /**
   * Routes to the login index page and passes the login form to it
   *
   * @return    Result directing the flow to the Login page including the login form
   */
  def index = Action { implicit request =>
    Ok(html.login(loginForm))
  }

  /**
   * Takes the login attempt from the Login page, bound to the loginForm
   * and retrieves the user.
   * The user's email is then mapped to the security username cookie along
   * along with the login date time.
   *
   * @return    Result redirecting the user back to the root application index
   */
  def authenticate = Action { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.login(formWithErrors)),
      user => Redirect(routes.Application.index).withSession(Security.username -> user._1, "connected" -> DateTime.now.toString())
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
      "success" -> "You are now logged out."
    )
  }
}


/**
 * Implements the Secured trait from the Play api to allow
 * secure authorisations and sessions through cookies
 */
trait Secured extends Controller{

  /** The time limit from last login until the user should be timed out */
  val inactivityLimit: Double = Play.application.configuration.getString("user.timeout.milli").getOrElse(Play.application.configuration.getString("user.timeout.days").getOrElse("5")).toDouble

  /**
   * Gets the email address from the current session cookie
   * To be used in authentication and by the Play api for the
   * Security  trait
   *
   * @param request   RequestHeader sent by the browser.
   *                  Holds the cookie to be accessed
   * @return          String the email address from the cookie
   */
  def email(request: RequestHeader) = request.session.get(Security.username)

  /**
   * Redirects the flow to the Login page when the user is not authorised
   * Used by the Play api so the Secured trait knows what to do when
   * unauthorised
   *
   * @param request   RequestHeader sent by the browser
   * @return          Result redirecting the flow to the Login page
   */
  def onUnauthorized(request: RequestHeader) = Results.Redirect(routes.Login.index)

  /**
   * Authorisation method used to check the cookie in the request and ensure it holds
   * the email address specified. When the cookie has been tampered with, the unauthorised
   * function is run.
   * Checks for timeout in the cookie by comparing the current time against it
   *
   * @param f     Function to run when authentication is confirmed
   * @return      Result specified in the Function parameter f
   *              Will change when user has timed out to redirect the
   *              flow to the Login page.
   * @see         #hasTimedOut(Request[AnyContent], timeout)
   */
  def withAuth(f: => String => Request[AnyContent] => Result) = {
    Security.Authenticated(email, onUnauthorized) { user =>
      Action(request => {
          hasTimedOut(request, inactivityLimit).getOrElse({
            f(user)(request)
          })
      })
    }
  }

  /**
   * Authorisation function used to wrap the withAuth function to get a handle on the user object
   * dictated by the email address in the cookie.
   * Makes use of the findByEmail function of the User class to return a full User object
   * from MongoDB
   *
   * @param f     Function to run when authentication is confirmed
   * @return      Result specified in the Function parameter f
   *              Will change when user has timed out to redirect the
   *              flow to the Login page.
   * @see         #withAuth
   * @see         User#findByEmail(String)
   */
  def withUser(f: User => Request[AnyContent] => Result) = withAuth { email => implicit request =>
    User.findByEmail(email).map { user =>
      f(user)(request)
    }.getOrElse(onUnauthorized(request))
  }

  /**
   * Checks the incoming request to ensure the user has logged in within the previous 'x' days, where
   * 'x' is the inactivityLimit value.
   *
   * @param request   Request[AnyContent] the incoming request
   * @param timeout   Double the time in days until the user should be timed out
   * @return          Option[Result] result redirecting the user to the login page
   *                  when timed out
   * @see             #inactivityLimit
   */
  def hasTimedOut (request:Request[AnyContent], timeout:Double):Option[Result]= {
    request.session.get("connected").map ({
      connected =>
        if (Play.application.configuration.getString("user.timeout.milli").isDefined) {
          /** We are in dev or test */
          if (DateTime.now().getMillisOfDay.-(DateTime.parse(connected).getMillisOfDay).>(timeout)) {
          val inactivityString = "You have been inactive for over "+timeout.toInt+" milli seconds"
          return Option(Redirect(routes.Login.index).withNewSession.flashing("Timeout" -> inactivityString))
          }
        }
        else if (Days.daysBetween(DateTime.parse(connected).toDateMidnight() , DateTime.now().toDateMidnight()).getDays().>(timeout)) {
          val inactivityString = "You have been inactive for over "+timeout+" days"
          return Option(Redirect(routes.Login.index).withNewSession.flashing("Timeout" -> inactivityString))
        }
    })
    return None
  }
}
