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

/**
 *  Controller for the Investment Model
 *  Holds the web services to be consumed on the dashboard view
 */
object Investment extends Controller with Secured with Valuation{

  /**
   * Web Service getting the automated investments.
   * These are investments with symbols which can be retrieved in real time.
   * Uses the cookie sent by the request to get the user's investments.
   *
   * @return  Result Json result containing an array of Json objects mapping out the
   *          automated investment and it's details
   */
  def getAutomatedInvestmentValues = withUser {
    user => implicit request => {

      /** Get the automated investments for the user */
      val investmentsWithSymbols = models.Investment.getInvestmentsWithSymbols(user.id)

      /** If the user has automated investments, we can get the real time value */
      if (investmentsWithSymbols.isDefined) {

        /** Create empty listbuffers which will gather automated investment details to be consumed by the valuator */
        val symbolList = new ListBuffer[String]()
        val symbolQuantities = new ListBuffer[(String,Int)]()

        /** Gather the investment symbols and add them to the symbol list */
        /** Gather symbol and value pairs for the symbol quantities */
        for (investment <- investmentsWithSymbols.get) {
          symbolList.append(investment.symbol.getOrElse(""))
          symbolQuantities.append((investment.symbol.getOrElse(""),investment.quantity.getOrElse(0)))
        }

        /** Use the symbols and quantities to retrieve the real time value of each */
        val realTimeInvestmentValues = getSymbolValuesWithQuantity(symbolList.toList,symbolQuantities.toList)

        /** upon success, check if the value has changed from the value retrieved from the database.
          * If so, add a new investment history row to show the change and update the investment value in the database
          */
        if (realTimeInvestmentValues.isDefined) {

          /** Loop through the users automated investments */
          for (investment <-investmentsWithSymbols.get) {

            /** Find the real time object returned to map against the user's automated investments */
            val realTimeInvestment = realTimeInvestmentValues.get.find{item => item.formattedInvestment.symbol.equals(investment.symbol.get)}

            /** If the real time investment exists we can continue to process */
            if (realTimeInvestment.isDefined) {

              /** Check if the real time value has differed from what we retrieved from the database */
              if (realTimeInvestment.get.value.setScale(2, RoundingMode.CEILING) != investment.value.setScale(2, RoundingMode.CEILING)) {

                /** Update the automated investment to contain the new real time value so we can save it to the database */
                val updatedInvestment = investment.copy(value = realTimeInvestment.get.value.setScale(2, RoundingMode.CEILING))

                /** Add a new investment history to the database showing the change in value */
                models.InvestmentHistory.createToday(realTimeInvestment.get.value.setScale(2, RoundingMode.CEILING),
                  realTimeInvestment.get.value.setScale(2, RoundingMode.CEILING).-(investment.value.setScale(2, RoundingMode.CEILING)).toDouble,
                  investment.id,
                  investment.quantity)

                /** Update the investment in the database to show the new value */
                models.Investment.updateInvestmentValue(updatedInvestment)
              }
            }
          }

          /** Create the Json parser which will write the realtime investments in a stock format */
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

          /** Return the Json string to the browser */
          Ok(Json.toJson(realTimeInvestmentValues.get))
        }

        /** No realtime investments were returned */
        else BadRequest("No data available")
      }

      /** The user has no automated investments */
      else BadRequest("No data available")
    }
  }
}
