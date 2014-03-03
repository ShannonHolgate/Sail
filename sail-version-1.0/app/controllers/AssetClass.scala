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
import play.api.mvc.Controller
import helpers.Valuation
import org.bson.types.ObjectId
import java.util.Date
import javax.security.auth.callback.LanguageCallback
import org.joda.time.LocalDate

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

  def getTimeSeriesForAssetClass(assetClass:String,assetId:Option[ObjectId] = None,dateFrom:Option[Date] = None,dateTo:Option[Date] = None) = withUser {
    user => implicit request => {
      /** If an assetId is not provided, get all the assets histories between the dates given */
      if (assetId.isEmpty) {
        /** Get a list of investments for the user and assetClass*/
        val investments = models.Investment.getInvestmentForAssetClass(user.id,assetClass)

        if (investments.isDefined) {
          /** Get a list of investment Id's for the automated investments */
          val automatedIds = investments.get.filter(_.symbol.isDefined).map(_.id).distinct

          /** Get the investment histories between the dates given */
          val histories = models.InvestmentHistory.getHistoryForInvestments(investments.get,dateFrom,dateTo)
          /** If a date range exists, filter the histories */
          if (dateFrom.isDefined && dateTo.isDefined) {
            /** Get the automated investments at the dateFrom and dateTo to ensure their values are correctly quoted from yahoo finance */
            if (histories.isDefined) {
              val automatedDateToHistories = histories.get.filter(history => LocalDate.fromDateFields(history.date).equals(LocalDate.fromDateFields(dateFrom.get)) && automatedIds.exists(_.equals(history.investment)))
              //TODO Add a method in the valuator to get stock prices at given dates
              /**
               * Then replace these items in the DATABASE with the new values
               * This should be repeated for the histories at the end of the date range
               * Once complete, a call to models.InvestmentHistory.getInvestmentHistoryForAssetClass to make use of the new data
               * This should complete the assetClass side of the service
               */
            }
          }

        }
      }

      Ok("asdada")

    }
  }
}
