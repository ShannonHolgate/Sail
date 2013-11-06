package models

import play.api.Play.current
import java.util.Date
import com.novus.salat._
import com.novus.salat.annotations._
import com.novus.salat.dao._
import com.mongodb.casbah.Imports._
import se.radley.plugin.salat._
import models.MongoContext._
import play.Logger

/**
 * Created with IntelliJ IDEA.
 * User: Shannon
 * Date: 05/11/13
 * Time: 22:43
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


object User extends ModelCompanion[User, ObjectId] {
  val dao = new SalatDAO[User, ObjectId](collection = mongoCollection("users")) {}

  def create(username: String, email: String, password: String) {
    dao.insert(User(username = username, email = email, password = password))
  }

  def delete(email: String) {
    dao.remove(MongoDBObject("email" -> email))
  }

  def All(): List[User] = dao.find(MongoDBObject.empty).toList

  def findByEmail(email: String): Option[User] = dao.findOne(MongoDBObject("email" -> email))

  def findByEmailPass(email: String, password: String): Option[User] = {
    dao.findOne(MongoDBObject("email" -> email, "password" -> password))
  }

  def authenticate(email: String, password: String): Option[User] = {
    findByEmailPass(email, password)
  }
}
