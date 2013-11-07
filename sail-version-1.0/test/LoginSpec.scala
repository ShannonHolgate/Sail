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
import play.api.libs.json._
import play.api.libs.json.Json._
import play.api.mvc.{Session, Cookie}
import play.api.test.Helpers._
import play.api.test._
import play.mvc.Http

@RunWith(classOf[JUnitRunner])
class LoginSpec extends Specification{

  "Login" should {
    "Respond to the login Action" in new WithApplication(FakeApplication(additionalConfiguration = Map("user.timeout.milli" -> "1"))){
      val jsonObject = toJson(
        Map(
          "email" -> toJson("shannon@mail.com"),
          "password" -> toJson("shannon")
        )
      )

      val result = route(FakeRequest(POST, "/login").withJsonBody(jsonObject)).get
      val sessionCookies = cookies(result).get("PLAY_SESSION").orNull

      status(result) must equalTo(SEE_OTHER)
      sessionCookies.value must find("shannon.{1,4}mail.{1,4}com.*connected")
      redirectLocation(result) must some("/")
    }

    "Respond to the logout Action" in {
      val result = controllers.Login.logout()(FakeRequest())

      status(result) must equalTo(SEE_OTHER)
      headers(result).mkString must find("success")
      redirectLocation(result) must some("/login")
    }
  }
}