/*
 * Copyright (c) 2014. Shannon Holgate.
 *
 * Sail - A personal fund management web application.
 *
 * Makes use of the Play web framework for scala, MongoDB and the salat-Play plugin
 */

package Controllers

import org.specs2.mutable.Specification
import test_data.TestUser
import play.api.test.{FakeRequest, WithApplication}
import play.api.libs.json.Json._
import play.api.test.Helpers._

/**
 * Tests the Risk Appetite controller
 */
class RiskAppetiteSpec extends Specification with TestUser{

  "RiskAppetite" should {

    "render the Risk Questionnaire" in new WithApplication(currentApplication){
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

      val result = controllers.RiskAppetite.index()(FakeRequest().withCookies(sessionCookies))

      status(result.run) must equalTo(OK)
      contentAsString(result.run) must contain("Risk")
      contentAsString(result.run) must contain(testUser.username)
    }
  }
}
