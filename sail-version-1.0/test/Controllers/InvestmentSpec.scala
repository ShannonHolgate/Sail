/*
 * Copyright (c) 2013. Shannon Holgate.
 *
 * Sail - A personal fund management web application.
 *
 * Makes use of the Play web framework for scala, MongoDB and the salat-Play plugin
 */

package Controllers

import org.specs2.mutable.Specification
import test_data.TestUser
import play.api.test.{FakeRequest, WithApplication}
import play.api.test.Helpers._
import play.api.libs.json.Json._

/**
 * Tests the Investment controller
 */
class InvestmentSpec extends Specification with TestUser{

  "Investment" should {

    "get real Time JSON values and update value in db" in new WithApplication(currentApplication){
      /** Ensure user exists */
      removeTestUsers
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

      val result = route(
        FakeRequest(GET, "/service/realtimeinvestments").withCookies(sessionCookies)
      )

      contentAsJson(result.get).\\("symbol").seq.size must be_>(0)
      contentAsString(result.get) must find("symbol.*value.*quantity.*price.*exchange")
    }
  }

}
