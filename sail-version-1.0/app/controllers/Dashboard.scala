/*
 * Copyright (c) 2013. Shannon Holgate.
 *
 * Sail - A personal fund management web application.
 *
 * Makes use of the Play web framework for scala, MongoDB and the salat-Play plugin
 */

package controllers

import play.api.mvc._
import helpers.{Risker, Valuation}
import views.html
import scala.collection.mutable.ListBuffer
import scala.math.BigDecimal.RoundingMode
import play.api.data.Form
import play.api.data.Forms._
import scala.Some


/**
 * Controller for the Dashboard flow.
 */
object Dashboard extends Controller with Secured with Valuation with Risker{

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
   * Renders the dashboard page by getting all the current investments related
   * to the user.
   * Makes use of the Security trait to ensure the user is logged in
   *
   * @return  Result rendering the Dashboard page
   */
  def index = withUser{
    user => implicit request => {

      /** Get a list of investments for the user */
      val investments = models.Investment.getInvestmentForUser(user.id)

      /** Create empty listbuffers to parse investment details into */
      val symbolInvestments = ListBuffer[(String,String,Int,BigDecimal)]()
      val assetClassValues = ListBuffer[(String,BigDecimal)]()

      /** Create temporary values to be used in parsing data for the view */
      var tempAssetClass = Some("") : Option[String]
      var runningTotal = BigDecimal(0)

      /** Create the risk profile of the fund */
      var fundRisk = 0

      /** Ensure the user has investments defined before accessing them */
      if (investments.isDefined) {
        /** Get the percentage breakdown of the current fund */
        val percentageBreakdown = models.Investment.getPercentageBreakdown(user.id)
        fundRisk = analyseFundRisk(percentageBreakdown.get)

        /** find all asset classes with investments that have symbols
          * This will allow us to add any manual investments to the symbol array
          */
        var classesWithSymbols = ListBuffer[(String)]()
        investments.get.foreach(investment => {
          if (investment.symbol.isDefined) {
            if (!classesWithSymbols.exists(sClass => sClass == investment.assetclass)){
              classesWithSymbols.append(investment.assetclass)
            }
          }
        })

        /** Loop through the investments by asset class to parse the data into asset class key value pairs */
        for (investment <- investments.get.sortBy(_.assetclass)) {

          /** If the investment has a symbol, it is automated and the value can be accessed real time.
            * Add this to the Symbol List */
          if (investment.symbol.isDefined && investment.quantity.get.>(0))
            symbolInvestments.append((investment.symbol.get,investment.assetclass,investment.quantity.get,
              investment.value.setScale(2, RoundingMode.CEILING)))
          /** The investment does not have a symbol but it exists in a class with automated values
            * so it should be added to the symbol array */
          else if (classesWithSymbols.exists(sClass => sClass == investment.assetclass))
            symbolInvestments.append(("Manual",investment.assetclass,1,investment.value.setScale(2,
              RoundingMode.CEILING)))

          /** If the investment is in the same asset class as the previous, add it's value */
          if (investment.assetclass != tempAssetClass.get) {
            assetClassValues.append((investment.assetclass,investment.value.setScale(2, RoundingMode.CEILING)))
          }
          /** Otherwise it is a new investment class and should be appended */
          else {
            assetClassValues.update(assetClassValues.size-1, (assetClassValues.last._1, assetClassValues.last._2.+(
              investment.value.setScale(2, RoundingMode.CEILING))))
          }

          /** Update the temporary values */
          tempAssetClass = Some(investment.assetclass)
          runningTotal+=investment.value.setScale(2, RoundingMode.CEILING)
        }
      }

      /** Get the target fund if it exists */
      val targetFund = models.TargetFund.getTargetFundForUser(user.id)
      val riskAppetite = models.RiskAppetite.getRiskAppetiteForUser(user.id)
      /** If the target fund exists, use it in the view, otherwise use an empty list*/
      if (targetFund.isDefined && riskAppetite.isDefined) {
        /** Render the dashboard view, now that the data is ready for it */
        Ok(html.dashboard(user.name,symbolInvestments.toList,assetClassValues.toList,runningTotal,
          fundRisk, targetFund.get, riskAppetite.get,
          addAutoForm,addManualForm,removeForm)).flashing(request.flash)
      }
      else {
        /** Render the dashboard view, now that the data is ready for it */
        Ok(html.dashboard(user.name,symbolInvestments.toList,assetClassValues.toList,runningTotal,
          fundRisk, List[Double](), 0, addAutoForm,addManualForm,removeForm)).flashing(request.flash)
      }
    }
  }
}
