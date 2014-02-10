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
import play.api.data.Form
import play.api.data.Forms._
import helpers.InvestmentWithValue
import play.api.libs.json.JsString
import play.api.libs.json.JsNumber

/**
 *  Controller for the Investment Model
 *  Holds the web services to be consumed on the dashboard view
 */
object Investment extends Controller with Secured with Valuation{

  /** auto add form to bind the dashboard view */
  val addAutoForm = Form(
    tuple(
      "investmentresults" -> text,
      "quantity" -> number,
      "assetclass" -> text
    ) verifying ("Quantity must be greater than 0", result => result._2 match {
      case (quantity) => quantity.>(0)
    }) verifying("Something went wrong, that asset class does not exist", result => result._3 match {
      case (assetClass) => matchAssetClass(assetClass)
    })
  )

  def matchAssetClass(x: String): Boolean = x match {
    case "Shares" => true
    case "Bonds" => true
    case "Commodities" => true
    case "Bank Accounts"  => true
    case "Collectibles"  => true
    case "Property"  => true
    case _ => false
  }

  /** manual add form to bind the dashboard view */
  val addManualForm = Form(
    tuple(
      "name" -> text,
      "currentvalue" -> bigDecimal,
      "assetclass" -> text
    )
  )

  /** remove form to bind the dashboard view */
  val removeForm = Form(
    tuple(
      "id" -> text,
      "quantity" -> number,
      "value" -> bigDecimal,
      "removeall" -> boolean,
      "password" -> text
    )
  )

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
                "value" -> JsNumber(investmentWithValue.value.setScale(2, RoundingMode.CEILING)),
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

  def getTickerSymbolService(query:String) = withUser {
    user => implicit request => {
      /** Find the Json string of results */
      Ok(findTickerSymbols(query).get)
    }
  }

  def getInvestmentList(assetClass:String) = withUser {
    user => implicit request => {
      val investments = models.Investment.getInvestmentForAssetClass(user.id,assetClass)
      if (investments.isDefined) {
        /** Create the Json parser which will write the realtime investments in a stock format */
        implicit val investmentWrites = new Writes[models.Investment] {
          def writes(investment: models.Investment) = {
            Json.obj(
              "id" -> JsString(investment.id.toString),
              "name" -> JsString(investment.name),
              "symbol" -> JsString(investment.symbol.getOrElse("")),
              "quantity" -> JsNumber({if (investment.quantity.isDefined) investment.quantity.get.toDouble else 1.0})
            )
          }
        }

        /** Return the Json string to the browser */
        Ok(Json.toJson(investments.get))
      }
      else
        Ok("No Results")
    }
  }

  def addAuto = withUser {
    user => implicit request => {
      addAutoForm.bindFromRequest().fold(
        formWithErrors => Redirect(routes.Dashboard.index).flashing(configValues.genericError -> formWithErrors.errors(0).message),
        autoBound => {
          val investmentName = autoBound._1.split("~").last
          val investmentSymbol = autoBound._1.split("~").head
          val existingInvestment = models.Investment.getOneFromSymbol(investmentSymbol,user)
          if (existingInvestment.isDefined) {
            if (models.Investment.updateInvestmentValue(existingInvestment.get.copy(quantity = Some(autoBound._2)))) Redirect(routes.Dashboard.index).flashing(configValues.genericSuccess -> "Investment Updated")
            else Redirect(routes.Dashboard.index).flashing(configValues.genericError -> "Updating the existing investment failed")
          }
          else {
            val investmentsWithValue = getSymbolValuesWithQuantity(List[String](investmentSymbol),List[(String,Int)]((investmentSymbol,autoBound._2)))
            if (investmentsWithValue.isDefined) {
              val investmentId = models.Investment.createOne(quantity=Some(autoBound._2),value=investmentsWithValue.get.head.value,assetClass = autoBound._3,name=investmentName,symbol=Some(investmentSymbol),user=user)
              if (investmentId.isDefined) {
                if (models.InvestmentHistory.createToday(investmentsWithValue.get.head.value,investmentsWithValue.get.head.value,investmentId.get,Some(autoBound._2)))
                  Redirect(routes.Dashboard.index).flashing(configValues.genericSuccess -> "New Investment added")
                else
                  Redirect(routes.Dashboard.index).flashing(configValues.genericError -> "Could not create a history for the new investment")
              }
              else
                Redirect(routes.Dashboard.index).flashing(configValues.genericError -> "Adding the new investment failed")
            }
            else
              Redirect(routes.Dashboard.index).flashing(configValues.genericError -> "Failed to get a quote for this investment")
          }
        }
      )
    }
  }

  def addManual = withUser {
    user => implicit request => {
      Ok("hey")
    }
  }

  def remove = withUser {
    user => implicit request => {
      Ok("hey")
    }
  }
}
