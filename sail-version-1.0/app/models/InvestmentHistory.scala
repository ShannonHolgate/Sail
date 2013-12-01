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
import scala.collection.mutable.ListBuffer
import play.api.Play.current

case class InvestmentHistory(
                       id: ObjectId = new ObjectId,
                       quantity: Option[Double] = None,
                       value: BigDecimal,
                       valuechanged: BigDecimal,
                       date: Date,
                       investment: ObjectId,
                       recentchange: Option[ObjectId],
                       added: Date = new Date(),
                       updated: Option[Date] = None,
                       deleted: Option[Date] = None
                       )

object InvestmentHistory extends ModelCompanion[InvestmentHistory, ObjectId] {

  val dao = new SalatDAO[InvestmentHistory, ObjectId](collection = mongoCollection("investmentHistorys")) {}

  def getHistoryForInvestments(investments:List[Investment], dateFrom:Option[Date], dateTo:Option[Date]) : List[InvestmentHistory] = {
    var histories = List[InvestmentHistory]()
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

  def getTimeSeriesForInvestmentHistories(histories:List[InvestmentHistory]) : Option[List[(Date,BigDecimal)]]= {
    var timeSeries = ListBuffer[(Date,BigDecimal)]()

    for ((history, index) <- histories.sortBy(history => (history.date, history.investment)).zipWithIndex) {
      if (index == 0)
        timeSeries.+=((history.date,history.valuechanged))
      else if (history.date.equals(timeSeries.last._1))
        timeSeries.update(timeSeries.size-1, (history.date, timeSeries.last._2 + history.valuechanged))
      else
        timeSeries.+=((history.date, timeSeries.last._2 + history.valuechanged))
    }
    Option(timeSeries.toList)
  }

  def removeForRecentChange(recentChangeId:ObjectId): Boolean = {
    val history = dao.findOne(MongoDBObject("recentchange" -> recentChangeId))
    if (history.isDefined) {
      dao.remove(history.get)
      dao.findOne(MongoDBObject("recentchange" -> recentChangeId)).isEmpty
    }
    else false
  }
}
