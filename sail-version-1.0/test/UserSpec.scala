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
import models.User
import play.api.test._

/**
 * Tests the User Model, DAO and Integration
 */
@RunWith(classOf[JUnitRunner])
class UserSpec extends Specification with TestUser{
    "User" should {
      "be created" in new WithApplication(currentApplication){
        removeTestUser
        User.create(testUser.username,testUser.email,testUser.password)

        testUserExists must beTrue
      }

      "be found by email" in new WithApplication(currentApplication){
        confirmTestUserExists
        val emailTestUser = User.findByEmail(testUser.email)

        emailTestUser must not beNone
      }

      "be authenticated" in new WithApplication(currentApplication){
        confirmTestUserExists
        val authenticatedUser:User = User.authenticate(testUser.email,testUser.password).getOrElse(null)

        authenticatedUser must not beNull
      }
    }
}
