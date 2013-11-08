/*
 * Copyright (c) 2013. Shannon Holgate.
 *
 * Sail - A personal fund management web application.
 *
 * Makes use of the Play web framework for scala, MongoDB and the salat-Play plugin
 */

import com.mongodb.casbah.Imports._
import java.util.Date
import org.bson.types.ObjectId
import play.api.test.FakeApplication

/**
 * A simple User class to be mapped for testing purposes only
 *
 * @param username  String username of the Test User
 * @param email     String email of the Test User
 * @param password  String password of the Test User
 */
case class UserForTest (username:String,
                        email:String,
                        password:String)

/**
 * Test User trait to be accessed across test specs to keep test user consistent during integration
 * Provides methods to ensure the Test User exists and remove if necessary
 * Provides a default FakeApplication to attach to the test MongoDB
 */
trait TestUser{

  /** Specs2 has issues starting and stopping FakeApplications, rename to currentApplication for understanding */
  val currentApplication = FakeApplication(additionalConfiguration =Map("mongodb.default.db" -> "test"))

  /** The default Test User to be used across the test Specs */
  val testUser:UserForTest = UserForTest("tester", "test_user@mail.com", "added")

  /**
   * Removes the Test User from the test MongoDB
   */
  def removeTestUser = {
    MongoConnection().getDB("test").getCollection("users").findAndRemove(MongoDBObject("email" -> testUser.email))
  }

  /**
   * Checks if the Test User exists in the test MongoDB
   * Creates the Test User in the MongoDB if needed
   */
  def confirmTestUserExists = {
    if (MongoConnection().getDB("test").getCollection("users").find(MongoDBObject("email" -> testUser.email)).count().<(1)) {
      MongoConnection().getDB("test").getCollection("users").insert(
        MongoDBObject("_id" -> new ObjectId,
          "username" -> testUser.username,
          "email" -> testUser.email,
          "password" -> testUser.password,
          "added" -> new Date()))
    }
  }

  /**
   * Checks if the Test User exists in the MongoDB with no further processing
   *
   * @return  Boolean whether the Test User exists or not
   */
  def testUserExists : Boolean = {
    MongoConnection().getDB("test").getCollection("users").find(MongoDBObject("email" -> testUser.email)).count().>=(1)
  }
}
