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
import org.joda.time.{LocalDate, DateTime}
import java.text.SimpleDateFormat

/**
 * InvestmentHistory class to be mapped from MongoDB using the Salat library
 * Many to one relationship with an investment and should only contain one per date in days.
 * Used to build up a time series of histories for the user
 * ValueChanged is critical to the system.
 *
 * @param id            ObjectId generated on creation
 * @param quantity      Option[Int] quantity of investments if an automated investment is related
 * @param value         BigDecimal the value of the investment at the history date
 * @param valuechanged  BigDecimal the change in value from the last known value to current
 * @param date          Date the date in which the history was created
 * @param investment    ObjectId the Id of the related investment
 * @param recentchange  Option[ObjectId] the Id of the related recent change to allow an accurate recent change history
 * @param added         Option[Date] the InvestmentHistory was created, needed by MongoDB
 * @param updated       Option[Date] the InvestmentHistory was updated, needed by MongoDB
 * @param deleted       Option[Date] the InvestmentHistory was deleted
 */
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

/**
 * Object to hold the InvestmentHistory functionality implementing getters and setters
 * extends the ModelCompanion trait from Salat
 */
object InvestmentHistory extends ModelCompanion[InvestmentHistory, ObjectId] {

  /** Salat Data Access Object to hook into the user collection on the MongoDB */
  val dao = new SalatDAO[InvestmentHistory, ObjectId](collection = mongoCollection("investmenthistorys")) {}

  /**
   * Gets a list investment histories for a set of given investments
   *
   * @param investments List[Investment] the list of investments to retrieve the histories for
   * @return            Option[List[InvestmentHistory] ] A list of investment histories for the investments, if any
   */
  def getHistoryForInvestments(investments:List[Investment]) : Option[List[InvestmentHistory]] = {

    /** Create an empty placeholder list to be filled with investment histories */
    var histories = List[InvestmentHistory]()

    /** For each investment get a list of histories from the database */
    for( investment <- investments) {
      val investmentHistories = dao.find(MongoDBObject("investment" -> investment.id))

      /** Merge the list to the histories list if histories are available */
      if (!investmentHistories.isEmpty) histories = histories ++ investmentHistories.toList
    }

    /** Return the list of histories */
    if (histories.size.>(0)) Some(histories)
    else None
  }

  /**
   * Gets a time series of Date value pairs for a given list of investment histories
   *
   * @param histories   List[InvestmentHistory] the list of investment histories to create a time series for
   * @return            Option[List[(Date,BigDecimal)] ] a List of tuples containing the date and the value at that date
   */
  def getTimeSeriesForInvestmentHistories(histories:List[InvestmentHistory]) : Option[List[(Date,BigDecimal)]]= {

    /** Create an empty listholder to build the time series into */
    var timeSeries = ListBuffer[(Date,BigDecimal)]()
    /** The date format to create a readable date for presentation on a view */
    val dateFormat = new SimpleDateFormat("dd-MM-yyyy")

    /** For each history get build up a time series, sorted by date to ensure the values are in the correct order
      * relies on the dates of each history being correct
      */

    val sortedHistory = histories.sortBy(history => (history.date, history.investment))

    /** Get each distinct Investment ID to ensure ensure all investments are counted at each date */
    val investmentIds = histories.map(history => history.investment).distinct

    /** Get each distinct Date to allow values to be totalled */
    val dates = sortedHistory.map(history => LocalDate.fromDateFields(history.date).toDateTimeAtStartOfDay).distinct

    /** Store a list of each date stored so far */
    var investmentHistoriesRecorded = ListBuffer[InvestmentHistory]()
    /** Store a list of each investment ID recorded so far on a single date */
    var investmentsRecorded = ListBuffer[ObjectId]()

    dates.foreach(date => {
      var runningTotal = BigDecimal(0)
      sortedHistory.foreach(history => if (LocalDate.fromDateFields(history.date).toDateTimeAtStartOfDay.equals(date)){
        runningTotal+=history.value
        investmentHistoriesRecorded.+=(history)
        investmentsRecorded.+=(history.investment)
      })
      if (investmentIds.length != investmentsRecorded.length) {
        val investmentsLeft = (investmentIds++investmentsRecorded).groupBy(id => id).filter(_._2.lengthCompare(1) == 0)
        investmentsLeft.foreach(id => {
          val pastHistory = investmentHistoriesRecorded.reverse.find(_.investment == id._1)
          if (pastHistory.isDefined) {
            runningTotal+=pastHistory.get.value
          }
        })
      }
      timeSeries.+=((date.toDate(),runningTotal))
      investmentsRecorded.clear()
    })

   /** for ((history, index) <- histories.sortBy(history => (history.date, history.investment)).zipWithIndex) {

      /** The first index must append the first tuple */
      if (index == 0) {
        timeSeries.+=((history.date,history.value))
        investmentsRecorded.+=(history.investment)
      }

      /** If the date is the same as the last date, add the value to the previous value */
      else if (dateFormat.format(history.date).equals(dateFormat.format(timeSeries.last._1))) {
        timeSeries.update(timeSeries.size-1, (history.date, timeSeries.last._2 + history.valuechanged))
        investmentsRecorded.+=(history.investment)
      }
      /** Otherwise, add a new tuple */
      else
        timeSeries.+=((history.date, history.value))
    }     */

    /** Return the time series */
    if (timeSeries.length > 0) Option(timeSeries.toList) else None
  }

  /**
   * Get a single investment history for an investment history Id
   *
   * @param investmentHistoryId ObjectId the Id of the history to be retrieved
   * @return                    InvestmentHistory the history object relating to the Id
   */
  def getOne(investmentHistoryId:ObjectId) : Option[InvestmentHistory] = {

    /** Get the InvestmentHistory from the database */
    val investmentHistory = dao.findOne(MongoDBObject("_id" -> investmentHistoryId))

    /** Return the InvestmentHistory */
    if (investmentHistory.isDefined) Some(investmentHistory.get)
    else None
  }

  /**
   * Insert the given investment history into the database
   *
   * @param investmentHistory InvestmentHistory the history to be inserted into the database
   * @return                  Boolean whether the insertion was a success or failure
   */
  def create(investmentHistory: InvestmentHistory) : Boolean = {

    /** Insert the history */
    dao.insert(investmentHistory)

    /** Check if the history was inserted and return*/
    val newHistory = getOne(investmentHistory.id)
    if (newHistory.isDefined) {
      newHistory.get.value == investmentHistory.value
    }
    else false
  }

  /**
   * Create a new investment history on todays date based on the investment data given
   *
   * @param value         BigDecimal the value of the investment at todays date
   * @param valueChanged  BigDecimal the value changed with respect to the last known date of the investment
   * @param investmentId  ObjectId the Id of the investment of which the history relates to
   * @param quantity      Option[Int] the quantity of investments if the the related investment is automated
   * @return              Boolean whether the creation was a success or failure
   */
  def createToday(value:BigDecimal, valueChanged:BigDecimal, investmentId:ObjectId, quantity:Option[Int]) : Boolean = {

    /** Create a new InvestmentHistory object for today */
    val thisMorning = LocalDate.now().toDateTimeAtStartOfDay.toDate()
    val midnight = LocalDate.now().plusDays(1).toDateTimeAtStartOfDay().toDate()
    val alreadyRecorded = dao.findOne(MongoDBObject("investment" -> investmentId, "date" -> MongoDBObject("$gte" -> thisMorning, "$lte" -> midnight)))

    val recordedHistory = alreadyRecorded.getOrElse({
      val history = new InvestmentHistory(value = value, valuechanged = valueChanged, investment = investmentId, quantity = quantity,date = DateTime.now().toDate, recentchange = None)
      /** Insert the created history into the database and return true or false*/
      create(history)
    })
    if (recordedHistory.isInstanceOf[InvestmentHistory]) {
      /** Update the InvestmentHistory for the Investment id in the database */
      dao.save(recordedHistory.asInstanceOf[InvestmentHistory].copy(value = value))

      val updatedHistory = getOne(recordedHistory.asInstanceOf[InvestmentHistory].id).getOrElse(false)
      if (updatedHistory.isInstanceOf[InvestmentHistory])
        updatedHistory.asInstanceOf[InvestmentHistory].value.equals(value)
      else
        updatedHistory.asInstanceOf[Boolean]
    }
    else
      recordedHistory.asInstanceOf[Boolean]
  }

  /**
   * Remove an InvestmentHistory from the database based on the Id of the recent change
   * To be used when the user declines an automated payment
   *
   * @param recentChangeId  ObjectId the Id of the recent change
   * @return                Boolean where the removal was a success or failure
   */
  def removeForRecentChange(recentChangeId:ObjectId): Boolean = {

    /** Find the InvestmentHistory for the given change Id */
    val history = dao.findOne(MongoDBObject("recentchange" -> recentChangeId))

    /** Ensure the history exists */
    if (history.isDefined) {

      /** Remove the history from the database */
      dao.remove(history.get)

      /** Return whether the history exists in the database any longer */
      dao.findOne(MongoDBObject("recentchange" -> recentChangeId)).isEmpty
    }

    /** The history does not exist */
    else false
  }
}
