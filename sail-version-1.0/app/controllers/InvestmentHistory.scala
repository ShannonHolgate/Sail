/*
 * Copyright (c) 2013. Shannon Holgate.
 *
 * Sail - A personal fund management web application.
 *
 * Makes use of the Play web framework for scala, MongoDB and the salat-Play plugin
 */

package controllers

import play.api.mvc.{Action, Controller}
import org.joda.time.DateTime
import play.api.libs.json.{JsNumber, JsString, Json, Writes}
import helpers.InvestmentWithValue
import java.util.Date
import java.text.SimpleDateFormat


/**
 *  Controller for the InvestmentHistory Model
 *  Holds the web services to be consumed on the dashboard view
 */
object InvestmentHistory extends Controller with Secured {

  /**
   * Web Service getting the Json data for the investment history by
   * asset class. Uses the cookie sent by the request to get the user's investments.
   *
   * @param assetClass String retrieved from the URL specifying the asset class to be retrieved
   * @return           Result Json result containing key value pairs for a time series. Conforms
   *                   to the Highcharts format to allow easy parsing.
   */
  def getInvestmentHistoryForAssetClass(assetClass:String = "", dateFrom:Option[Date] = None, dateTo:Option[Date] = None) = withUser {
    user => implicit request => {

      /** Create a list holder which will change based on the assetClass */
      var investments:Option[List[models.Investment]] = None

      /** When no assetClass is supplied the application should retrieve all investments */
      if (assetClass.isEmpty) investments = models.Investment.getInvestmentForUser(user.id)
      else investments = models.Investment.getInvestmentForAssetClass(user.id,assetClass)

      /** Ensure the investments are defined before accessing their histories */
      if (investments.isDefined) {

        /** Get the histories for the investment list */
        val histories = models.InvestmentHistory.getHistoryForInvestments(investments.get,dateFrom,dateTo)

        /** Ensure the histories list is defined before parsing it to a time series */
        if (histories.isDefined) {

          /** Convert the histories to a time series list of key value pairs */
          val timeSeries = models.InvestmentHistory.getTimeSeriesForInvestmentHistories(histories.get)

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
          else BadRequest("No data available")
        }

        /** There are no histories available for the investment class */
        else BadRequest("No data available")
      }

      /** There are no investments defined */
      else BadRequest("No data available")
    }
  }
}
