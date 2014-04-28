/*
 * Copyright (c) 2013. Shannon Holgate.
 *
 * Sail - A personal fund management web application.
 *
 * Makes use of the Play web framework for scala, MongoDB and the salat-Play plugin
 */

package models

import java.util.Date
import com.novus.salat.dao._
import com.mongodb.casbah.Imports._
import se.radley.plugin.salat._
import models.MongoContext._
import play.api.Play.current
import com.mongodb.casbah.commons.TypeImports._

/**
 * RecentChange class to be mapped from MongoDB using the Salat library
 * To be used when setting up regular payments by being manipulated in the database when a regular payment date is
 * reached
 *
 * @param id            ObjectId generated on creation
 * @param quantity      Option[Int] quantity of investments if an automated investment is related
 * @param value         BigDecimal the value of the Investment after the regular payment
 * @param valuechanged  BigDecimal the value that the Investment changed by due to the regular payment
 * @param date          Date the date at which the regular payment created the recent change
 * @param accepted      Boolean whether the user has accepted or declined the recent change, defaults to accepted
 * @param investment    ObjectId the Id of the Investment relating to the recent change
 * @param user          ObjectId the Id of the User relating to the recent change
 * @param added         Option[Date] the RecentChange was created, needed by MongoDB
 * @param updated       Option[Date] the RecentChange was updated, needed by MongoDB
 * @param deleted       Option[Date] the RecentChange was deleted
 */
case class RecentChange(
                        id: ObjectId = new ObjectId,
                        quantity: Option[Int] = None,
                        value: BigDecimal,
                        valuechanged: BigDecimal,
                        date: Date,
                        accepted: Boolean = true,
                        investment: ObjectId,
                        user: ObjectId,
                        added: Date = new Date(),
                        updated: Option[Date] = None,
                        deleted: Option[Date] = None
                        )

/**
 * Object to hold the RecentChange functionality implementing getters and setters
 * extends the ModelCompanion trait from Salat
 */
object RecentChange extends ModelCompanion[RecentChange, ObjectId] {

  /** Salat Data Access Object to hook into the user collection on the MongoDB */
  val dao = new SalatDAO[RecentChange, ObjectId](collection = mongoCollection("recentchanges")) {}

  /**
   * Gets a list of recent changes based on the given investments
   *
   * @param investments List[Investment] the list of investments to find recent changes for
   * @param dateFrom    Option[Date] the date range to get the recent changes for
   * @param dateTo      Option[Date] the date range to get the recent changes for
   * @return            List[RecentChange] the list of recent changes relating to the Investments
   */
  def getRecentChangesForInvestments(investments:List[Investment], dateFrom:Option[Date], dateTo:Option[Date]) :
  List[RecentChange] = {

    /** Create an empty list to be updated with the recent changes */
    var histories = List[RecentChange]()

    /** If the date range is not given, get all the recent changes for the investment */
    if (dateFrom.isEmpty || dateTo.isEmpty) {
      for( investment <- investments) {
        /** For each list of recent changes, merge them onto the histories list */
        histories = histories ++ dao.find(MongoDBObject("recentchanges" -> investment.id)).toList
      }
    }

    /** Get the recent changes for the date range */
    else {
      for( investment <- investments) {
        /** For each list of recent changes, merge them onto the histories list */
        histories = histories ++ dao.find(MongoDBObject("recentchanges" -> investment.id, "date" ->
          MongoDBObject("$gte" -> dateFrom.get, "$lte" -> dateTo.get))).toList
      }
    }
    /** Return the list of recent changes */
    histories
  }

  /**
   * Remove a recent change from the database
   *
   * @param recentChangeId ObjectId the Id of the recent change to remove
   * @param user           ObjectId the Id of the User who relates to the recent change
   * @return               Boolean whether the removal was a success or failure
   */
  def removeRecentChange(recentChangeId: ObjectId, user: ObjectId): Boolean = {

    /** Remove the recent change from the database */
    dao.remove(MongoDBObject("_id" -> recentChangeId, "user" -> user))

    /** Ensure the recent change no longer exists and return */
    dao.findOne(MongoDBObject("_id" -> recentChangeId)).isEmpty
  }

}
