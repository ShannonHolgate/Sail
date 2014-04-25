/*
 * Copyright (c) 2013. Shannon Holgate.
 *
 * Sail - A personal fund management web application.
 *
 * Makes use of the Play web framework for scala, MongoDB and the salat-Play plugin
 */

package models

import play.api.Play.current
import com.novus.salat.dao._
import com.mongodb.casbah.Imports._
import se.radley.plugin.salat._
import models.MongoContext._
import com.mongodb.casbah.commons.TypeImports.ObjectId
import java.util.Date

/**
 * TargetFund class to be mapped from MongoDB using the Salat library
 * Holds a list of asset class breakdowns, to be held in alphabetical order
 *
 * @param id                      ObjectId generated on creation
 * @param assetClassPercentages   List[Double] the list of asset class percentages,
 *                                Stored in alphabetical order
 * @param user                    ObjectId the Id of the user relating to the target fund
 * @param added                   Option[Date] the TargetFund was created, needed by MongoDB
 * @param updated                 Option[Date] the TargetFund was updated, needed by MongoDB
 * @param deleted                 Option[Date] the TargetFund was deleted
 */
case class TargetFund(
                       id: ObjectId = new ObjectId,
                       assetClassPercentages: List[Double],
                       user: ObjectId,
                       added: Date = new Date(),
                       updated: Option[Date] = None,
                       deleted: Option[Date] = None
                       )

/**
 * Object to hold the TargetFund functionality implementing getters and setters
 * extends the ModelCompanion trait from Salat
 */
object TargetFund extends ModelCompanion[TargetFund, ObjectId] {

  /** Salat Data Access Object to hook into the user collection on the MongoDB */
  val dao = new SalatDAO[TargetFund, ObjectId](collection = mongoCollection("targetfund")) {}

  /**
   * Get the target fund percentages for each asset class in alphabetical order for the user Id
   *
   * @param user  ObjectId the Id of the User
   * @return      Option[List[Double] ] the percentage breakdown of the target fund, Stored in alphabetical order
   */
  def getTargetFundForUser(user:ObjectId) : Option[List[Double]] = {

    /** Get the target fund from the database */
    val targetFund: Option[TargetFund] = dao.findOne(MongoDBObject("user" -> user))

    /** Check if the targetFund exists and return */
    if (targetFund.isDefined) {
      return  Option(targetFund.get.assetClassPercentages)
    }
    else
      None
  }

  /**
   * Inserts or updates the Users target fund breakdown
   *
   * @param user                    ObjectId of the User
   * @param assetClassPercentages   List[Double] list of the target percentages and asset classes,
   *                                Stored in alphabetical order
   * @return                        String Error of the update if the insert or update fails
   */
  def addTargetFundForUser(user: ObjectId, assetClassPercentages:List[Double]): String = {
    /** Map the updates to the row in the database */
    dao.update(q = MongoDBObject("user" -> user),o = MongoDBObject("$set" -> MongoDBObject(
      "assetClassPercentages" -> assetClassPercentages)),
      upsert = true, multi = false, wc = new WriteConcern()).getError()
  }

}