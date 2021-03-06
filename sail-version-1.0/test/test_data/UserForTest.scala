package test_data

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
import org.joda.time.DateTime
import play.api.test.FakeApplication
import org.mindrot.jbcrypt.BCrypt

/**
 * A simple User class to be mapped for testing purposes only
 *
 * @param username  String name of the Test User
 * @param email     String email of the Test User
 * @param password  String password of the Test User
 */
case class UserForTest (id:ObjectId,
                        username:String,
                        email:String,
                        password:String)

/**
 * Test User trait to be accessed across test specs to keep test user consistent during integration
 * Provides methods to ensure the Test User exists and remove if necessary
 * Provides a default FakeApplication to attach to the test MongoDB
 */
trait TestUser{

  /** Specs2 has issues starting and stopping FakeApplications, rename to currentApplication for understanding */
  val currentApplication = FakeApplication(additionalConfiguration =Map("mongodb.default.db" -> "test2", "user.timeout.days" -> "1"))

  /** The default Test User to be used across the test Specs */
  val testUser:UserForTest = UserForTest(new ObjectId("5283e32d03649c00127432d7"), "tester", "sailTestUser@gmail.com", "password1234")

  /** Second Test User for use with password token tests */
  val testUser2:UserForTest = UserForTest(new ObjectId("535bb6df322994ca99c67bd5"),"tester 2", "sailTestUser2@gmail.com", "password1234")

  /** Standalone Test User for use with password token tests */
  val standalone:UserForTest = UserForTest(new ObjectId("535d759ef744a1d77d2ba56c"), "Standalone", "standalone@mail.com", "standalone")

  /**
   * Creates a user with no investments to test target fund failures etc
   */
  def createStandaloneUser {
    MongoConnection().getDB("test2").getCollection("users").insert(
      MongoDBObject("_id" -> standalone.id,
        "name" -> standalone.username,
        "email" -> standalone.email,
        "password" -> BCrypt.hashpw(standalone.password, BCrypt.gensalt()),
        "added" -> new Date()))
  }

  /**
   * Removes the Test Users from the test MongoDB
   */
  def removeTestUsers = {
    MongoConnection().getDB("test2").getCollection("users").drop()
  }

  /**
   * Remove the target fund for the test user
   */
  def removeTargetFund {
    MongoConnection().getDB("test2").getCollection("targetfund").findAndRemove(MongoDBObject("user" ->
     testUser.id));
  }

  /**
   * Add the target fund for the test user and standalone user
   */
  def addTargetFund {
    MongoConnection().getDB("test2").getCollection("targetfund").insert(MongoDBObject("_id" -> new ObjectId(),
    "assetClassPercentages" -> List[Double](15,5,10,10,40,20), "user" -> testUser.id))
    MongoConnection().getDB("test2").getCollection("targetfund").insert(MongoDBObject("_id" -> new ObjectId(),
      "assetClassPercentages" -> List[Double](15,5,10,10,40,20), "user" -> standalone.id))
  }

  /**
   * Add Risk appetite for the test user and standalone user
   */
  def addRiskAppetite {
    MongoConnection().getDB("test2").getCollection("riskappetite").insert(MongoDBObject("_id" -> new ObjectId(),
    "qOne" -> "3", "qTwo" -> "3", "qThree" -> "1", "qFour" -> "1", "qFive" -> "2", "qSix" -> "2", "qSeven" -> "3",
    "qEight" -> "3", "qNine" -> "1", "qTen" -> "1", "qEleven" -> "1", "qTwelve" -> "1", "riskAppetite" -> 4,
    "user" -> testUser.id));
    MongoConnection().getDB("test2").getCollection("riskappetite").insert(MongoDBObject("_id" -> new ObjectId(),
      "qOne" -> "3", "qTwo" -> "3", "qThree" -> "1", "qFour" -> "1", "qFive" -> "2", "qSix" -> "2", "qSeven" -> "3",
      "qEight" -> "3", "qNine" -> "1", "qTen" -> "1", "qEleven" -> "1", "qTwelve" -> "1", "riskAppetite" -> 4,
      "user" -> standalone.id));
  }

  /**
   * Remove Risk appetite for test user
   */
  def removeRiskAppetite {
    MongoConnection().getDB("test2").getCollection("riskappetite").findAndRemove(MongoDBObject("user" ->
      testUser.id));
  }

  /**
   * Checks if the Test User exists in the test MongoDB
   * Creates the Test User in the MongoDB if needed
   */
  def confirmTestUserExists = {
    if (MongoConnection().getDB("test2").getCollection("users").find(MongoDBObject("_id" -> testUser.id)).count().<(1)) {
      MongoConnection().getDB("test2").getCollection("users").insert(
        MongoDBObject("_id" -> testUser.id,
          "name" -> testUser.username,
          "email" -> testUser.email,
          "password" -> BCrypt.hashpw(testUser.password, BCrypt.gensalt()),
          "added" -> new Date()))
    }
  }

  /**
   * Checks if the Test User exists in the MongoDB with no further processing
   *
   * @return  Boolean whether the Test User exists or not
   */
  def testUserExists : Boolean = {
    MongoConnection().getDB("test2").getCollection("users").find(MongoDBObject("email" -> testUser.email)).count().>=(1)
  }

  /**
   * Adds 2 users for reset and returns their unique reset keys
   *
   * @return (String,String) tuple containing the reset key for TestUser and
   *         TestUser2
   */
  def createPasswordResetUsers : (String,String) = {
    /** Remove test users */
    removeTestUsers

    val objectId1 = new ObjectId()
    val objectId2 = new ObjectId()
    val tomorrow = DateTime.now().plusDays(1).toDate

    /** Add the first reset request */
    MongoConnection().getDB("test2").getCollection("users").insert(
      MongoDBObject("_id" -> testUser.id,
        "name" -> testUser.username,
        "email" -> testUser.email,
        "password" -> BCrypt.hashpw(testUser.password, BCrypt.gensalt()),
        "added" -> new Date(),
        "reset" -> objectId1,
        "expire" -> tomorrow))

    /** Add the second reset request */
    MongoConnection().getDB("test2").getCollection("users").insert(
      MongoDBObject("_id" -> new ObjectId,
        "name" -> testUser2.username,
        "email" -> testUser2.email,
        "password" -> BCrypt.hashpw(testUser2.password, BCrypt.gensalt()),
        "added" -> new Date(),
        "reset" -> objectId2,
        "expire" -> tomorrow))

    (objectId1.toString, objectId2.toString)
  }

  /**
   * Adds a user with password requested yesterday
   *
   * @return String containing the reset key for TestUser
   */
  def createPasswordResetUserYesterday : String = {
    /** Remove test users */
    removeTestUsers

    val objectId1 = new ObjectId()
    val yesterday = DateTime.now().minusDays(1).toDate

    /** Add the first reset request */
    MongoConnection().getDB("test2").getCollection("users").insert(
      MongoDBObject("_id" -> testUser.id,
        "name" -> testUser.username,
        "email" -> testUser.email,
        "password" -> BCrypt.hashpw(testUser.password, BCrypt.gensalt()),
        "added" -> new Date(),
        "reset" -> objectId1,
        "expire" -> yesterday))

    objectId1.toString
  }
}
