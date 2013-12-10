/*
 * Copyright (c) 2013. Shannon Holgate.
 *
 * Sail - A personal fund management web application.
 *
 * Makes use of the Play web framework for scala, MongoDB and the salat-Play plugin
 */

package controllers

import play.api.mvc._
import helpers.{Valuation}
import views.html
import scala.collection.mutable.ListBuffer
import scala.math.BigDecimal.RoundingMode


/**
 * Controller for the Dashboard flow.
 */
object Dashboard extends Controller with Secured with Valuation{

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

      /** Ensure the user has investments defined before accessing them */
      if (investments.isDefined) {

        /** Loop through the investments by asset class to parse the data into asset class key value pairs */
        for (investment <- investments.get.sortBy(_.assetclass)) {

          /** If the investment has a symbol, it is automated and the value can be accessed real time. Add this to the Symbol List */
          if (investment.symbol.isDefined && investment.quantity.get.>(0))
            symbolInvestments.append((investment.symbol.get,investment.assetclass,investment.quantity.get,investment.value.setScale(2, RoundingMode.CEILING)))

          /** If the investment is in the same asset class as the previous, add it's value */
          if (investment.assetclass != tempAssetClass.get) {
            assetClassValues.append((investment.assetclass,investment.value.setScale(2, RoundingMode.CEILING)))
          }
          /** Otherwise it is a new investment class and should be appended */
          else {
            assetClassValues.update(assetClassValues.size-1, (assetClassValues.last._1, assetClassValues.last._2.+(investment.value.setScale(2, RoundingMode.CEILING))))
          }

          /** Update the temporary values */
          tempAssetClass = Some(investment.assetclass)
          runningTotal+=investment.value.setScale(2, RoundingMode.CEILING)
        }
      }

      /** Render the dashboard view, now that the data is ready for it */
      Ok(html.dashboard(user.name,symbolInvestments.toList,assetClassValues.toList,runningTotal))
    }
  }
}
