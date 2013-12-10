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
 * AlertHistory class to be mapped from MongoDB using the Salat library
 * Relates to an investment to allow a meaningful alert to be displayed to the user
 *
 * @param id            ObjectId generated on creation
 * @param valuechanged  BigDecimal the value changed in the investment to cause the Alert
 * @param read          Boolean whether the alert has been read or not
 * @param investment    ObjectId the Investment which the Alert is attributed to
 * @param added         Option[Date] the AlertHistory was created, needed by MongoDB
 * @param updated       Option[Date] the AlertHistory was updated, needed by MongoDB
 * @param deleted       Option[Date] the AlertHistory was deleted
 */
case class AlertHistory(
                       id: ObjectId = new ObjectId,
                       valuechanged: BigDecimal,
                       read: Boolean = false,
                       investment: ObjectId,
                       added: Date = new Date(),
                       updated: Option[Date] = None,
                       deleted: Option[Date] = None
                       )

/**
 * Object to hold the AlertHistory functionality implementing getters and setters
 * extends the ModelCompanion trait from Salat
 */
object AlertHistory extends ModelCompanion[AlertHistory, ObjectId] {

  /** Salat Data Access Object to hook into the user collection on the MongoDB */
  val dao = new SalatDAO[AlertHistory, ObjectId](collection = mongoCollection("alerthistory")) {}

  /**
   * Gets all the alerts relating to a list of investments to be displayed on the UI
   *
   * @param investments   List[Investment] the List of investments to retireve Alerts for
   * @param dateFrom      Option[Date] Optional date to set a date range to find alerts for
   * @param dateTo        Option[Date] Optional date to set a date range to find alerts for
   * @return
   */
  def getAlertsForInvestments(investments:List[Investment], dateFrom:Option[Date], dateTo:Option[Date]) : List[AlertHistory] = {

    /** Create an empty list to be filled based on the date range */
    var histories = List[AlertHistory]()
    if (dateFrom.isEmpty || dateTo.isEmpty) {
      /** No date range has been provided, get all alerts for the investments */
      for( investment <- investments) {
        histories = histories ++ dao.find(MongoDBObject("investment" -> investment.id)).toList
      }
    }
    else {
      /** Get the Alerts in the date range provided */
      for( investment <- investments) {
        histories = histories ++ dao.find(MongoDBObject("investment" -> investment.id, "date" -> MongoDBObject("$gte" -> dateFrom.get, "$lte" -> dateTo.get))).toList
      }
    }

    /** Return the list of Alert histories retrieved, can be of length 0 */
    histories
  }

  /**
   * Mark an alert as read. To be used when an alert is dismissed by the user
   *
   * @param alerts  List[AlertHistory] The list of AlertHistorys to be dismissed
   */
  def markAlertRead(alerts:List[AlertHistory]) {
    for (alert <- alerts) {
      dao.update(MongoDBObject("_id" -> alert.id),MongoDBObject("valuechanged" -> alert.valuechanged, "read" -> true, "investment" -> alert.investment),false)
    }
  }

}
