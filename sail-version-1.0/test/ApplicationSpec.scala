/*
 * Copyright (c) 2013. Shannon Holgate.
 *
 * Sail - A personal fund management web application.
 *
 * Makes use of the Play web framework for scala, MongoDB and the salat-Play plugin
 */

import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

import play.api.libs.json.Json._
import play.api.test._
import play.api.test.Helpers._

/**
 * Tests the root application controller
 */
@RunWith(classOf[JUnitRunner])
class ApplicationSpec extends Specification with TestUser{

  "Application" should {

    "send 404 on a bad request" in new WithApplication{
      route(FakeRequest(GET, "/boum")) must beNone
    }

    "redirect to the Login page" in new WithApplication{
      val home = route(FakeRequest(GET, "/")).get
      status(home) must equalTo(SEE_OTHER)
      redirectLocation(home) must some("/login")
    }

    "render to the index template" in {
      val html = views.html.index(testUser.username)

      contentType(html) must equalTo("text/html")
      contentAsString(html) must contain(testUser.username)
    }

    "respond to the index Action" in new WithApplication(currentApplication){
      /** The Json form object*/
      val jsonObject = toJson(
        Map(
          "email" -> toJson(testUser.email),
          "password" -> toJson(testUser.password)
        )
      )
      val authenticate= route(FakeRequest(POST,"/login").withJsonBody(jsonObject)).get

      /** Store the cookies to pass onto the next request */
      val sessionCookies = cookies(authenticate).get("PLAY_SESSION").orNull

      val result = route(FakeRequest(GET,"/").withCookies(sessionCookies)).get

      status(result) must equalTo(OK)
      contentAsString(result) must contain(testUser.username)
    }
  }
}
