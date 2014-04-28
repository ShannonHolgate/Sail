/*
 * Copyright (c) 2013. Shannon Holgate.
 *
 * Sail - A personal fund management web application.
 *
 * Makes use of the Play web framework for scala, MongoDB and the salat-Play plugin
 */

package controllers

import play.api.mvc.Controller
import play.api.libs.json.{JsNumber, JsString, Json, Writes}
import views.html
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.JsNumber
import play.i18n.Messages
import helpers.Risker

/**
 *  Controller for the TargetFund Model
 *  Holds the web services to be consumed on the dashboard view
 */
object TargetFund extends Controller with Secured with Risker{

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

  /**
   * Routes to the Target Fund screen if a target fund exists
   * If no target fund exists, redirect to the Risk Appetite Questionnaire
   *
   * @return  Result Rendering the Target Fund screen or Redirecting to the Risk appetite flow
   */
  def index = withUser{
    user => implicit request => {
      /** Retrieve the target fund from the database */
      val targetFund = models.TargetFund.getTargetFundForUser(user.id)
      val riskAppetite = models.RiskAppetite.getRiskAppetiteForUser(user.id)
      /** If the target fund exists, render the target fund page */
      if (targetFund.isDefined && riskAppetite.isDefined) {
        /** Get the percentage breakdown of the current fund */
        val percentageBreakdown = models.Investment.getPercentageBreakdown(user.id)
        if (percentageBreakdown.isDefined)
          Ok(html.targetfund(user.name,Messages.get("view.target.pagetitle"),Messages.get("view.target.header"),
            percentageBreakdown.get.sortBy(_._1).map(_._2).toList,targetFund.get,
            analyseFundRisk(percentageBreakdown.get),
            riskAppetite.get,
            addAutoForm,addManualForm,removeForm)).flashing(request.flash)
        else
        /** If the percentage breakdown does not exist, the user has no investments
          * Notify the user on the frontend
          */
          Ok(html.targetfund(user.name,Messages.get("view.target.pagetitle"),Messages.get("view.target.header"),
            List[BigDecimal](),targetFund.get,
            0,
            riskAppetite.get,
            addAutoForm,addManualForm,removeForm)).flashing(request.flash)

      }
      else
        /** Redirect to the Risk Appetite Questionnaire */
        Redirect(routes.RiskAppetite.index).flashing(configValues.genericError ->
          Messages.get("error.risk.none"))
    }
  }

  /**
   * Web Service Getting the target fund percentages from the database.
   * Uses the cookie sent in the request to find the user
   *
   * @return  Result Json result containing the percentage breakdown of the target fund
   */
  def getTargetFund = withUser {
    user => implicit request => {

      /** Create the Json Parser to map the target fund percentages to Json */
      implicit val targetFundWriter = new Writes[BigDecimal] {
        def writes(assetPercentage:BigDecimal) = {
          Json.obj(
          "percentage" -> JsNumber(assetPercentage)
          )
        }
      }

      /** Retrieve the target fund from the database */
      val targetFund = models.TargetFund.getTargetFundForUser(user.id)

      /** If the user has created a target fund we should map it to Json and return it */
      if (targetFund.isDefined) Ok(Json.toJson(targetFund.get))

      /** The user has not created a target fund */
      else BadRequest(Messages.get("error.target.none"))
    }
  }
}
