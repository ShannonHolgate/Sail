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

object TargetFund extends Controller with Secured{

  def getTargetFund = withUser {
    user => implicit request => {
      implicit val targetFundWriter = new Writes[BigDecimal] {
        def writes(assetPercentage:BigDecimal) = {
          Json.obj(
          "percentage" -> JsNumber(assetPercentage)
          )
        }
      }
      val targetFund = models.TargetFund.getTargetFundForUser(user.id)
      if (targetFund.isDefined) Ok(Json.toJson(targetFund.get))
      else BadRequest("No Target Fund Available")
    }
  }
}
