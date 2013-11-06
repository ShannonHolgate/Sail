package controllers

import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._
import models.User
import views.html
import org.joda.time.{Days, DateTime}

/**
 * Created with IntelliJ IDEA.
 * User: Shannon
 * Date: 05/11/13
 * Time: 22:48
 */

/**
 * Controller for the Login flow.
 * Holds the login details is used in by the User class to log the user in.
 * Holds the Security trait which is used by the root flow to ensure the user
 * is authenticated
 */
object Login extends Controller {

  //login form used on the Login screen to gather details
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
   * @return    the html result including the login form
   */
  def index = Action { implicit request =>
    Ok(html.login(loginForm))
  }

  /**
   * Takes the login attempt from the Login screen, bound to the loginForm
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

trait Secured extends Controller{

  def email(request: RequestHeader) = request.session.get(Security.username)

  def onUnauthorized(request: RequestHeader) = Results.Redirect(routes.Login.index)

  def withAuth(f: => String => Request[AnyContent] => Result) = {
    Security.Authenticated(email, onUnauthorized) { user =>
      Action(request => {
        if (hasTimedOut(request)) {
          Redirect(routes.Login.index).withNewSession.flashing("Timeout" -> "You have been inactive for over 5 days")
        }
        else f(user)(request)
      })
    }
  }

  def withUser(f: User => Request[AnyContent] => Result) = withAuth { email => implicit request =>
    User.findByEmail(email).map { user =>
      f(user)(request)
    }.getOrElse(onUnauthorized(request))
  }

  def hasTimedOut (request:Request[AnyContent]):Boolean= {
    request.session.get("connected").map ({
      connected =>
      //DateTime.now().getMillisOfDay.-(DateTime.parse(connected).getMillisOfDay).>(500)
        Days.daysBetween(DateTime.parse(connected).toDateMidnight() , DateTime.now().toDateMidnight()).getDays().>(5)
    }).getOrElse(true)
  }
}