/*
 * Copyright (c) 2013. Shannon Holgate.
 *
 * Sail - A personal fund management web application.
 *
 * Makes use of the Play web framework for scala, MongoDB and the salat-Play plugin
 */

package models

import play.api.Play.current
import java.util.Date
import com.novus.salat.dao._
import com.mongodb.casbah.Imports._
import se.radley.plugin.salat._
import models.MongoContext._
import org.joda.time.{DateTime, Days}
import play.api.Play
import play.i18n.Messages
import org.mindrot.jbcrypt.BCrypt

/**
 * User class to be mapped from MongoDB with use of the Salat library
 * A name, email and password must be available when creating an instance
 * of the User class
 *
 * @param id        ObjectId generated on creation of a new User
 * @param name      String username
 * @param email     String email address
 * @param password  String password
 * @param added     Option[Date] the user was created, needed by MongoDB
 * @param updated   Option[Date] the user was updated, needed by MongoDB
 * @param deleted   Option[Date] the user was deleted
 * @param reset     Option[ObjectId] unique reset password key
 * @param expire    Option[Date] expiry date of the reset password key
 */
case class User(
                 id: ObjectId = new ObjectId,
                 name: String,
                 email: String,
                 password: String,
                 added: Date = new Date(),
                 updated: Option[Date] = None,
                 deleted: Option[Date] = None,
                 reset: Option[ObjectId] = None,
                 expire: Option[Date] = None
                 )

/**
 * Object to hold the User functionality implementing getters and setters
 * extends the ModelCompanion trait from Salat
 */
object User extends ModelCompanion[User, ObjectId] {

  /** Salat Data Access Object to hook into the user collection on the MongoDB */
  val dao = new SalatDAO[User, ObjectId](collection = mongoCollection("users")) {}

  /**
   * Creates a new User and saves it to the users collection on the MongoDB
   *
   * @param name  String username of the new User
   * @param email     String email address of the new User
   * @param password  String password of the new user
   */
  def create(name: String, email: String, password: String) {
    dao.insert(User(name = name, email = email, password = BCrypt.hashpw(password, BCrypt.gensalt())))
  }

  /**
   * Gets a single user from the users collection on the MongoDB.
   * If none are found, the Option will be empty
   *
   * @param email String email address of the User to be returned
   * @return      Option[User] the User object to be returned or empty
   */
  def findByEmail(email: String): Option[User] = dao.findOne(MongoDBObject("email" -> email))


  /**
   * Gets a single user from the users collection on the MongoDB.
   * Takes an email address and password to be used in authorisation methods
   *
   * @param email     String email address of the User to be returned
   * @param password  String password of the User to be returned
   * @return          Option[User] the User object to be returned or empty
   */
  def authenticate(email: String, password: String): Option[User] = {
    val user = findByEmail(email)
    if (user.isEmpty)
      return None
    else {
      if (BCrypt.checkpw(password, user.get.password))
         return user
      else
        return None
    }
  }

  /**
   * Requests a reset key from the MongoDB and maps an expiry date to the user.
   *
   * @param email   String email address of the user to reset
   * @return        (Option[String],Option[Date],Option[String]) tuple containing the unique
   *                reset key for the user, the expiry date of the key and the name of the
   *                user to be reset. Returns none when the user is not found by email
   */
  def requestReset(email: String): (Option[String],Option[Date], Option[String]) = {
    val user: Option[User] = findByEmail(email)
    if (user.isDefined) {
      val resetKey = new ObjectId
      val resetExpires = DateTime.now().plusDays(Play.application.configuration.getString("reset.expire.days").get.toInt).toDate
      dao.update(MongoDBObject("email" -> user.get.email),MongoDBObject("name" -> user.get.name, "email" -> user.get.email, "password" -> user.get.password, "reset" -> resetKey, "expire" -> resetExpires),false)
      (Option(resetKey.toString),Option(resetExpires),Option(user.get.name))
    }
    /** Unlikely to happen as client side validation of the email is performed */
    else (None,None,None)
  }

  /**
   * Checks the MongoDB for the reset password key given.
   * Ensures the key has not expired, if it has, update the user object
   * by removing the reset key and expiry date
   *
   * @param key   String the reset key to be found on the MongoDB
   * @return      Option[User] the user that requested the password reset
   *              or None when the key does not exist or the key has expired
   */
  def findForPasswordReset(key: String): Option[User] = {
    try {
      val user: Option[User] = dao.findOne(MongoDBObject("reset" -> new ObjectId(key)))
      if(user.isDefined && user.get.expire.isDefined){
        if(DateTime.now().isAfter(new DateTime(user.get.expire.get))) {
          dao.update(MongoDBObject("email" -> user.get.email),MongoDBObject("name" -> user.get.name, "email" -> user.get.email, "password" -> user.get.password),false)
          None
        }
        else user
      }
      /** Unlikely to happen as client side validation of the email is performed */
      else None
    }
    catch {
      case ile: IllegalArgumentException => None
    }
  }

  /**
   * Checks the MongoDB to ensure a password reset has been requested for the given user
   *
   * @param email   String email address of the user to be reset
   * @return        Boolean stating whether the user has requested their password to be reset
   *                or not
   */
  def resetRequested(email: String): Boolean = {
    val user:Option[User] = dao.findOne(MongoDBObject("email" -> email))
    if (user.isDefined && user.get.reset.isDefined) true
    else false
  }

  /**
   * Resets the password for the user on the MongoDB.
   * Updates the password and removes the reset key and expiry date.
   *
   * @param email     String email address of the user
   * @param password  String new password for the user
   * @param key       String unique reset key relating to the user
   * @return          (Option[String],Option[String]) tuple containing the users name and None
   *                  or no user and an error message stating that the key did not match the email
   */
  def resetPassword(email: String, password: String, key: String):(Option[String],Option[String]) = {
    val user: Option[User] = Option(dao.findOne(MongoDBObject("email" -> email,"reset" -> new ObjectId(key))).getOrElse({
      return(None,Option(Messages.get("error.email.incorrect")))
    }))
    dao.update(MongoDBObject("email" -> email),MongoDBObject("name" -> user.get.name, "email" -> user.get.email, "password" -> BCrypt.hashpw(password, BCrypt.gensalt())),false)
    return (Option(user.get.name),None)
  }

  /**
   * Changes the password for the email address in the MongoDB
   * Used in the User Admin screen
   *
   * @param email     String Email address of the User
   * @param password  String new Password of the User
   * @return          String containing any update errors that occur
   */
  def changePassword(user:ObjectId, email: String, password: String): String = {
    /** Update the password for the email address */
    dao.update(q = MongoDBObject("_id" -> user, "email" -> email),o = MongoDBObject("$set" -> MongoDBObject(
      "password" -> BCrypt.hashpw(password, BCrypt.gensalt()))),
      upsert = false, multi = false, wc = new WriteConcern()).getError()
  }

  /**
   * Changes the email address for the user in the MongoDB
   * Ensure the session is reset on the browser to implement the changes without logging out
   *
   * @param user    ObjectID of the user
   * @param email   String new email address for the user
   * @return        String containing any update errors that occur
   */
  def changeEmail(user:ObjectId, email: String): String = {
    /** Update the email address for the user */
    dao.update(q = MongoDBObject("_id" -> user),o = MongoDBObject("$set" -> MongoDBObject(
      "email" -> email)),
      upsert = false, multi = false, wc = new WriteConcern()).getError()
  }

  /**
   * Creates the new fund by giving the user a new object id
   * Old users are kept without their email addresses to allow future changes where the user can open old
   * funds again
   *
   * @param user  ObjectID of the user
   * @return      Boolean whether the fund change was successful or not
   */
  def newFund(user:ObjectId): Boolean = {
    /** Get the current user from the MongoDB */
    val mongoUser = dao.findOne(MongoDBObject("_id" -> user))
    if (mongoUser.isDefined) {
      /** Get the credentials of the new user */
      val newUser = mongoUser.get.copy(id = new ObjectId)

      /** Clear down the old user for reuse later */
      dao.update(q = MongoDBObject("_id" -> user),o = MongoDBObject("$set" -> MongoDBObject(
        "email" -> "",
        "reset" -> "", "expire" -> "")),
        upsert = false, multi = false, wc = new WriteConcern()).getError()

      /** Insert the new user into the MongoDB
        * This acts in the same way as creating a new fund
        */
      dao.insert(newUser).isDefined
    }
    else
      false
  }

  /**
   * Changes the name for the user in the MongoDB
   *
   * @param user  ObjectId of the user
   * @param name  String the new name of the user
   * @return      String containing any update errors that occur
   */
  def changeName(user:ObjectId, name:String): String = {
    /** Update the name for the user */
    dao.update(q = MongoDBObject("_id" -> user),o = MongoDBObject("$set" -> MongoDBObject(
      "name" -> name)),
      upsert = false, multi = false, wc = new WriteConcern()).getError()
  }
}
