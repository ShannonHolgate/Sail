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

object RiskFinder extends Controller with Secured with Risker{

  def getRisksForUser = withUser {
    user => implicit request => {
      val investments = models.Investment.getInvestmentForUser(user.id)
      val percentageList = ListBuffer[BigDecimal]()
      var tempClass = ""
      var runningTotal:BigDecimal = 0
      val risks = ListBuffer[(String,Int)]()
      if (investments.isDefined) {
        for (investment <- investments.get.sortBy(_.assetclass)) {
          if (percentageList.isEmpty || !investment.assetclass.equals(tempClass)) percentageList.append(investment.value)
          else percentageList.update(percentageList.size-1,percentageList.last+investment.value)
          tempClass = investment.assetclass
          runningTotal+=investment.value
        }
        for ((value, index) <- percentageList.zipWithIndex) {
          val percentageOfTotal = (value/runningTotal)*100
          percentageList.update(index,percentageOfTotal)
        }
        risks.append(("Fund",analyseRiskAppetite(percentageList.toList)))
      }
      val targetFundBreakdown = models.TargetFund.getTargetFundForUser(user.id)
      if (targetFundBreakdown.isDefined) {
        risks.append(("Target",analyseRiskAppetite(targetFundBreakdown.get)))
      }

      implicit val riskWriter = new Writes[(String,Int)] {
        def writes(risk: (String,Int)) = {
          Json.obj(
            "label" -> JsString(risk._1),
            "risk" -> JsNumber(risk._2)
          )
        }
      }

      if (risks.size.>(0)) Ok(Json.toJson(risks.toList))
      else BadRequest("Risk appetite not found")
    }

  }

}
