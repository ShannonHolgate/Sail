/*
 * Copyright (c) 2013. Shannon Holgate.
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
import play.i18n.Messages

/**
 * Tests the TargetFund controller
 */
class TargetFundSpec extends Specification with TestUser{

  "TargetFund" should {

    "bad request when no target fund is available" in new WithApplication(currentApplication){
      /** Ensure user exists */
      removeTestUsers
      confirmTestUserExists

      /** Remove target fund */
      removeTargetFund

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
        FakeRequest(GET, "/service/targetfund").withCookies(sessionCookies)
      )
      status(result.get) must equalTo(BAD_REQUEST)
    }

    "render the Target Fund view - no target fund - Redirect to questions" in new WithApplication(currentApplication){
      /** Ensure user exists */
      removeTestUsers
      confirmTestUserExists

      /** Remove target fund */
      removeTargetFund

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

      val result = controllers.TargetFund.index()(FakeRequest().withCookies(sessionCookies))

      status(result.run) must equalTo(SEE_OTHER)
      redirectLocation(result.run) must some("/riskappetite")
    }

    "render the Target Fund view - with fund and target" in new WithApplication(currentApplication){
      /** Ensure user exists */
      removeTestUsers
      confirmTestUserExists
      removeTargetFund
      removeRiskAppetite

      /** add the target fund and risk appetite*/
      addTargetFund
      addRiskAppetite

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

      val result = controllers.TargetFund.index()(FakeRequest().withCookies(sessionCookies))

      status(result.run) must equalTo(OK)
      contentAsString(result.run) must contain("Target Fund")
      contentAsString(result.run) must contain(testUser.username)
    }

    "render the Target Fund view - no fund with target" in new WithApplication(currentApplication){
      /** Ensure user exists */
      removeTestUsers
      removeTargetFund
      removeRiskAppetite

      /** add the target fund and risk appetite*/
      addTargetFund
      addRiskAppetite
      createStandaloneUser

      /** The Json form object*/
      val jsonObject = toJson(
        Map(
          "email" -> toJson(standalone.email),
          "password" -> toJson(standalone.password)
        )
      )
      val login= controllers.Login.authenticate()(FakeRequest().withJsonBody(jsonObject))

      /** Store the cookies to pass onto the next request */
      val sessionCookies = cookies(login).get("PLAY_SESSION").orNull

      val result = controllers.TargetFund.index()(FakeRequest().withCookies(sessionCookies))

      status(result.run) must equalTo(OK)
      contentAsString(result.run) must contain("Target Fund")
      contentAsString(result.run) must contain(Messages.get("view.dash.nofund"))
      contentAsString(result.run) must contain(standalone.username)
    }
  }
}