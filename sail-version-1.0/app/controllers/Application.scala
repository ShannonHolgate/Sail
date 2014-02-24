/*
 * Copyright (c) 2013. Shannon Holgate.
 *
 * Sail - A personal fund management web application.
 *
 * Makes use of the Play web framework for scala, MongoDB and the salat-Play plugin
 */

package controllers

import play.api.mvc._
import play.api.Play
import models.User
import org.joda.time.{DateTime, Days}
import play.api.Play.current
import play.i18n.Messages
import play.Logger

/**
 * Landing route controller.
 * Should hold little logic and send user to the next
 * controller where the flow will continue.
 *
 * Holds the Security trait which is used by the root flow to ensure the user
 * is authenticated
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
      val username = user.name
      /** Update the connected session cookie */
      Redirect("/dash").withSession(session - configValues.timeoutSession + (configValues.timeoutSession -> DateTime.now().toString()))
    }
  }
}

/**
 * Implements the Secured trait from the Play api to allow
 * secure authorisations and sessions through cookies
 */
trait Secured extends Controller{

  object configValues {
    /** The time limit from last login until the user should be timed out */
    val inactivityLimit: Int = Play.application.configuration.getString("user.timeout.days").get.toInt

    /** The key to get the timeout date */
    val timeoutSession: String = Play.application.configuration.getString("user.timeout.connected").get

    /** The flash key to map the timeout message */
    val timeoutFlash: String = Play.application.configuration.getString("timeout.flash").get

    /** The flash key to map the invalid user message */
    val invalidUser: String = Play.application.configuration.getString("user.invalid.flash").get

    /** The flash key to map the successful logout message */
    val logoutSuccess: String = Play.application.configuration.getString("user.logout.flash").get

    /** The flash key used to map the successful reset username */
    val resetSuccess: String = Play.application.configuration.getString("user.reset.flash").get

    /** The flash key used to map the reset request email */
    val resetRequest: String = Play.application.configuration.getString("user.reset.request").get

    /** The flash key used to map the generic errors*/
    val genericError: String = Play.application.configuration.getString("generic.error").get

    /** The flash key used to map the generic success */
    val genericSuccess: String = Play.application.configuration.getString("generic.success").get
  }

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
        hasTimedOut(request, configValues.inactivityLimit).getOrElse({
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
   * Checks the incoming request to ensure the user has been active in within the previous 'x' days, where
   * 'x' is the inactivityLimit value.
   *
   * @param request   Request[AnyContent] the incoming request
   * @param timeout   Double the time in days until the user should be timed out
   * @return          Option[Result] result redirecting the user to the login page
   *                  when timed out
   * @see             #inactivityLimit
   */
  def hasTimedOut (request:Request[AnyContent], timeout:Int):Option[Result]= {
    request.session.get(configValues.timeoutSession).map ({
      connected =>
        if (Days.daysBetween(DateTime.parse(connected), DateTime.now()).getDays.>(timeout)) {
          val inactivityString = Messages.get("error.user.inactive",timeout.toString)
          return Option(Redirect(routes.Login.index).withNewSession.flashing( configValues.timeoutFlash -> inactivityString))
        }
    })
    return None
  }

  /**
   * Ensures a logged in User cannot access the Login or Register pages before being logged out
   * Tries to get the email address from the session cookie to confirm whether the user is logged in
   * Then ensures the user has not timed out
   *
   * @param request Request[AnyContent] the request used to try and access the login/register page
   * @param result  Result the result expected when the user is NOT logged in
   * @return        Result the result redirecting the user to the application index or
   *                the Result expected in the parameter list
   */
  def withLoginRestriction (request:Request[AnyContent], result:Result):Result = {

    /** Get the email address from the request cookie */
    val email:Option[String] = request.session.get(Security.username)

    /** If an email address exists in the cookie, ensure it is valid */
    if (email.isDefined) {
      /** Get the user object from the MongoDB */
      val userObject:Option[User] = User.findByEmail(email.get)
      /** Check if the user exists in the MongoDB */
      if (userObject.isDefined) {
        /** The user exists in the MongoDB, check the user has not timed out */
        hasTimedOut(request,configValues.inactivityLimit).getOrElse({
          Redirect(routes.Application.index)
        })
      }
      else
        /** No user was found on the MongoDB */
        result.flashing(configValues.invalidUser -> Messages.get("error.user.invalid"))
    }
    else
      /** No email exists in the session, User is not logged in */
      result
  }
}