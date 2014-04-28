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
 * Tests the AssetClass controller
 */
@RunWith(classOf[JUnitRunner])
class AssetClassSpec extends Specification with TestUser{
  "AssetClass" should {
    "render the Asset Class view - Shares" in new WithApplication(currentApplication){
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

      val result = controllers.AssetClass.index("Shares")(FakeRequest().withCookies(sessionCookies))

      status(result.run) must equalTo(OK)
      contentAsString(result.run) must contain("Sail - Shares")
      contentAsString(result.run) must contain(testUser.username)
    }

    "generate a time series for the Shares asset class" in new WithApplication(currentApplication){
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

      val result = route(
        FakeRequest(GET, "/service/class/Shares").withCookies(sessionCookies)
      )

      contentAsJson(result.get).\\("name").seq.size must be_>(0)
      contentAsString(result.get) must find("name.*y")
    }

    "generate a list of investment values at a date - Shares" in new WithApplication(currentApplication){
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

      val result = route(
        FakeRequest(GET, "/service/onedate/Shares/27-04-2014").withCookies(sessionCookies)
      )

      contentAsJson(result.get).\\("name").seq.size must be_>(0)
      contentAsString(result.get) must find("name.*value.*quantity")
    }
  }
}
