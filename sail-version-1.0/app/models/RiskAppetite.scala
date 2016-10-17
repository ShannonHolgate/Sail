/*
 * Copyright (c) 2014. Shannon Holgate.
 *
 * Sail - A personal fund management web application.
 *
 * Makes use of the Play web framework for scala, MongoDB and the salat-Play plugin
 */

package models

import com.mongodb.casbah.Imports._
import org.bson.types.ObjectId
import com.mongodb.casbah.commons.TypeImports.ObjectId
import java.util.Date
import com.novus.salat.dao.{SalatDAO, ModelCompanion}
import plugins.salat._
import play.api.Play.current
import models.MongoContext._

/**
 * Risk Appetite class to be mapped from MongoDB with use of the Salat library
 * maps the answer to each question strongly typed to preserve order
 *
 * @param id              ObjectId generated on creation of a risk appetite
 * @param user            ObjectId of the User who owns the risk appetite
 * @param qOne            String value of the answer to the related question
 * @param qTwo            String value of the answer to the related question
 * @param qThree          String value of the answer to the related question
 * @param qFour           String value of the answer to the related question
 * @param qFive           String value of the answer to the related question
 * @param qSix            String value of the answer to the related question
 * @param qSeven          String value of the answer to the related question
 * @param qEight          String value of the answer to the related question
 * @param qNine           String value of the answer to the related question
 * @param qTen            String value of the answer to the related question
 * @param qEleven         String value of the answer to the related question
 * @param qTwelve         String value of the answer to the related question
 * @param riskAppetite    Int the Interger representation of the calculated risk appetite
 * @param added           Option[Date] the user was created, needed by MongoDB
 * @param updated         Option[Date] the user was updated, needed by MongoDB
 * @param deleted         Option[Date] the user was deleted
 */
case class RiskAppetite(
                 id: ObjectId = new ObjectId,
                 user: ObjectId,
                 qOne: String,
                 qTwo: String,
                 qThree: String,
                 qFour: String,
                 qFive: String,
                 qSix: String,
                 qSeven: String,
                 qEight: String,
                 qNine: String,
                 qTen: String,
                 qEleven: String,
                 qTwelve: String,
                 riskAppetite: Int,
                 added: Date = new Date(),
                 updated: Option[Date] = None,
                 deleted: Option[Date] = None
                 )

/**
 * Object to hold the Risk Appetite functionality implementing getters and setters
 * extends the ModelCompanion trait from Salat
 */
object RiskAppetite extends ModelCompanion[RiskAppetite, ObjectId] {
  /** Salat Data Access Object to hook into the user collection on the MongoDB */
  val dao = new SalatDAO[RiskAppetite, ObjectId](collection = mongoCollection("riskappetite")) {}

  /**
   * Insert or update the users risk appetite
   *
   * @param user          ObjectId of the User
   * @param answers       List[String] Ordered list of answer values
   * @param riskAppetite  Int value of the Users risk appetite
   * @return              String Error of the update if the insert or update fails
   */
  def addRiskAppetiteForUser(user:ObjectId,answers:List[String],riskAppetite:Int): String = {
    /** Map the updates to the row in the database */
    dao.update(q = MongoDBObject("user" -> user),o = MongoDBObject("$set" -> MongoDBObject("qOne" -> answers(0),
      "qTwo" -> answers(1),
      "qThree" -> answers(2),
      "qFour" -> answers(3),
      "qFive" -> answers(4),
      "qSix" -> answers(5),
      "qSeven" -> answers(6),
      "qEight" -> answers(7),
      "qNine" -> answers(8),
      "qTen" -> answers(9),
      "qEleven" -> answers(10),
      "qTwelve" -> answers(11),
      "riskAppetite" -> riskAppetite)),
      upsert = true, multi = false, wc = new WriteConcern()).getError()
  }

  /**
   * Gets the Integer representation of the users risk appetite if it exists
   *
   * @param user  ObjectId of the User
   * @return      Option[Int] Optional integer risk appetite if it exists
   */
  def getRiskAppetiteForUser(user:ObjectId):Option[Int] = {
    val appetite = dao.findOne(MongoDBObject("user" -> user))
    if (appetite.isDefined)
      Some(appetite.get.riskAppetite)
    else
      None
  }
}
