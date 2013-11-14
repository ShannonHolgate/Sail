/*
 * Copyright (c) 2013. Shannon Holgate.
 *
 * Sail - A personal fund management web application.
 *
 * Makes use of the Play web framework for scala, MongoDB and the salat-Play plugin
 */

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import play.api.libs.json.Json._
import play.api.test.{FakeApplication, FakeRequest}
import play.api.test.Helpers._
import play.api.test._
import play.i18n.Messages

/**
 * Tests the register controller
 */
@RunWith(classOf[JUnitRunner])
class RegisterSpec extends Specification with TestUser with controllers.Secured{

  "Register" should {
    "respond to the index Action" in new WithApplication(currentApplication){
      val result = controllers.Register.index()(FakeRequest())

      status(result) must equalTo(OK)
      contentAsString(result) must contain("register")
    }

    "respond to the index Action - User logged in" in new WithApplication(currentApplication){
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

      val result = controllers.Register.index()(FakeRequest().withCookies(sessionCookies))
      status(result) must equalTo(SEE_OTHER)
      redirectLocation(result) must some("/")
    }

    "respond to the register action" in new WithApplication(currentApplication){
      /** Ensure Test User does NOT exist */
      removeTestUsers

      /** The Json form object*/
      val jsonObject = toJson(
        Map(
          "email" -> toJson(testUser.email),
          "name" -> toJson(testUser.username),
          "password" -> toJson(testUser.password)
        )
      )
      val result= controllers.Register.register()(FakeRequest().withJsonBody(jsonObject))

      /** Store the cookies */
      val sessionCookies = cookies(result).get("PLAY_SESSION").orNull

      status(result) must equalTo(SEE_OTHER)
      sessionCookies.value must find("mail.{1,4}com.*"+configValues.timeoutSession)
      redirectLocation(result) must some("/")
    }

    "respond to the register action - user already exists" in new WithApplication(currentApplication){
      /** Ensure Test User DOES exist */
      confirmTestUserExists

      /** The Json form object*/
      val jsonObject = toJson(
        Map(
          "email" -> toJson(testUser.email),
          "name" -> toJson(testUser.username),
          "password" -> toJson(testUser.password)
        )
      )
      val result= controllers.Register.register()(FakeRequest().withJsonBody(jsonObject))

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must contain(Messages.get("error.user.exists"))
    }
  }
}
