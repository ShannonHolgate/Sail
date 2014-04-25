/*
 * Copyright (c) 2014. Shannon Holgate.
 *
 * Sail - A personal fund management web application.
 *
 * Makes use of the Play web framework for scala, MongoDB and the salat-Play plugin
 */

package controllers

import play.api.data.Form
import play.api.data.Forms._
import views.html
import play.api.mvc.{SimpleResult, Results, Controller}
import helpers.{InvestmentAtDate, Valuation}
import org.bson.types.ObjectId
import java.util.Date
import javax.security.auth.callback.LanguageCallback
import org.joda.time.LocalDate
import scala.collection.mutable.ListBuffer
import java.text.{ParseException, SimpleDateFormat}
import models.{Investment, InvestmentHistory}
import play.Logger
import play.api.libs.json.{JsNumber, JsString, Json, Writes}
import play.i18n.Messages
import scala.math.BigDecimal.RoundingMode

object AssetClass extends Controller with Secured with Valuation{

  /** Empty auto add form to render the dashboard view */
  val addAutoForm = Form(
    tuple(
      "investmentresults" -> text,
      "quantity" -> number,
      "assetclass" -> text
    )
  )

  /** Empty manual add form to render the dashboard view */
  val addManualForm = Form(
    tuple(
      "name" -> text,
      "currentvalue" -> bigDecimal,
      "assetclass" -> text
    )
  )

  /** Empty remove form to render the dashboard view */
  val removeForm = Form(
    tuple(
      "id" -> text,
      "quantity" -> optional(number),
      "value" -> optional(bigDecimal),
      "removeall" -> boolean,
      "password" -> text
    )
  )

  /**
   * Renders the Asset Class view by taking the asset class from the url
   * If no asset class is assigned, the full list of investments is used
   *
   * @param assetClass String the asset class that the view should render a time series for
   * @return           Result renderinf the Asset Class view
   */
  def index(assetClass:String) = withUser{
    user => implicit request => {
      /** Get the investments in the asset class for the user */
      val investments = models.Investment.getInvestmentForAssetClass(user.id,assetClass)
      if (investments.isDefined) {
        /** Render the dashboard view, now that the data is ready for it */
        Ok(html.assetclass(user.name,assetClass,"Sail - "+assetClass,investments.get,addAutoForm,addManualForm,removeForm)).flashing(request.flash)
      }
      else {
        Ok(html.assetclass(user.name,assetClass,"Sail - "+assetClass,List[models.Investment](),addAutoForm,addManualForm,removeForm)).flashing(configValues.genericError -> Messages.get("view.assetclass.noinvestments",assetClass))
      }
    }
  }

  /**
   * Web Service building up the time series of investments for use on the highcharts time series graph
   * Can return a full history for the asset class or
   * a full history for a specific investment
   * A date range can be specified to give the history between dates
   *
   * @param assetClass  String the asset class to get the investment values for
   * @param assetId     String the string version of the Object ID relating to the investment defaults to empty
   * @param dateFrom    String the string date in the format "yyyy-MM-dd" to be parsed defaults to empty
   * @param dateTo      String the string date in the format "yyyy-MM-dd" to be parsed defaults to empty
   * @return            Result either as a BadRequest or JSON response with the time series in date-value pairs
   */
  def getTimeSeriesForAssetClass(assetClass:String,assetId:String = "",dateFrom:String = "",dateTo:String = "") = withUser {
    user => implicit request => {
      /** Wrap in a try catch to handle date parse errors */
      try {
        /** If an assetId is not provided, get all the assets histories between the dates given */
        if (assetId.length == 0) {
          /** Get a list of investments for the user and assetClass*/
          val investments = models.Investment.getInvestmentForAssetClass(user.id,assetClass)

          if (investments.isDefined) {
            /** Get a list of investment Id's for the automated investments */
            val automatedIds = investments.get.filter(_.symbol.isDefined).map(_.id).distinct

            /** If a date range exists, filter the histories */
            if (dateFrom.length.>(0) && dateTo.length.>(0)) {

              /** Parse the string dates */
              val dateFromFmt = new SimpleDateFormat("yyyy-MM-dd").parse(dateFrom)
              val dateToFmt = new SimpleDateFormat("yyyy-MM-dd").parse(dateTo)

              /** Get the investment histories between the dates given */
              var histories = models.InvestmentHistory.getHistoryForInvestments(investments.get,Some(dateFromFmt),Some(dateToFmt))

              /** Get the automated investments at the dateFrom and dateTo to ensure their values are correctly quoted from yahoo finance */
              if (histories.isDefined) {
                /** Keep a temporary success flag to save extra processing on failure */
                var updateSuccess = true

                /** Get the automated investments and their accurate value at the date from */
                val automatedInvestmentValuesFrom = automatedInvestmentLimitFinder(histories.get,automatedIds,investments.get,dateFromFmt)

                /** Ensure the investment values at the limits are accurate by updating the investment histories in the database */
                if (automatedInvestmentValuesFrom.isDefined) {
                  updateSuccess = updateHistoriesForAutomatedLimit(automatedInvestmentValuesFrom.get,investments.get,histories.get)
                }

                /** If the date from limit updates were successful, update the date to */
                if (updateSuccess) {
                  /** Get the automated investments and their accurate value at the date to */
                  val automatedInvestmentValuesTo = automatedInvestmentLimitFinder(histories.get,automatedIds,investments.get,dateToFmt)

                  /** Ensure the investment values at the limits are accurate by updating the investment histories in the database */
                  if (automatedInvestmentValuesTo.isDefined) {
                    updateSuccess = updateHistoriesForAutomatedLimit(automatedInvestmentValuesTo.get,investments.get,histories.get)
                  }
                }

                /** If all updates were successful do one final call to the investment history table to find the most accurate
                  * Investment values at the date range
                  */
                if (updateSuccess) {
                  histories = models.InvestmentHistory.getHistoryForInvestments(investments.get,Some(dateFromFmt),Some(dateToFmt))

                  /** Finally create the time series for the histories */
                  if (histories.isDefined) {
                    createTimeSeriesAsJson(histories.get)
                  }
                  /** No histories were found on the second run, unlikely to happen unless the DB fails */
                  else BadRequest("Database failure, please try again")
                }
                /** Updating the date limit failed where it should have worked, update the user and bail out */
                else BadRequest("Could not get an accurate value for some of your investments!")
              }
              /** No histories exist in the date range, user must have no investments on or before the range */
              else BadRequest("You have no investments in this date range!")
            }
            /** A date range does not exist, return a full time series for the investments */
            else {
              /** Get the investment histories between the dates given */
              val histories = models.InvestmentHistory.getHistoryForInvestments(investments.get,None,None)
              /** Finally create the time series for the histories */
              if (histories.isDefined) {
                createTimeSeriesAsJson(histories.get)
              }
              /** No histories were found, Will not happen unless DB fails mid flight*/
              else  BadRequest("Database failure, please try again")
            }
          }
          /** No investments were found, user should add */
          else BadRequest("You have no investments, add new investments using the menu on the left")
        }

        /**
         * An Asset ID exists so this is a single investment we are looking for
         */
        else {
          /** Get the investment for the investment id*/
          val investment = models.Investment.getOne(new ObjectId(assetId))

          if (investment.isDefined) {

            /** If a date range exists, filter the histories */
            if (dateFrom.length.>(0) && dateTo.length.>(0)) {

              /** Parse the string dates */
              val dateFromFmt = new SimpleDateFormat("yyyy-MM-dd").parse(dateFrom)
              val dateToFmt = new SimpleDateFormat("yyyy-MM-dd").parse(dateTo)

              /** Get the investment histories between the dates given */
              var histories = models.InvestmentHistory.getHistoryForInvestments(List[Investment](investment.get),Some(dateFromFmt),Some(dateToFmt))

              /** Get the automated investments at the dateFrom and dateTo to ensure their values are correctly quoted from yahoo finance */
              if (histories.isDefined) {
                /** Keep a temporary success flag to save extra processing on failure */
                var updateSuccess = true

                /** Only update the limits if this is an automated investment */
                if (investment.get.symbol.isDefined) {
                  /** Get the automated investments and their accurate value at the date from */
                  val automatedInvestmentValuesFrom = automatedInvestmentLimitFinder(histories.get,
                    List[ObjectId](investment.get.id),
                    List[Investment](investment.get),
                    dateFromFmt)

                  /** Ensure the investment values at the limits are accurate by updating the investment histories in the database */
                  if (automatedInvestmentValuesFrom.isDefined) {
                    updateSuccess = updateHistoriesForAutomatedLimit(automatedInvestmentValuesFrom.get,List[Investment](investment.get),histories.get)
                  }

                  /** If the date from limit updates were successful, update the date to */
                  if (updateSuccess) {
                    /** Get the automated investments and their accurate value at the date to */
                    val automatedInvestmentValuesTo = automatedInvestmentLimitFinder(histories.get,
                      List[ObjectId](investment.get.id),
                      List[Investment](investment.get),
                      dateToFmt)

                    /** Ensure the investment values at the limits are accurate by updating the investment histories in the database */
                    if (automatedInvestmentValuesTo.isDefined) {
                      updateSuccess = updateHistoriesForAutomatedLimit(automatedInvestmentValuesTo.get,List[Investment](investment.get),histories.get)
                    }
                  }
                }
                /** If all updates were successful do one final call to the investment history table to find the most accurate
                  * Investment values at the date range
                  */
                if (updateSuccess) {
                  histories = models.InvestmentHistory.getHistoryForInvestments(List[Investment](investment.get),Some(dateFromFmt),Some(dateToFmt))

                  /** Finally create the time series for the histories */
                  if (histories.isDefined) {
                    createTimeSeriesAsJson(histories.get)
                  }
                  /** No histories were found on the second run, unlikely to happen unless the DB fails */
                  else BadRequest("Database failure, please try again")
                }
                /** Updating the date limit failed where it should have worked, update the user and bail out */
                else BadRequest("Could not get an accurate value for this investment!")
              }
              /** No histories exist in the date range, user must have no investments on or before the range */
              else BadRequest("This investment does not exist in this date range!")
            }
            /** A date range does not exist, return a full time series for the investments */
            else {
              /** Get the investment histories between the dates given */
              val histories = models.InvestmentHistory.getHistoryForInvestments(List[Investment](investment.get),None,None)
              /** Finally create the time series for the histories */
              if (histories.isDefined) {
                createTimeSeriesAsJson(histories.get)
              }
              /** No histories were found, Will not happen unless DB fails mid flight*/
              else  BadRequest("Database failure, please try again")
            }
          }
          /** No investments were found, user should add */
          else BadRequest("This investment does not exist!")
        }
      }
      catch {
        case pe: ParseException => BadRequest("Please correct the URL: " + pe.getMessage)
        case e: Exception => BadRequest("Service request failed: " + e.getMessage)
      }
    }
  }

  /**
   * Web Service to get a formatted list of investment histories for a given date
   * Uses the Investment history model to get all histories from MongoDB at the date then gets the
   * corresponding Investment objects to retrieve the name from.
   * Finally creates a Json string and returns it in the response.
   *
   * @param assetClass  String asset class of from which the histories should be retrieved
   * @param assetId     String the individual investemnt ID if the results need to be filtered
   * @param onDate        String the string date to be parsed in the format "dd-MM-yyyy"
   * @return            Result carrying the Json string representation of the histories at the date
   */
  def getValuesAtDate(assetClass:String,assetId:String = "",onDate:String) = withUser {
    user => implicit request => {
      /** Wrap in a try catch to handle date parse errors */
      try {
        /** Parse the string date */
        val date = new SimpleDateFormat("dd-MM-yyyy").parse(onDate)
        /** Get all the investments in the asset class to gather the histories */
        val investments = models.Investment.getInvestmentForAssetClass(user.id, assetClass)
        if (investments.isDefined) {
          var historiesAtDate = models.InvestmentHistory.getAtDate(date, Some(investments.get.map(_.id).toList))

          /** Create a list buffer to hold the details we will use to parse the histories */
          val formattedHistories = ListBuffer[(String,BigDecimal,Int)]()
          if (historiesAtDate.isDefined) {
            /** If an assetId is provided, only get the histories for it */
            if (assetId.length > 0 && historiesAtDate.get.exists(_.investment.equals(new ObjectId(assetId)))) {
              historiesAtDate = Some(historiesAtDate.get.filter(_.investment.equals(new ObjectId(assetId))))
            }

            historiesAtDate.get.foreach(history => {
              /** Get the investment name for the history */
              val investment = models.Investment.getOne(history.investment)

              /** Create a new list entry for the investment at the date */
              if (investment.isDefined) {
                formattedHistories.append((investment.get.name,
                  history.value.setScale(2, RoundingMode.CEILING),
                  history.quantity.getOrElse(0)))
              }
            })

            /** The Json parser which writes the history at date object to Json strings */
            implicit val historyAtDateWrites = new Writes[(String,BigDecimal,Int)] {
              def writes(historyAtDate: (String,BigDecimal,Int)) = {
                Json.obj(
                  "name" -> JsString(historyAtDate._1),
                  "value" -> JsNumber(historyAtDate._2),
                  "quantity" -> JsNumber(historyAtDate._3)
                )
              }
            }

            /** Return the Json result */
            Ok(Json.toJson(formattedHistories.toList))
          }
          else BadRequest("Please add an investment for this asset class")
        }
        else  BadRequest("There are no investment values recorded at this date")
      }
      catch {
        case pe: ParseException => BadRequest("Please correct the URL: " + pe.getMessage)
        case e: Exception => BadRequest("Service request failed: " + e.getMessage)
      }
    }
  }

  /**
   * Gets the automated investments and their values at any specific date
   * Used mainly to get the accurate automated investment value at the limits of the date range for the asset class
   *
   * @param histories     List[InvestmentHistory] the list of investment histories to pick the dates and quantities from
   * @param automatedIds  List[ObjectId] the list of investment IDs which carry an automated investment
   * @param investments   List[Investments] the list of investment to match the symbol from in the history
   * @param date          Date the simple date to get the accurate investment value for
   *
   * @return              Option[List[InvestmentAtDate]] the list of investment values at the date returns from the
   *                      valuation helper
   */
  def automatedInvestmentLimitFinder(histories:List[InvestmentHistory], automatedIds:List[ObjectId], investments:List[Investment], date:Date):Option[List[InvestmentAtDate]] = {
    /** Get the automated investments at the date */
    val automatedDateFromHistories = histories.filter(history => LocalDate.fromDateFields(history.date).toDateTimeAtStartOfDay.equals(LocalDate.fromDateFields(date).toDateTimeAtStartOfDay) && automatedIds.exists(_.equals(history.investment)))

    /** Get the symbols from the histories */
    val automatedDateFromSymbols = ListBuffer[(String,Int)]()
    automatedDateFromHistories.foreach(history => {
      /** All of these should exists to make it this far */
      val automatedInvestment = investments.find(_.id == history.investment).get
      automatedDateFromSymbols.append((automatedInvestment.symbol.get,history.quantity.get))
    })

    /** Get the automated investment values for the date and return */
    getSymbolValuesAtDateWithQuantity(automatedDateFromSymbols.map(_._1).toList,automatedDateFromSymbols.toList,date)
  }

  /**
   * Updates the Investment histories for each automated investment returned from the Valuation helper
   * Used to update the values at the limits of the date range
   *
   * @param automatedInvestmentValues List[InvestmentAtDate] the list of automated investments returned from the
   *                                  Valuation helper
   * @param investments               List[Investment] the list of Investments to find the correct history from
   * @param histories                 List[InvestmentHistory] the list of Investment histories to be updated
   * @return                          Boolean whether the updates of all the data was successful
   */
  def updateHistoriesForAutomatedLimit(automatedInvestmentValues: List[InvestmentAtDate], investments:List[Investment], histories: List[InvestmentHistory]): Boolean = {
    /** Local success variable to ensure both investment history date limits succeed */
    var historyUpdateSuccess = true

    /** Update the histories for the automated investments */
    automatedInvestmentValues.foreach(automatedInvestment => {
      /** Get the investment with the symbol */
      val investment = investments.find(_.symbol.getOrElse("") == automatedInvestment.yqlQuote.Symbol)
      /** Bail out on the first sign of failure to keep data in sync */
      if (investment.isDefined && historyUpdateSuccess) {
        /** Get the Investment history for the symbol and date */
        val history = histories.find(history => {
          history.investment == investment.get.id && LocalDate.fromDateFields(history.date).toDateTimeAtStartOfDay.equals(LocalDate.parse(automatedInvestment.yqlQuote.Date).toDateTimeAtStartOfDay)
        })
        if (history.isDefined) {
          /** Update the investment history */
          historyUpdateSuccess = models.InvestmentHistory.update(history.get.copy(value = automatedInvestment.value))
        }
      }
    })
    /** Was the update successful */
    historyUpdateSuccess
  }

  /**
   * Creates a time series in Json format from the list of investment histories provided.
   * Will throw a BadRequest if the time series generation fails
   *
   * @param investmentHistorys  List[InvestmentHistory] the list of investment histories
   * @return                    SimpleResult containing the Json or a bad request
   */
  def createTimeSeriesAsJson(investmentHistorys:List[InvestmentHistory]): SimpleResult = {
    /** Convert the histories to a time series list of key value pairs */
    val timeSeries = models.InvestmentHistory.getTimeSeriesForInvestmentHistories(investmentHistorys)

    /** Ensure the time series was successful in being created */
    if (timeSeries.isDefined) {

      /** Format the date for readability */
      val dateFormat = new SimpleDateFormat("dd-MM-yyyy")

      /** The Json parser which writes the time series object to Json strings */
      implicit val timeSeriesWrites = new Writes[(Date,BigDecimal)] {
        def writes(timeSeries: (Date,BigDecimal)) = {
          Json.obj(
            "name" -> JsString(dateFormat.format(timeSeries._1)),
            "y" -> JsNumber(timeSeries._2)
          )
        }
      }

      /** Return the Json result */
      Ok(Json.toJson(timeSeries.get))
    }

    /** The time series failed to be collected */
    else BadRequest("Time series generation failure, please try again")
  }
}
