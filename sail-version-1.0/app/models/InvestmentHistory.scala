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
  def getHistoryForInvestments(investments:List[Investment], dateFrom:Option[Date] = None, dateTo:Option[Date] = None):
  Option[List[InvestmentHistory]] = {

    /** Create an empty placeholder list to be filled with investment histories */
    var histories = List[InvestmentHistory]()

    /** For each investment get a list of histories from the database */
    for( investment <- investments) {
      val investmentHistories = dao.find(MongoDBObject("investment" -> investment.id))

      /** Merge the list to the histories list if histories are available */
      if (!investmentHistories.isEmpty) histories = histories ++ investmentHistories.toList
    }

    /** If a date range is defined, find the histories in that range and create new histories at the boundaries **/
    if (dateFrom.isDefined && dateTo.isDefined && histories.length.>(0)) {
      /** Create a holder for the histories */
      val historiesInRange = new ListBuffer[InvestmentHistory]()

      /** Create a sorted array of investment histories to iterate over */
      val sortedHistory = histories.sortBy(history => history.date)

      /** Create a list of investments before the range to ensure the start date has the correct value */
      val investmentsBeforeRange = histories.filter(history => LocalDate.fromDateFields(history.date).
        toDateTimeAtStartOfDay.isBefore(LocalDate.fromDateFields(dateFrom.get).toDateTimeAtStartOfDay)).
        map(history => history.investment).distinct

      /** Ensure the investments that exist before the date exist on the first date by creating histories at the dateFrom */
      investmentsBeforeRange.foreach(investmentId => {
        /** If a history does not exist at the dateFrom for an existing id, create one */
        if (!histories.exists(history => LocalDate.fromDateFields(history.date).toDateTimeAtStartOfDay.
          equals(LocalDate.fromDateFields(dateFrom.get).toDateTimeAtStartOfDay)
           && history.investment == investmentId)) {
          val investmentBeforeRange = sortedHistory.reverse.find(history => LocalDate.fromDateFields(history.date).
            toDateTimeAtStartOfDay.isBefore(LocalDate.fromDateFields(dateFrom.get).toDateTimeAtStartOfDay) &&
            history.investment.equals(investmentId))
          if (investmentBeforeRange.isDefined) {
            val newHistory = investmentBeforeRange.get.copy(id = new ObjectId,date = dateFrom.get)
            if (this.create(newHistory))
              historiesInRange.append(newHistory)
          }
        }
      })

      /** Filter the list of histories between the dates and append them onto the new date range list */
      historiesInRange.appendAll(sortedHistory.filter(history => LocalDate.fromDateFields(history.date).
        toDateTimeAtStartOfDay.isBefore(LocalDate.fromDateFields(dateTo.get).plusDays(1).toDateTimeAtStartOfDay) &&
        LocalDate.fromDateFields(history.date).toDateTimeAtStartOfDay.isAfter(LocalDate.fromDateFields(dateFrom.get).
          minusDays(1).toDateTimeAtStartOfDay)))

      /** Get each investment id in the range to ensure a history is available at the end date */
      val investmentsInRange = historiesInRange.map(history => history.investment).distinct

      /** Ensure a history exists at the last date for each investment */
      investmentsInRange.foreach(investmentId => {
        /** If a history does not exist at the dateTo for an existing id, create one */
        if (!historiesInRange.exists(history => LocalDate.fromDateFields(history.date).toDateTimeAtStartOfDay.
          equals(LocalDate.fromDateFields(dateTo.get).toDateTimeAtStartOfDay)
          && history.investment == investmentId)) {
          val investmentinRange = historiesInRange.reverse.find(history => LocalDate.fromDateFields(history.date).
            toDateTimeAtStartOfDay.isBefore(LocalDate.fromDateFields(dateTo.get).toDateTimeAtStartOfDay) &&
            history.investment.equals(investmentId))
          if (investmentinRange.isDefined) {
            val newHistory = investmentinRange.get.copy(id = new ObjectId,date = dateTo.get)
            if (this.create(newHistory))
              historiesInRange.append(newHistory)
          }
        }
      })

      /** Return the list of histories in the range */
      if (historiesInRange.length.>(0)) Some(historiesInRange.toList)
      else None
    }
    else {
      /** Return the list of histories */
      if (histories.size.>(0)) Some(histories)
      else None
    }
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

    /** Create a sorted array of investment histories to iterate over */
    val sortedHistory = histories.sortBy(history => (history.date, history.investment))

    /** Get each distinct Investment ID to ensure ensure all investments are counted at each date */
    val investmentIds = histories.map(history => history.investment).distinct

    /** Get each distinct Date to allow values to be totalled */
    val dates = sortedHistory.map(history => LocalDate.fromDateFields(history.date).toDateTimeAtStartOfDay).distinct

    /** Store a list of each date stored so far */
    var investmentHistoriesRecorded = ListBuffer[InvestmentHistory]()

    /** Store a list of each investment ID recorded so far on a single date */
    var investmentsRecorded = ListBuffer[ObjectId]()

    /** For each history get build up a time series, sorted by date to ensure the values are in the correct order
      * relies on the dates of each history being correct
      */
    dates.foreach(date => {
      /** set up a running total for this date */
      var runningTotal = BigDecimal(0)

      /** iterate over the histories at this date */
      sortedHistory.foreach(history => if (LocalDate.fromDateFields(history.date).toDateTimeAtStartOfDay.equals(date)){

        /** Add the value of any history at this date */
        runningTotal+=history.value

        /** Add this history to the recorded history array to be used later */
        investmentHistoriesRecorded.+=(history)

        /** Mark this investment as being recorded for this date to be used later */
        investmentsRecorded.+=(history.investment)
      })

      /** Check if all Investments have been recorded for this date */
      if (investmentIds.length != investmentsRecorded.length) {

        /** Find the ID's of the investments which have not been recorded yet */
        val investmentsLeft = (investmentIds++investmentsRecorded).groupBy(id => id).filter(_._2.lengthCompare(1) == 0)

        /** Iterate over each investment left to find the most recent value */
        investmentsLeft.foreach(id => {
          /** Reverse the recorded history array to find the last history with this ID */
          val pastHistory = investmentHistoriesRecorded.reverse.find(_.investment == id._1)

          /** Ensure the recorded history value exists and add it to this dates running total */
          if (pastHistory.isDefined) {
            runningTotal+=pastHistory.get.value
          }
        })
      }

      /** The running total is now complete for this date - add append it to the time series array */
      timeSeries.+=((date.toDate(),runningTotal))

      /** Clear the investments recorded array to use in the next iteration */
      investmentsRecorded.clear()
    })

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

    /** Find the start of the day and the end of the day to provide a date range */
    val thisMorning = LocalDate.now().toDateTimeAtStartOfDay.toDate()
    val midnight = LocalDate.now().plusDays(1).toDateTimeAtStartOfDay().toDate()

    /** Check if a history has been recorded for this investment today */
    val alreadyRecorded = dao.findOne(MongoDBObject("investment" -> investmentId, "date" ->
      MongoDBObject("$gte" -> thisMorning, "$lte" -> midnight)))

    /** If the history has been not been recorded add it as a new date to the investmenthistorys table */
    val recordedHistory = alreadyRecorded.getOrElse({
      /** Create the history to add */
      val history = new InvestmentHistory(value = value, valuechanged = valueChanged, investment = investmentId,
        quantity = quantity,date = DateTime.now().toDate, recentchange = None)
      /** Insert the created history into the database and return true or false*/
      create(history)
    })

    /** If the history has already been recorded for today - update it's value */
    if (recordedHistory.isInstanceOf[InvestmentHistory]) {

      /** Update the InvestmentHistory for the Investment id in the database */
      dao.save(recordedHistory.asInstanceOf[InvestmentHistory].copy(value = value, quantity = quantity))

      /** Ensure the update was successful and return */
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
   * Update an Investment History in the DB
   * Ensures the update has been successful
   *
   * @param investmentHistory InvestmentHistory the updated investment history to save
   * @return                  Boolean whether the update was successful
   */
  def update(investmentHistory: InvestmentHistory): Boolean = {
    /** Update the InvestmentHistory */
    dao.save(investmentHistory)

    /** Ensure the update was successful */
    val history = getOne(investmentHistory.id)
    history.isDefined && history.get.value.equals(investmentHistory.value)
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

  /**
   * Finds a list of investment histories at the date passed in
   *
   * @param date            Date the simple date object to be used in the Mongo query
   * @param investmentIds   Option[List[ObjectId]] The optional list of investment ids relating to the asset class
   * @return                Option[List[InvestmentHistory]] The list of histories to return, if any
   */
  def getAtDate(date: Date, investmentIds:Option[List[ObjectId]]): Option[List[InvestmentHistory]] = {
    /** Find the start of the day and the end of the day to provide a date range */
    val thisMorning = LocalDate.fromDateFields(date).toDateTimeAtStartOfDay.toDate()
    val midnight = LocalDate.fromDateFields(date).plusDays(1).toDateTimeAtStartOfDay().toDate()

    /** Get all the investment histories for the date passed in */
    if (investmentIds.isDefined) {
      val histories = dao.find(MongoDBObject("date" -> MongoDBObject("$gte" -> thisMorning, "$lt" -> midnight),
        "investment" -> MongoDBObject("$in" -> investmentIds.get))).toList
      /** Create a temporary array to hold the id's of investments found at the date */
      var investmentsAtDate = investmentIds.get.toList

      histories.foreach(history => {
        investmentsAtDate = investmentsAtDate.filterNot(invId => invId == history.investment)
      })

      /** Now we have a list of investment which we could not find an investment value for on the date
        * We will need these values for the date from the most recent value on the db
        */
      if (investmentsAtDate.length.>(0)) {
        val historiesBeforeDate = dao.find(MongoDBObject("date" -> MongoDBObject("$lt" -> thisMorning),
          "investment" -> MongoDBObject("$in" -> investmentsAtDate))).toList.sortBy(history => history.date)

        /** Create a list of the most recent values for each of the histories */
        val mostRecentHistories = ListBuffer[InvestmentHistory]()
        investmentsAtDate.foreach(investment => {
          if (historiesBeforeDate.exists(_.investment == investment)) {
            mostRecentHistories.append(historiesBeforeDate.reverse.find(_.investment == investment).get)
          }
        })

        if (mostRecentHistories.length.>(0))
          Some(histories ++ mostRecentHistories.toList)
        else if (histories.length.>(0))
          Some(histories)
        else
          None
      }
      else {
        if (histories.length > 0) Some(histories)
        else None
      }
    }
    else {
      val histories = dao.find(MongoDBObject("date" -> MongoDBObject("$gte" -> thisMorning, "$lt" -> midnight))).toList
      if (histories.length > 0) Some(histories)
      else None
    }
  }
}
