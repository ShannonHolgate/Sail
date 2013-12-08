/*
 * Copyright (c) 2013. Shannon Holgate.
 *
 * Sail - A personal fund management web application.
 *
 * Makes use of the Play web framework for scala, MongoDB and the salat-Play plugin
 */

package controllers

import play.api.mvc.Controller
import org.joda.time.DateTime
import play.api.libs.json.{JsNumber, JsString, Json, Writes}
import helpers.InvestmentWithValue
import java.util.Date
import java.text.SimpleDateFormat

object InvestmentHistory extends Controller with Secured {
  def getInvestmentHistoryForAssetClass(assetClass:String = "") = withUser {
    user => implicit request => {
      var investments:Option[List[models.Investment]] = None
      if (assetClass.isEmpty) investments = models.Investment.getInvestmentForUser(user.id)
      else investments = models.Investment.getInvestmentForAssetClass(user.id,assetClass)
      if (investments.isDefined) {
        val histories = models.InvestmentHistory.getHistoryForInvestments(investments.get)
        if (histories.isDefined) {
          val timeSeries = models.InvestmentHistory.getTimeSeriesForInvestmentHistories(histories.get)
          if (timeSeries.isDefined) {
            val dateFormat = new SimpleDateFormat("dd-MM-yyyy")
            implicit val timeSeriesWrites = new Writes[(Date,BigDecimal)] {
              def writes(timeSeries: (Date,BigDecimal)) = {
                Json.obj(
                  "date" -> JsString(dateFormat.format(timeSeries._1)),
                  "value" -> JsNumber(timeSeries._2)
                )
              }
            }
            Ok(Json.toJson(timeSeries.get))
          }
          else BadRequest("No data available")
        }
        else BadRequest("No data available")
      }
      else BadRequest("No data available")
    }

  }

}
