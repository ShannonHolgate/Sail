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
import java.text.SimpleDateFormat
import models.{Investment, InvestmentHistory}
import play.Logger
import play.api.libs.json.{JsNumber, JsString, Json, Writes}

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

  def index(assetClass:String) = withUser{
    user => implicit request => {
      /** Render the dashboard view, now that the data is ready for it */
      Ok(html.assetclass(user.name,assetClass,"Sail - "+assetClass,addAutoForm,addManualForm,removeForm)).flashing(request.flash)
    }
  }

  def getTimeSeriesForAssetClass(assetClass:String,assetId:String = "",dateFrom:String = "",dateTo:String = "") = withUser {
    user => implicit request => {
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
      // TODO Single investment time!
      else BadRequest("Single Investment")
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
