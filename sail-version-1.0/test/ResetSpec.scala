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
import play.api.test.Helpers._
import play.api.test._
import play.api.test.{FakeRequest, WithApplication}
import play.i18n.Messages

/**
 * Tests the reset controller
 */
@RunWith(classOf[JUnitRunner])
class ResetSpec extends Specification with TestUser with controllers.Secured{

  "Reset" should {
    "respond to the index Action" in  new WithApplication(currentApplication) {
      val (stringKey,stringKey2) = createPasswordResetUsers

      val result = controllers.Reset.index(stringKey)(FakeRequest())
      status(result) must equalTo(OK)
      contentAsString(result) must contain("confirm")
    }

    "respond to the index Action - invalid reset key" in  new WithApplication(currentApplication) {

      val result = controllers.Reset.index("1111111111111")(FakeRequest())
      status(result) must equalTo(SEE_OTHER)
      redirectLocation(result) must some("/")
    }

    "respond to the reset Action" in  new WithApplication(currentApplication) {
      val (stringKey,stringKey2) = createPasswordResetUsers

      /** The Json form object*/
      val jsonObject = toJson(
        Map(
          "email" -> toJson(testUser.email),
          "password" -> toJson(testUser.password),
          "confirm" -> toJson(testUser.password)
        )
      )

      val result = controllers.Reset.reset(stringKey)(FakeRequest().withJsonBody(jsonObject))
      status(result) must equalTo(SEE_OTHER)
      redirectLocation(result) must some("/login")
      Helpers.flash(result).data must haveKey(configValues.resetSuccess)
    }

    "respond to the reset Action - passwords don't match" in  new WithApplication(currentApplication) {
      val (stringKey,stringKey2) = createPasswordResetUsers

      /** The Json form object*/
      val jsonObject = toJson(
        Map(
          "email" -> toJson(testUser.email),
          "password" -> toJson(testUser.password),
          "confirm" -> toJson("notPassword")
        )
      )

      val result = controllers.Reset.reset(stringKey)(FakeRequest().withJsonBody(jsonObject))
      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must contain(Messages.get("error.password.nomatch"))
    }

    "respond to the reset Action - email does not exist" in  new WithApplication(currentApplication) {
      val (stringKey,stringKey2) = createPasswordResetUsers

      /** The Json form object*/
      val jsonObject = toJson(
        Map(
          "email" -> toJson("doesNotExist@mail.com"),
          "password" -> toJson(testUser.password),
          "confirm" -> toJson(testUser.password)
        )
      )

      val result = controllers.Reset.reset(stringKey)(FakeRequest().withJsonBody(jsonObject))
      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must contain(Messages.get("error.email.incorrect"))
    }

    "respond to the reset Action - email matches a different reset key" in  new WithApplication(currentApplication) {
      val (stringKey,stringKey2) = createPasswordResetUsers

      /** The Json form object*/
      val jsonObject = toJson(
        Map(
          "email" -> toJson(testUser2.email),
          "password" -> toJson(testUser2.password),
          "confirm" -> toJson(testUser2.password)
        )
      )

      val result = controllers.Reset.reset(stringKey)(FakeRequest().withJsonBody(jsonObject))
      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must contain(Messages.get("error.email.incorrect"))
    }

  }

}
