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
import play.api.test.{Helpers, FakeRequest, WithApplication}
import play.api.test.Helpers._
import play.api.libs.json.Json._

/**
 * Tests the Investment controller
 */
class InvestmentSpec extends Specification with TestUser with controllers.Secured{

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

    "get a list of ticker symbols for a query" in new WithApplication(currentApplication){
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

      val result = controllers.Investment.getTickerSymbolService("google")(FakeRequest().withCookies(sessionCookies))

      contentType(result.run) must beSome("text/plain")
      contentAsJson(result.run).\\("symbol").seq.size must be_>(0)
      contentAsString(result.run) must find("symbol.*GOOG")
    }

    "get a list of shares for the user" in new WithApplication(currentApplication){
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

      val result = controllers.Investment.getInvestmentList("Shares")(FakeRequest().withCookies(sessionCookies))

      contentType(result.run) must beSome("application/json")
      contentAsJson(result.run).\\("symbol").seq.size must be_>(0)
      contentAsString(result.run) must find("id.*name.*symbol.*quantity.*value")
    }

    "add an automated investment" in new WithApplication(currentApplication){
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

      /** Get the JKS investment if it exists so we can remove it in prep of the test */
      val jKSInvestment = models.Investment.getOneFromSymbol("JKS",models.User.findByEmail(testUser.email).get)

      if (jKSInvestment.isDefined) {
        /** Json to remove the test investment if it exists */
        val jsonRemoveObject = toJson(
          Map(
            "id" -> toJson(jKSInvestment.get.id.toString),
            "removeallbool" -> toJson(true),
            "password" -> toJson(testUser.password)
          )
        )
        val RemoveResult = controllers.Investment.remove()(FakeRequest().withCookies(sessionCookies).withJsonBody(jsonRemoveObject))
      }

      /** Json auto investment form */
      val jsonAutoObject = toJson(
        Map(
          "investmentresults" -> toJson("JKS~JinkoSolar Holding Co., Ltd."),
          "quantity" -> toJson(15),
          "assetclass" -> toJson("Shares")
        )
      )

      val result = controllers.Investment.addAuto()(FakeRequest().withCookies(sessionCookies).withJsonBody(jsonAutoObject))

      status(result.run) must beEqualTo(SEE_OTHER)
      Helpers.flash(result.run).data must haveKey(configValues.genericSuccess)
    }
  }

}
