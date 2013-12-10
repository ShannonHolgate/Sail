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

/**
 *  Controller for the TargetFund Model
 *  Holds the web services to be consumed on the dashboard view
 */
object TargetFund extends Controller with Secured{

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
      else BadRequest("No Target Fund Available")
    }
  }
}
