/*
 * Copyright (c) 2014. Shannon Holgate.
 *
 * Sail - A personal fund management web application.
 *
 * Makes use of the Play web framework for scala, MongoDB and the salat-Play plugin
 */

package Controllers

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.mutable.Specification
import test_data.TestUser
import play.api.test.{FakeRequest, WithApplication}
import play.api.libs.json.Json._
import play.api.test.Helpers._

/**
 * Tests the Dashboard Controller
 */
@RunWith(classOf[JUnitRunner])
class DashboardSpec extends Specification with TestUser{
  "Dashboard" should {
    "Render the Dashboard view" in new WithApplication(currentApplication){
      /** Ensure user exists */
      confirmTestUserExists

      /** The Json form object*/
      val jsonObject = toJson(
        Map(
          "email" -> toJson(testUser.email),
          "password" -> toJson(testUser.password)
        )
      )
      val login= controllers.Login.authenticate()(FakeRequest().withJsonBody(jsonObject))

      /** Store the cookies to pass onto the next request */
      val sessionCookies = cookies(login).get("PLAY_SESSION").orNull

      val result = controllers.Dashboard.index()(FakeRequest().withCookies(sessionCookies))

      status(result.run) must equalTo(OK)
      contentAsString(result.run) must contain("Dashboard")
      contentAsString(result.run) must contain(testUser.username)
    }

    "Redirect to login - user not logged in" in new WithApplication(currentApplication){
      /** Attempt to render the Dashboard view */
      val result = controllers.Dashboard.index()(FakeRequest())

      status(result.run) must equalTo(SEE_OTHER)
      redirectLocation(result.run) must some("/login")
    }
  }
}
