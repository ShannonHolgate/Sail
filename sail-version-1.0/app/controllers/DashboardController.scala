/*
 * Copyright (c) 2013. Shannon Holgate.
 *
 * Sail - A personal fund management web application.
 *
 * Makes use of the Play web framework for scala, MongoDB and the salat-Play plugin
 */

package controllers

import play.api.mvc._
import helpers.{Valuation, Risker}
import views.html
import play.api.libs.json.Json


object DashboardController extends Controller with Risker with Secured with Valuation{

/**  def index = withUser{
    user => implicit request => {
      val investments = Investment.getInvestmentForUser(user)
      val assetClassValues = ListBuffer[(String,BigDecimal)]()
      val shareBondValues = ListBuffer[(String,String,Int)]()
      var tempAssetClass = None : Option[String]
      if (investments.size > 0) {
        for (investment <- investments.sortBy(_.assetclass)) {
          if (investment.quantity.isDefined && investment.quantity.get.>(0)) shareBondValues.append((investment.assetclass, investment.symbol.getOrElse(investment.name), investment.quantity.get))
          else {
            if (investment.assetclass != tempAssetClass.get) {
              assetClassValues.append((investment.assetclass,investment.value.getOrElse(0)))
            }
            else {
              assetClassValues.update(assetClassValues.size-1, (assetClassValues.last._1, assetClassValues.last._2.+(investment.value.getOrElse(0))))
            }
          }
          tempAssetClass = Some(investment.assetclass)
        }
      }

      val homePage: Promise[play.api.libs.ws.Response] = WS.url("http://mysite.com").get()

      val targetFundPercentages = TargetFund.getTargetFundForUser(user)
      var targetRisk = None : Option[Int]
      if (targetFundPercentages.isDefined) targetRisk = Some(analyseRiskAppetite(targetFundPercentages.get))
      val alerts = AlertHistory.getAlertsForInvestments(investments, Some(DateTime.lastMonth.toDate), Some(DateTime.now.toDate))

      var newAlert = false
      for (alert <- alerts) {
        if (!alert.read) {
          newAlert = true
          AlertHistory.markAlertRead(alerts)
        }
      }
      val recentChanges = RecentChange.getRecentChangesForInvestments(investments, Some(DateTime.lastMonth.toDate), Some(DateTime.now.toDate))
      Ok(html.index())

    }
  }   **/
}
