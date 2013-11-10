/*
 * Copyright (c) 2013. Shannon Holgate.
 *
 * Sail - A personal fund management web application.
 *
 * Makes use of the Play web framework for scala, MongoDB and the salat-Play plugin
 */

import java.util.Date
import org.joda.time.{Days, DateTime}
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import models.User
import play.api.Play
import play.api.Play.current
import play.api.test._
import play.i18n.Messages
import se.radley.plugin.salat.Binders.ObjectId

/**
 * Tests the User Model, DAO and Integration
 */
@RunWith(classOf[JUnitRunner])
class UserSpec extends Specification with TestUser{
    "User" should {
      "be created" in new WithApplication(currentApplication){
        removeTestUsers
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

      "request reset" in new WithApplication(currentApplication){
        createPasswordResetUsers
        val (key,expire,name) = User.requestReset(testUser.email)

        key.isEmpty must beFalse
        DateTime.now().isAfter(new DateTime(expire.get)) must beFalse
        name.get must equalTo(testUser.username)
      }

      "request reset - user does not exist" in new WithApplication(currentApplication){
        val (key,expire,name) = User.requestReset("doesNotExist@mail.com")

        key.isEmpty must beTrue
        expire.isEmpty must beTrue
        name.isEmpty must beTrue
      }

      "find user for password reset" in new WithApplication(currentApplication){
        val (stringKey,stringKey2) = createPasswordResetUsers
        val user = User.findForPasswordReset(stringKey)

        user.isDefined must beTrue
        user.get.expire.equals(DateTime.now().plusDays(1))
        user.get.reset.get.toString must equalTo(stringKey)
      }

      "find user for password reset - invalid key" in new WithApplication(currentApplication){
        val user = User.findForPasswordReset(new ObjectId().toString)

        user.isDefined must beFalse
      }

      "find user for password reset - key expired" in new WithApplication(currentApplication){
        val stringKey = createPasswordResetUserYesterday
        val user = User.findForPasswordReset(stringKey)

        user.isDefined must beFalse
      }

      "check if reset was requested" in new WithApplication(currentApplication){
        createPasswordResetUsers
        val resetRequested = User.resetRequested(testUser.email)

        resetRequested must beTrue
      }

      "check if reset was requested" in new WithApplication(currentApplication){
        val resetRequested = User.resetRequested("doesNotExist@email.com")

        resetRequested must beFalse
      }

      "reset password" in new WithApplication(currentApplication){
        val (stringKey,stringKey2) = createPasswordResetUsers
        val (name,errorMessage) = User.resetPassword(testUser.email,"password",stringKey)

        name.get must equalTo(testUser.username)
        errorMessage.isDefined must beFalse
      }

      "reset password - invalid key for email" in new WithApplication(currentApplication){
        val (stringKey,stringKey2) = createPasswordResetUsers
        val (name,errorMessage) = User.resetPassword(testUser.email,"password",stringKey2)

        name.isDefined must beFalse
        errorMessage.get must equalTo(Messages.get("incorrectEmailMessage"))
      }
    }
}
