/*
 * Copyright (c) 2013. Shannon Holgate.
 *
 * Sail - A personal fund management web application.
 *
 * Makes use of the Play web framework for scala, MongoDB and the salat-Play plugin
 */

package controllers

import play.api.mvc.Controller
import scala.collection.mutable.ListBuffer
import helpers.{InvestmentWithValue, Risker}
import play.api.libs.json.{JsNumber, JsString, Writes, Json}
import scala.math.BigDecimal.RoundingMode

/**
 *  Controller for the Risk Web Services
 *  Holds the web services to be consumed on the dashboard view
 */
object RiskFinder extends Controller with Secured with Risker{

  /**
   * Web Service getting the risks related to the Current fund and the Target Fund.
   * Utilises the Risker trait to analyse risk based on asset class quantities.
   * Uses the cookie to retrieve the user.
   *
   * @return Result Json result containing a Key value pair of which fund and the risk related
   */
  def getRisksForUser = withUser {
    user => implicit request => {

      /** Get a list of investments for the user to be processed by the Risker */
      val percentageBreakdown = models.Investment.getPercentageBreakdown(user.id)

      /** Create the empty risk holder */
      val risks = ListBuffer[(String,Int)]()

      /** If the user holds investments we can continue */
      if (percentageBreakdown.isDefined) {
        /** Analyse the risk and add it to the Risk List */
        risks.append(("Fund",analyseFundRisk(percentageBreakdown.get)))
      }

      /** Retrieve the target fund percentage breakdown from the database */
      val targetFundBreakdown = models.TargetFund.getTargetFundForUser(user.id)

      /** If the user has a target fund we should retrieve the risk related to it and add it to the risk list */
      if (targetFundBreakdown.isDefined) {
        // TODO Get the users risk from the Risk Appetite table
        risks.append(("Target",2))
      }

      /** Create the Json Risk parser which will format the Risk list into Json to be returned */
      implicit val riskWriter = new Writes[(String,Int)] {
        def writes(risk: (String,Int)) = {
          Json.obj(
            "label" -> JsString(risk._1),
            "risk" -> JsNumber(risk._2)
          )
        }
      }

      /** If there is analysed risks, return them as a Json result */
      if (risks.size.>(0)) Ok(Json.toJson(risks.toList))

      /** No Risk analysis was performed */
      else BadRequest("Risk appetite not found")
    }

  }

}
