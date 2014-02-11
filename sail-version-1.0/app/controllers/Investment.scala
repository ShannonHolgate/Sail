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
import org.bson.types.ObjectId
import play.i18n.Messages

/**
 *  Controller for the Investment Model
 *  Holds the web services to be consumed on the dashboard view
 */
object Investment extends Controller with Secured with Valuation{

  /** auto add investment form to bind the index view */
  val addAutoForm = Form(
    tuple(
      "investmentresults" -> text,
      "quantity" -> number,
      "assetclass" -> text
    ) verifying (Messages.get("error.investment.quantity",0.toString), result => result._2 match {
      case (quantity) => quantity.>(0)
    }) verifying(Messages.get("error.investment.noasset"), result => result._3 match {
      case (assetClass) => matchAssetClass(assetClass)
    })
  )

  val assetClasses = Array[String](Messages.get("view.index.shares"),
    Messages.get("view.index.bonds"),
    Messages.get("view.index.bank"),
    Messages.get("view.index.commodities"),
    Messages.get("view.index.collectibles"),
    Messages.get("view.index.property"))

  /**
   * Pattern match to ensure added and removed investments exist in the provided asset Classes only
   *
   * @param x String the asset class to be checked
   * @return  Boolean whether the asset class should be used
   */
  def matchAssetClass(x: String): Boolean = {
    assetClasses.contains(x)
  }

  /** manual add investment form to bind the dashboard view */
  val addManualForm = Form(
    tuple(
      "name" -> text,
      "currentvalue" -> bigDecimal,
      "assetclass" -> text
    ) verifying (Messages.get("error.investment.value"), result => result._2 match {
      case (value) => value.>=(0)
    }) verifying(Messages.get("error.investment.noasset"), result => result._3 match {
      case (assetClass) => matchAssetClass(assetClass)
    })
  )

  /** remove investment form to bind the dashboard view */
  val removeForm = Form(
    tuple(
      "id" -> text,
      "quantity" -> optional(number),
      "value" -> optional(bigDecimal),
      "removeallbool" -> boolean,
      "password" -> text
    ) verifying (Messages.get("error.investment.noinvestmentform"), result => result._1 match {
      case (id) => models.Investment.getOne(new ObjectId(id)).isDefined
    }) verifying (Messages.get("error.investment.value"), result => result._3 match {
      case (value) => { if (value.isDefined) value.get.>=(0) else true
      }
    })
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

  /**
   * Web Service to find ticker symbols based on the query string.
   * Makes use of the findTickerSymbols function in the Valuation helper
   * Retrieves a results set from Yahoo Finance
   *
   * @param query String the name or symbol to query Yahoo Finance for so a results set can be
   *              found
   * @return      Result Json array holding the ticker symbols returned from the Valuation
   *              Helper
   */
  def getTickerSymbolService(query:String) = withUser {
    user => implicit request => {
      /** Find the Json string of results */
      Ok(findTickerSymbols(query).get)
    }
  }

  /**
   * Web Service to return a list of investments in Json format for the user depending
   * on the asset class string provided
   *
   * @param assetClass  String the asset class to find the investments for
   * @return            Result Json array of investments if any are found
   */
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
              "quantity" -> JsNumber({if (investment.quantity.isDefined) investment.quantity.get.toDouble else 1.0}),
              "value" -> JsNumber(investment.value)
            )
          }
        }

        /** Return the Json string to the browser */
        Ok(Json.toJson(investments.get))
      }
      else
      /** No Results were found */
        Ok("No Results")
    }
  }

  /**
   * Adds a new investment to for the user based on the form bound on the index view
   * Takes the addAuto form and binds it to ensure no errors are found
   * The form is then split to check if the investment already exists. If it does, update the quantity.
   * If not, add a new investment with the given asset class and add a new row to the investment history table
   *
   * @return  Result redirecting the user to the Dashboard flashing a success or failure message
   */
  def addAuto = withUser {
    user => implicit request => {
      addAutoForm.bindFromRequest().fold(
        formWithErrors => Redirect(routes.Dashboard.index).flashing(configValues.genericError -> formWithErrors.errors(0).message),
        autoBound => {
          /** Split the name-symbol string */
          val investmentName = autoBound._1.split("~").last
          val investmentSymbol = autoBound._1.split("~").head

          /** Check if the symbol already exists in the database */
          val existingInvestment = models.Investment.getOneFromSymbol(investmentSymbol,user)
          if (existingInvestment.isDefined) {
            /** The investment exists, update the investment and redirect to the dashboard*/
            if (models.Investment.updateInvestmentValue(existingInvestment.get.copy(quantity = Some(autoBound._2)))) Redirect(routes.Dashboard.index).flashing(configValues.genericSuccess -> Messages.get("success.investment.updated",investmentName))
            /** The update failed - This should not happen */
            else Redirect(routes.Dashboard.index).flashing(configValues.genericError ->  Messages.get("error.investment.updatingautofail",investmentName))
          }
          else {

            /** This is a completely new investment
              * Get the real time value using the valuation helper
              * */
            val investmentsWithValue = getSymbolValuesWithQuantity(List[String](investmentSymbol),List[(String,Int)]((investmentSymbol,autoBound._2)))

            /** Ensure a real time value can be found - This is likely to fail as not all symbols carry a quote */
            if (investmentsWithValue.isDefined) {

              /** A quote was found, add a new investment to the database */
              val investmentId = models.Investment.createOne(quantity=Some(autoBound._2),value=investmentsWithValue.get.head.value,assetClass = autoBound._3,name=investmentName,symbol=Some(investmentSymbol),user=user)

              /** Ensure the insert was successful */
              if (investmentId.isDefined) {

                /** The insert was successful - Add a new investment history */
                if (models.InvestmentHistory.createToday(investmentsWithValue.get.head.value,investmentsWithValue.get.head.value,investmentId.get,Some(autoBound._2)))

                  /** Adding an investment history was successful - Redirect to the dashbaord */
                  Redirect(routes.Dashboard.index).flashing(configValues.genericSuccess -> Messages.get("success.investment.added",investmentName))
                else
                  /** Adding an investment history was unsuccessful - This should not happen */
                  Redirect(routes.Dashboard.index).flashing(configValues.genericError -> Messages.get("error.investment.historyaddfail",investmentName))
              }
              else
                /** The insert was unsuccessful - This should not happen */
                Redirect(routes.Dashboard.index).flashing(configValues.genericError -> Messages.get("error.investment.addautofail",investmentName))
            }
            else
              /** Retrieving a quote for the investment was unsuccessful - Back out and show the failure */
              Redirect(routes.Dashboard.index).flashing(configValues.genericError -> Messages.get("error.investment.quotefail",investmentName))
          }
        }
      )
    }
  }

  /**
   * Adds a new investment to for the user based on the form bound on the index view
   * Takes the addManual form and binds it to ensure no errors are found
   * The form is then split to check if the investment already exists. If it does, stop the update and alert the user.
   * If not, add a new investment with the given asset class and add a new row to the investment history table
   *
   * @return  Result redirecting the user to the Dashboard flashing a success or failure message
   */
  def addManual = withUser {
    user => implicit request => {
      addManualForm.bindFromRequest().fold(
        formWithErrors => Redirect(routes.Dashboard.index).flashing(configValues.genericError -> formWithErrors.errors(0).message),
        manualBound  => {

          /** Check if the symbol already exists in the database */
          val existingInvestment = models.Investment.getOneFromName(manualBound._1,user)

          /** The investment already exists - Redirect to the Dashboard and alert the user */
          if (existingInvestment.isDefined) Redirect(routes.Dashboard.index).flashing(configValues.genericError -> Messages.get("error.investment.manualexists",manualBound._1))
          else {
            /** This is a new investment - Add it to the database */
            val investmentId = models.Investment.createOne(quantity=None,value=manualBound._2.setScale(2, RoundingMode.CEILING),assetClass = manualBound._3,name=manualBound._1,symbol=None,user=user)

            /** Ensure the insert was successful */
            if (investmentId.isDefined) {

              /** The insert was successful - Insert a new row into the Investment History table */
              if (models.InvestmentHistory.createToday(manualBound._2.setScale(2, RoundingMode.CEILING),manualBound._2.setScale(2, RoundingMode.CEILING),investmentId.get,None))
                /** The insert was successful - Redirect to the Dashboard view */
                Redirect(routes.Dashboard.index).flashing(configValues.genericSuccess -> Messages.get("success.investment.added",manualBound._1))
              else
                /** The insert failed - This should not happen */
                Redirect(routes.Dashboard.index).flashing(configValues.genericError -> Messages.get("error.investment.historyaddfail",manualBound._1))
            }
            else
              /** The new Investment insert failed - This should not happen */
              Redirect(routes.Dashboard.index).flashing(configValues.genericError -> Messages.get("error.investment.addautofail",manualBound._1))
          }
        }
      )
    }
  }

  /**
   * Removes or Updates an investment for the user based on the form bound on the index view
   * Takes the remove form and binds it to ensure no errors are found
   * The form is then split to ensure the investment exists. If it does, continue to
   * update the investment.
   * Either remove the whole investment based on the remove all flag in the form or update it
   * based on either the input value or quantity
   *
   * @return  Result redirecting the user to the Dashboard flashing a success or failure message
   */
  def remove = withUser {
    user => implicit request => {
      removeForm.bindFromRequest().fold(
        formWithErrors => Redirect(routes.Dashboard.index).flashing(configValues.genericError -> formWithErrors.errors(0).message),
        removeBound => {

          /** Ensure the password input matches the user */
          if (models.User.authenticate(email(request).getOrElse("None"), removeBound._5).isDefined) {

            /** The password is correct - Get the investment from the database */
            val investment = models.Investment.getOne(new ObjectId(removeBound._1))

            /** Check if the remove all flag is true */
            if (removeBound._4) {
              /** The remove all flag is true - Remove the investment from the database and add a new history setting this investment
                * to a value of zero
                */
              if (models.Investment.removeOne(investment.get.id)) {

                /** The removal was successful - Add the history */
                if (models.InvestmentHistory.createToday(BigDecimal(0),investment.get.value,investment.get.id,Some(0)))

                  /** Adding the new Investment history was successful - Redirect to the dashboard */
                  Redirect(routes.Dashboard.index).flashing(configValues.genericSuccess -> Messages.get("success.investment.removed",investment.get.name))

                else
                  /** Adding the new history failed - This should not happen */
                  Redirect(routes.Dashboard.index).flashing(configValues.genericError -> Messages.get("error.investment.removehistoryfail",investment.get.name))
              }
              else
                /** The investment removal failed - This should not happen */
                Redirect(routes.Dashboard.index).flashing(configValues.genericError -> Messages.get("error.investment.removefail",investment.get.name))
            }
            /** check if a quantity exists in the form - Update an automated investment */
            else if (removeBound._2.isDefined) {

              /** A quantity exists - Ensure the current investment is also an automated investment */
              if (investment.get.quantity.isDefined && investment.get.symbol.isDefined) {

                /** Get the new quantity from the form */
                val newQuantity = removeBound._2.get

                /** Get the current value of the investment based on the quantity in the form and the symbol - Use the Valuation Helper*/
                val investmentsWithValue = getSymbolValuesWithQuantity(List[String](investment.get.symbol.get),List[(String,Int)]((investment.get.symbol.get,newQuantity)))

                /** Check if the new investment value was retrieved */
                if (investmentsWithValue.isDefined) {

                  /** Getting the new value was successful - Update the investment in the database */
                  if (models.Investment.updateInvestmentValue(investment.get.copy(quantity = Some(newQuantity),value = investmentsWithValue.get.head.value))) {

                    /** The insert was successful - Update the investment history for the investment */
                    if (models.InvestmentHistory.createToday(investmentsWithValue.get.head.value,investment.get.value.-(investmentsWithValue.get.head.value).setScale(2, RoundingMode.CEILING),investment.get.id,Some(newQuantity)))

                      /** Inserting the new history was successful - redirect to the dashboard */
                      Redirect(routes.Dashboard.index).flashing(configValues.genericSuccess -> Messages.get("success.investment.quantity",investment.get.name))
                    else
                      /** Adding a new history failed - This should not happen */
                      Redirect(routes.Dashboard.index).flashing(configValues.genericError -> Messages.get("error.investment.updatehistoryfail",investment.get.name))
                  }
                  else
                    /** The Insert failed - This should not happen */
                    Redirect(routes.Dashboard.index).flashing(configValues.genericError -> Messages.get("error.investment.quantityupdate",investment.get.name))
                }
                else
                  /** A quote could not be retireved - Alert the user */
                  Redirect(routes.Dashboard.index).flashing(configValues.genericError -> Messages.get("error.investment.quotefail",investment.get.name))
              }
              else
                /** The current investment is not automated */
                Redirect(routes.Dashboard.index).flashing(configValues.genericError -> Messages.get("error.investment.investmentnotauto",investment.get.name))
            }
            /** If a value exists on the form it is a manual investment */
            else if (removeBound._3.isDefined) {

              /** This is a manual investment
                * Update the value from the form */
              if (models.Investment.updateInvestmentValue(investment.get.copy(value = removeBound._3.get))) {

                /** Updating the value was successful - add a new history for the investment */
                if (models.InvestmentHistory.createToday(removeBound._3.get,investment.get.value.-(removeBound._3.get).setScale(2, RoundingMode.CEILING),investment.get.id,None))

                  /** Inserting the new history was successful - Redirec to the dashbaord */
                  Redirect(routes.Dashboard.index).flashing(configValues.genericSuccess -> Messages.get("success.investment.value",investment.get.name))
                else
                  /** Inserting the history failed - This should not happen */
                  Redirect(routes.Dashboard.index).flashing(configValues.genericError -> Messages.get("error.investment.updatehistoryfail",investment.get.name))
              }
              else
                /** Updating the investment value failed - This should not happen */
                Redirect(routes.Dashboard.index).flashing(configValues.genericError -> Messages.get("error.investment.valueupdatefail",investment.get.name))
            }
            else
              /** Neither a Quantity or Value is available - Alert the user */
              Redirect(routes.Dashboard.index).flashing(configValues.genericError -> Messages.get("error.investment.valuequantityinvalid"))
          }
          else
            /** The password entered is incorrect */
            Redirect(routes.Dashboard.index).flashing(configValues.genericError -> Messages.get("error.investment.password"))
        }
      )
    }
  }
}
