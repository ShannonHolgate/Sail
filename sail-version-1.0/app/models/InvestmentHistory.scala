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
import org.joda.time.DateTime
import java.text.SimpleDateFormat

case class InvestmentHistory(
                       id: ObjectId = new ObjectId,
                       quantity: Option[Int] = None,
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

  val dao = new SalatDAO[InvestmentHistory, ObjectId](collection = mongoCollection("investmenthistorys")) {}

  def getHistoryForInvestments(investments:List[Investment]) : Option[List[InvestmentHistory]] = {
    var histories = List[InvestmentHistory]()
    for( investment <- investments) {
      val investmentHistories = dao.find(MongoDBObject("investment" -> investment.id))
      if (!investmentHistories.isEmpty) histories = histories ++ investmentHistories.toList
    }
    if (histories.size.>(0)) Some(histories)
    else None
  }

  def getTimeSeriesForInvestmentHistories(histories:List[InvestmentHistory]) : Option[List[(Date,BigDecimal)]]= {
    var timeSeries = ListBuffer[(Date,BigDecimal)]()
    val dateFormat = new SimpleDateFormat("dd-MM-yyyy")

    for ((history, index) <- histories.sortBy(history => (history.date, history.investment)).zipWithIndex) {
      if (index == 0)
        timeSeries.+=((history.date,history.valuechanged))
      else if (dateFormat.format(history.date).equals(dateFormat.format(timeSeries.last._1)))
        timeSeries.update(timeSeries.size-1, (history.date, timeSeries.last._2 + history.valuechanged))
      else
        timeSeries.+=((history.date, timeSeries.last._2 + history.valuechanged))
    }
    Option(timeSeries.toList)
  }

  def getOne(investmentHistoryId:ObjectId) : Option[InvestmentHistory] = {
    val investmentHistory = dao.findOne(MongoDBObject("_id" -> investmentHistoryId))
    if (investmentHistory.isDefined) Some(investmentHistory.get)
    else None
  }

  def create(investmentHistory: InvestmentHistory) : Boolean = {
    dao.insert(investmentHistory)
    val newHistory = getOne(investmentHistory.id)
    if (newHistory.isDefined) {
      newHistory.get.value == investmentHistory.value
    }
    else false
  }

  def createToday(value:BigDecimal, valueChanged:BigDecimal, investmentId:ObjectId, quantity:Option[Int]) : Boolean = {
    val history = new InvestmentHistory(value = value, valuechanged = valueChanged, investment = investmentId, quantity = quantity,date = DateTime.now().toDate, recentchange = None)
    create(history)
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
