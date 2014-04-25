/*
 * Copyright (c) 2013. Shannon Holgate.
 *
 * Sail - A personal fund management web application.
 *
 * Makes use of the Play web framework for scala, MongoDB and the salat-Play plugin
 */

package helpers

import play.i18n.Messages
import scala.collection.immutable.{TreeMap, HashMap}
import play.api.Play.current
import play.api.Play
import scala.util.control.Breaks._
import scala.collection.mutable._

/**
 * Risk Helper class to analyse the risk based on a percentage breakdown of assets
 */
trait Risker {

  /** Map the asset classes to set the risk for. Includes the risk factor*/
  val assetClasses = HashMap(Messages.get("view.index.shares") -> 6,
    Messages.get("view.index.bonds") -> 2,
    Messages.get("view.index.bank") -> 1,
    Messages.get("view.index.commodities") -> 5,
    Messages.get("view.index.collectibles") -> 3,
    Messages.get("view.index.property") -> 4)

  /** Map the asset class risk scores to compare against the risk score evaluated */
  val assetRiskScale = TreeMap(1 -> Play.application.configuration.getString("asset.verylow").get.toInt,
    2 -> Play.application.configuration.getString("asset.low").get.toInt,
    3 -> Play.application.configuration.getString("asset.average").get.toInt,
    4 -> Play.application.configuration.getString("asset.high").get.toInt,
    5 -> Play.application.configuration.getString("asset.veryhigh").get.toInt)

  /** Map the risk scoring thresholds from the config file */
  val riskAppetiteScale = TreeMap(1 -> Play.application.configuration.getString("risk.verylow").get.toInt,
    2 -> Play.application.configuration.getString("risk.low").get.toInt,
    3 -> Play.application.configuration.getString("risk.average").get.toInt,
    4 -> Play.application.configuration.getString("risk.high").get.toInt,
    5 -> Play.application.configuration.getString("risk.veryhigh").get.toInt)

  /** Map out the target fund percentages for each risk level */
  val targetFunds = TreeMap(1 -> List[(String,Double)]((Messages.get("view.index.shares"),0),
      (Messages.get("view.index.bonds"),0),
      (Messages.get("view.index.bank"), 100),
      (Messages.get("view.index.commodities"), 0),
      (Messages.get("view.index.collectibles"), 0),
      (Messages.get("view.index.property"), 0)),
    2 -> List[(String,Double)]((Messages.get("view.index.shares"),1),
      (Messages.get("view.index.bonds"),20),
      (Messages.get("view.index.bank"), 48),
      (Messages.get("view.index.commodities"), 1),
      (Messages.get("view.index.collectibles"), 0),
      (Messages.get("view.index.property"), 30)),
    3 -> List[(String,Double)]((Messages.get("view.index.shares"),10),
      (Messages.get("view.index.bonds"),10),
      (Messages.get("view.index.bank"), 42),
      (Messages.get("view.index.commodities"), 3),
      (Messages.get("view.index.collectibles"), 5),
      (Messages.get("view.index.property"), 30)),
    4 -> List[(String,Double)]((Messages.get("view.index.shares"),20),
      (Messages.get("view.index.bonds"),5),
      (Messages.get("view.index.bank"), 15),
      (Messages.get("view.index.commodities"), 10),
      (Messages.get("view.index.collectibles"), 10),
      (Messages.get("view.index.property"), 40)),
    5 -> List[(String,Double)]((Messages.get("view.index.shares"),40),
      (Messages.get("view.index.bonds"),5),
      (Messages.get("view.index.bank"), 10),
      (Messages.get("view.index.commodities"), 20),
      (Messages.get("view.index.collectibles"), 5),
      (Messages.get("view.index.property"), 20))
  )

  /**
   * Analyse the risk related to ratios of higher and lower risk assets.
   *
   * @param percentages List[BigDecimal] containing the breakdown of asset class percentages
   *                    Must be sorted in Alphabetical Order.
   * @return            Int containing the associated risk on a scale of 1 to 5, with 5 being the highest risk.
   */
  def analyseFundRisk(percentages:List[(String,BigDecimal)]): Int = {
    /** Create a running total to factor the risk score into */
    var riskScore:Int = 0
    var fundRisk = 0
    /** Multiply each percentage with the asset classes associated risk factor */
    percentages.foreach(riskPair => {
      riskScore += assetClasses.get(riskPair._1).getOrElse(0)*riskPair._2.toInt
    })

    /** Find the risk associated with the score retrieved and return it */
    breakable {
      assetRiskScale.foreach(riskPair => {
        if (riskScore <= riskPair._2) {
          fundRisk=riskPair._1
          break
        }
      })
    }
    fundRisk
  }

  /**
   * Analyse the risk appetite of the investor
   * Takes the score from the risk appetite form and compares against the scoring set in the config file
   *
   * @param riskScore Int the integer score retrieved from the risk questionnaire
   * @return          Int containing the associated risk on a scale of 1 to 5, with 5 being the highest risk.
   */
  def analyseRiskAppetite(riskScore:Int): Int = {
    /** Create a temporary risk appetite to update */
    var riskAppetite = 5
    /** Find the risk associated with the score retrieved and return it */
    breakable {
      riskAppetiteScale.foreach(riskPair => {
        if (riskScore <= riskPair._2) {
          riskAppetite=riskPair._1
          break
        }
      })
    }
    riskAppetite
  }

  /**
   * Gets the list of percentage to asset class breakdowns for the Investors risk appetite
   *
   * @param riskAppetite  Int value of the Investors risk appetite
   * @return              List[Double] representation of the asset class breakdown for the risk
   */
  def getTargetFundForRiskAppetite(riskAppetite:Int): List[Double] = {
    /** Retrieve the target fund percentages from the target funds map */
    targetFunds.get(riskAppetite).get.sortBy(_._1).map(_._2).toList
  }

}
