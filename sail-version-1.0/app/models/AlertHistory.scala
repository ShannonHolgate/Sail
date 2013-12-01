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

case class AlertHistory(
                       id: ObjectId = new ObjectId,
                       valuechanged: BigDecimal,
                       read: Boolean = false,
                       investment: ObjectId,
                       added: Date = new Date(),
                       updated: Option[Date] = None,
                       deleted: Option[Date] = None
                       )

object AlertHistory extends ModelCompanion[AlertHistory, ObjectId] {

  val dao = new SalatDAO[AlertHistory, ObjectId](collection = mongoCollection("alerthistory")) {}

  def getAlertsForInvestments(investments:List[Investment], dateFrom:Option[Date], dateTo:Option[Date]) : List[AlertHistory] = {
    var histories = List[AlertHistory]()
    if (dateFrom.isEmpty || dateTo.isEmpty) {
      for( investment <- investments) {
        histories = histories ++ dao.find(MongoDBObject("investment" -> investment.id)).toList
      }
    }
    else {
      for( investment <- investments) {
        histories = histories ++ dao.find(MongoDBObject("investment" -> investment.id, "date" -> MongoDBObject("$gte" -> dateFrom.get, "$lte" -> dateTo.get))).toList
      }
    }
    histories
  }

  def markAlertRead(alerts:List[AlertHistory]) {
    for (alert <- alerts) {
      dao.update(MongoDBObject("_id" -> alert.id),MongoDBObject("valuechanged" -> alert.valuechanged, "read" -> true, "investment" -> alert.investment),false)
    }
  }

}
