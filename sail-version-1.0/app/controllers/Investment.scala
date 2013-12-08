/*
 * Copyright (c) 2013. Shannon Holgate.
 *
 * Sail - A personal fund management web application.
 *
 * Makes use of the Play web framework for scala, MongoDB and the salat-Play plugin
 */

package controllers

import play.api.mvc.Controller
import helpers.{InvestmentWithValue, Valuation}
import scala.collection.mutable.ListBuffer
import scala.math.BigDecimal.RoundingMode
import play.api.libs.json._

object Investment extends Controller with Secured with Valuation{

  def getAutomatedInvestmentValues = withUser {
    user => implicit request => {
      val investmentsWithSymbols = models.Investment.getInvestmentsWithSymbols(user.id)
      if (investmentsWithSymbols.isDefined) {
        val symbolList = new ListBuffer[String]()
        val symbolQuantities = new ListBuffer[(String,Int)]()
        for (investment <- investmentsWithSymbols.get) {
          symbolList.append(investment.symbol.getOrElse(""))
          symbolQuantities.append((investment.symbol.getOrElse(""),investment.quantity.getOrElse(0)))
        }
        val realTimeInvestmentValues = getSymbolValuesWithQuantity(symbolList.toList,symbolQuantities.toList)
        if (realTimeInvestmentValues.isDefined) {
          for (investment <-investmentsWithSymbols.get) {
            val realTimeInvestment = realTimeInvestmentValues.get.find{item => item.formattedInvestment.symbol.equals(investment.symbol.get)}
            if (realTimeInvestment.isDefined) {
              if (realTimeInvestment.get.value.setScale(2, RoundingMode.CEILING) != investment.value.setScale(2, RoundingMode.CEILING)) {
                val updatedInvestment = investment.copy(value = realTimeInvestment.get.value.setScale(2, RoundingMode.CEILING))

                models.InvestmentHistory.createToday(realTimeInvestment.get.value.setScale(2, RoundingMode.CEILING),
                  realTimeInvestment.get.value.setScale(2, RoundingMode.CEILING).-(investment.value.setScale(2, RoundingMode.CEILING)).toDouble,
                  investment.id,
                  investment.quantity)

                models.Investment.updateInvestmentValue(updatedInvestment)
              }
            }
          }
          implicit val realTimeValueWrites = new Writes[InvestmentWithValue] {
            def writes(investmentWithValue: InvestmentWithValue) = {
              Json.obj(
                "symbol" -> JsString(investmentWithValue.formattedInvestment.symbol),
                "value" -> JsNumber(investmentWithValue.value),
                "quantity" -> JsNumber(investmentWithValue.quantity),
                "price" -> JsNumber(investmentWithValue.formattedInvestment.closingPrice),
                "exchange" -> JsString(investmentWithValue.formattedInvestment.exchange)
              )
            }
          }
          Ok(Json.toJson(realTimeInvestmentValues.get))
        }
        else BadRequest("No data available")
      }
      else BadRequest("No data available")
    }
  }
}
