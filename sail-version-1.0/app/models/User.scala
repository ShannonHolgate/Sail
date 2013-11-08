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

/**
 * User class to be mapped from MongoDB with use of the Salat library
 * A username, email and password must be available when creating an instance
 * of the User class
 *
 * @param id        ObjectId generated on creation of a new User
 * @param username  String username
 * @param email     String email address
 * @param password  String password
 * @param added     Date the user was created, needed by MongoDB
 * @param updated   Date the user was updated, needed by MongoDB
 * @param deleted   Date the user was deleted
 */
case class User(
                 id: ObjectId = new ObjectId,
                 username: String,
                 email: String,
                 password: String,
                 added: Date = new Date(),
                 updated: Option[Date] = None,
                 deleted: Option[Date] = None
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
   * @param username  String username of the new User
   * @param email     String email address of the new User
   * @param password  String password of the new user
   */
  def create(username: String, email: String, password: String) {
    dao.insert(User(username = username, email = email, password = password))
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
    dao.findOne(MongoDBObject("email" -> email, "password" -> password))
  }
}
