package Controllers

/*
 * Copyright (c) 2013. Shannon Holgate.
 *
 * Sail - A personal fund management web application.
 *
 * Makes use of the Play web framework for scala, MongoDB and the salat-Play plugin
 */

import org.junit.runner._
import org.specs2.mutable._
import org.specs2.runner._
import play.api.libs.json.Json._
import play.api.test.Helpers._
import play.api.test._
import play.api.test.WithApplication
import org.joda.time.DateTime
import play.i18n.Messages
import test_data.TestUser

/**
 * Tests the login controller
 */
@RunWith(classOf[JUnitRunner])
class LoginSpec extends Specification with TestUser with controllers.Secured{

  "Login" should {

    "respond to the index Action" in  new WithApplication(currentApplication) {
      val result = controllers.Login.index()(FakeRequest())

      status(result) must equalTo(OK)
      contentAsString(result) must contain("login")
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

      val result = controllers.Login.index()(FakeRequest().withCookies(sessionCookies))
      status(result) must equalTo(SEE_OTHER)
      redirectLocation(result) must some("/")
    }

    "respond to the index Action - User logged in plus Timeout" in new WithApplication(currentApplication){
      /** Ensure user exists */
      confirmTestUserExists

      /** The Json form object*/
      val jsonObject = toJson(
        Map(
          "email" -> toJson(testUser.email),
          "password" -> toJson(testUser.password)
        )
      )

      /** Run the authenticate action and set the connected date a week in the past
        * This allows the timeout to be reached by the next request
        */
      val login= controllers.Login.authenticate(DateTime.now().minusWeeks(1).toString())(FakeRequest().withJsonBody(jsonObject))

      /** Store the cookies to pass onto the next request */
      val sessionCookies = cookies(login).get("PLAY_SESSION").orNull

      val result = controllers.Login.index()(FakeRequest().withCookies(sessionCookies))
      status(result) must equalTo(SEE_OTHER)
      Helpers.flash(result).data must haveKey(configValues.timeoutFlash)
      redirectLocation(result) must some("/login")
    }

    "respond to the authenticate Action" in new WithApplication(currentApplication){
      /** Ensure user exists */
      confirmTestUserExists

      /** The Json form object*/
      val jsonObject = toJson(
        Map(
          "email" -> toJson(testUser.email),
          "password" -> toJson(testUser.password)
        )
      )
      val result= controllers.Login.authenticate()(FakeRequest().withJsonBody(jsonObject))

      /** Store the cookies to pass onto the next request */
      val sessionCookies = cookies(result).get("PLAY_SESSION").orNull

      status(result) must equalTo(SEE_OTHER)
      sessionCookies.value must find("mail.{1,4}com.*"+configValues.timeoutSession)
      redirectLocation(result) must some("/")
    }

    "respond to the authenticate Action - User non-existent" in new WithApplication(currentApplication){

      /** The Json form object*/
      val jsonObject = toJson(
        Map(
          "email" -> toJson("user@fakemail.com"),
          "password" -> toJson(testUser.password)
        )
      )
      val result= controllers.Login.authenticate()(FakeRequest().withJsonBody(jsonObject))

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must contain(Messages.get("error.login.invalid"))
    }

    "respond to the logout Action via route" in new WithApplication{
      val result = route(FakeRequest(GET,"/logout")).get

      status(result) must equalTo(SEE_OTHER)
      Helpers.flash(result).data must haveKey(configValues.logoutSuccess)
      redirectLocation(result) must some("/login")
    }

    "respond to the authenticate Action with timeout" in new WithApplication(currentApplication) {
      /** Ensure user exists */
      confirmTestUserExists

      /** The Json form object*/
      val jsonObject = toJson(
        Map(
          "email" -> toJson(testUser.email),
          "password" -> toJson(testUser.password)
        )
      )

      /** Run the authenticate action and set the connected date a week in the past
        * This allows the timeout to be reached by the next request
        */
      val authenticate= controllers.Login.authenticate(DateTime.now().minusWeeks(1).toString())(FakeRequest(POST,"/login").withJsonBody(jsonObject))

      /** Store the cookies to pass onto the next request */
      val sessionCookies = cookies(authenticate).get("PLAY_SESSION").orNull

      val result = route(FakeRequest(GET,"/").withCookies(sessionCookies)).get
      status(result) must equalTo(SEE_OTHER)
      Helpers.flash(result).data must haveKey(configValues.timeoutFlash)
      redirectLocation(result) must some("/login")
    }

    "respond to the password reset request Action" in new WithApplication(currentApplication) {
      /** Ensure user exists */
      confirmTestUserExists

      /** The Json form object*/
      val jsonObject = toJson(
        Map(
          "email" -> toJson(testUser.email)
        )
      )

      val result = controllers.Login.requestReset()(FakeRequest(POST,"/reset").withJsonBody(jsonObject))
      status(result) must equalTo(SEE_OTHER)
      Helpers.flash(result).data must haveKey(configValues.resetRequest)
      redirectLocation(result) must some("/login")
      //TODO Test that email was received and contains valid key, expiry date and user name
    }

    "respond to the password reset request Action - User does not exist" in new WithApplication(currentApplication) {
      /** The Json form object*/
      val jsonObject = toJson(
        Map(
          "email" -> toJson("user@fakemail.com")
        )
      )

      val result = controllers.Login.requestReset()(FakeRequest(POST,"/reset").withJsonBody(jsonObject))
      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must contain(Messages.get("error.user.none"))
    }
  }
}