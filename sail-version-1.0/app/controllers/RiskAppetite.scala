/*
 * Copyright (c) 2014. Shannon Holgate.
 *
 * Sail - A personal fund management web application.
 *
 * Makes use of the Play web framework for scala, MongoDB and the salat-Play plugin
 */

package controllers

import play.mvc.Controller
import views.html
import play.api.data.Form
import play.api.data.Forms._
import play.i18n.Messages
import play.api.Play.current
import helpers.Risker

/**
 * Controller for the Risk Appetite questionaire
 */
object RiskAppetite extends Controller with Secured with Risker{

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

  /** Risk Appetite form */
  val riskForm = Form(
    tuple(
      "one" -> text,
      "two" -> text,
      "three" -> text,
      "four" -> text,
      "five" -> text,
      "six" -> text,
      "seven" -> text,
      "eight" -> text,
      "nine" -> text,
      "ten" -> text,
      "eleven" -> text,
      "twelve" -> text
    )
  )

  /**
   * Routes to the Risk Appetite view
   * Ensures the user is already logged in
   *
   * @return  Result rendering the risk appetite view
   */
  def index = withUser{
    user => implicit request => {
      /** Render the Risk Appetite View */
      Ok(html.riskappetite(user.name,Messages.get("view.risk.pagetitle"),Messages.get("view.risk.header"),
        addAutoForm,addManualForm,removeForm)).flashing(request.flash)
    }
  }

  /**
   * Risk Form submission action to collect the responses and determine the investors
   * risk appetite
   * Binds the form the distinguishes the points on each question to set against a scale
   *
   * @return Result directing flow to the Target Fund screen using the newly found risk appetite
   */
  def submit = withUser{
    user => implicit request => {
      riskForm.bindFromRequest().fold(
        formWithErrors => Redirect(routes.Dashboard.index).flashing(configValues.genericSuccess -> "failure"),
        riskBound => {
          /** Create a temporary empty total */
          var riskTotal = 0
          try {
            /** Get the score from the risk form */
            riskBound.productIterator.map(_.asInstanceOf[String]).foreach(score => riskTotal+=score.toInt)
            /** Get the integer representation of the investors risk appetite */
            val riskAppetite = analyseRiskAppetite(riskTotal)
            /** Save the risk appetite and question answers to the database */
            models.RiskAppetite.addRiskAppetiteForUser(user.id,
              riskBound.productIterator.map(_.asInstanceOf[String]).toList,riskAppetite)
            /** Get the target fund and save it to the database for the user */
            models.TargetFund.addTargetFundForUser(user.id,getTargetFundForRiskAppetite(riskAppetite))
            /** Direct flow to the new Target Fund */
            val successMessage = "You are a " + Messages.get("view.target.risk."+riskAppetite) +
              " Investor, here is a suggested target fund"
            Redirect(routes.TargetFund.index).flashing(configValues.genericSuccess ->
              successMessage)
          } catch {
            case nfe: NumberFormatException => {
              /** Someone messed with the form being sent */
              Redirect(routes.RiskAppetite.index).flashing(configValues.genericSuccess ->
                "An error occured collecting your answers")
            }
          }
        }
      )
    }
  }

}
