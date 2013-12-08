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
import play.api.libs.json.Json._
import play.api.test.Helpers._
import play.Logger

class InvestmentHistorySpec extends Specification with TestUser{

  "InvestmentHistory" should {

    "get time series of investments for asset class" in new WithApplication(currentApplication){
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
        FakeRequest(GET, "/service/timeseries").withCookies(sessionCookies)
      )

      Logger.info(contentAsString(result.get))
      contentAsJson(result.get).\\("date").seq.size must be_>(0)
      contentAsString(result.get) must find("date.*value")
    }
  }
}
