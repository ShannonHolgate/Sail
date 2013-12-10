/*
 * Copyright (c) 2013. Shannon Holgate.
 *
 * Sail - A personal fund management web application.
 *
 * Makes use of the Play web framework for scala, MongoDB and the salat-Play plugin
 */

package helpers

/**
 * Risk Helper class to analyse the risk based on a percentage breakdown of assets
 */
trait Risker {

  /**
   * Analyse the risk related to ratios of higher and lower risk assets.
   *
   * @param percentages List[BigDecimal] containing the breakdown of asset class percentages
   *                    Must be sorted in Alphabetical Order.
   * @return            Int containing the associated risk on a scale of 1 to 5, with 5 being the lowest risk.
   */
  def analyseRiskAppetite(percentages:List[BigDecimal]): Int = {
    // TODO Implement risk analysis against percentages passed in
    2
  }

}
