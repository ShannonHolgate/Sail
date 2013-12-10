/*
 * Copyright (c) 2013. Shannon Holgate.
 *
 * Sail - A personal fund management web application.
 *
 * Makes use of the Play web framework for scala, MongoDB and the salat-Play plugin
 */

package helpers

import play.api.libs.ws._
import scala.concurrent.Await
import scala.concurrent.duration._
import play.api.libs.json._
import scala.collection.mutable.ListBuffer

/**
 * GoogleFinance class to be mapped from the Json returned from the URL
 * Conforms to the GoogleFinance Json format
 *
 * @param t String ticker symbol mapped from Json
 * @param l String last price in GBP mapped from Json
 * @param e String exchange where the symbol is traded, mapped from Json
 */
case class GoogleFinance(
                          t: String,
                          l: String,
                          e: String
                          )

/**
 * ExchangeRate class to be mapped from the Json returned from the open exchange rate API
 *
 * @param rate BigDecimal exchange rate from USD to GBP mapped from Json
 */
case class ExchangeRate(
                        rate: BigDecimal
                        )

/**
 * FormattedInvestment class to create a more meaningful version of the GoogleFinance object
 *
 * @param symbol        String the ticker symbol
 * @param closingPrice  BigDecimal the last price in real time
 * @param exchange      String the exchange the symbol is traded at
 */
case class FormattedInvestment(
                               symbol: String,
                               closingPrice: BigDecimal,
                               exchange: String
                               )

/**
 * InvestmentWithValue class to be created by calculating the realtime investment value.
 * Final automated investment object to be returned by the Valuation trait
 *
 * @param formattedInvestment FormattedInvestment the simple investment holding
 * @param value               BigDecimal the value of the investment, based on quantity and price
 * @param quantity            Int the quantity of the investment
 */
case class InvestmentWithValue(
                                formattedInvestment: FormattedInvestment,
                                value: BigDecimal,
                                quantity: Int
                                )

/**
 * Valuation Helper used to retrieve real time valuations in Json and parse them into meaningful objects
 */
trait Valuation {

  /**
   * Gets a simple meaningful representation of the real time value of a symbol.
   * Does not take into account quantity
   *
   * @param symbols List[String] the symbols to find the value of
   * @return        Option[List[FormattedInvestment] ] A list of simple formatted real time investments
   */
  def getSymbolValues(symbols:List[String]): Option[List[FormattedInvestment]] = {
    /** Use the helper method to retrieve a cryptic representation of the real time investment from the Google Finance
      * API
      */
    val gFSequence = getGFinanceSeq(symbols)

    /** If real time values are returned, parse them into formatted investments to be read in a more meaningful way */
    if (gFSequence.isDefined && gFSequence.size.>(0)) {
      val formattedInvestments = new ListBuffer[FormattedInvestment]()
      for (gFinance <- gFSequence.get) {
        val formattedInvestment = new FormattedInvestment(gFinance.t,BigDecimal(gFinance.l.replace(",","")),gFinance.e)
        formattedInvestments.append(formattedInvestment)
      }

      /** Formatted investments have been created, return them */
      return Some(formattedInvestments.toList)
    }
    /** No real time values have been found */
    else None
  }

  /**
   * Gets a List of fully detailed real time investments and their price related to quantity
   *
   * @param symbols     List[String] a list of symbols to get the real time values for
   * @param quantities  List[(String, Int)] Symbol-Quantity key-value pairs to process
   * @return            Option[List[InvestmentWithValue] ] A full meaningful list of real time investments
   *                                                       with values relating to their quantities
   */
  def getSymbolValuesWithQuantity(symbols:List[String], quantities:List[(String,Int)]): Option[List[InvestmentWithValue]] = {

    /** Use the helper method to retrieve a cryptic representation of the real time investment from the Google Finance
      * API
      */
    val gFSequence = getGFinanceSeq(symbols)

    /** If real time values are returned, parse them into formatted investments to be read in a more meaningful way */
    if (gFSequence.isDefined && gFSequence.size.>(0)) {
      val investmentsWithValues = new ListBuffer[InvestmentWithValue]()
      for (gFinance <- gFSequence.get) {
        val quantity = quantities.find{item => item._1.equals(gFinance.t)}
        if (quantity.isDefined) {
          val formattedInvestment = new FormattedInvestment(gFinance.t,BigDecimal(gFinance.l.replace(",","")),gFinance.e)
          val investmentWithValue = new InvestmentWithValue(formattedInvestment,formattedInvestment.closingPrice.*(quantity.get._2),quantity.get._2)
          investmentsWithValues.append(investmentWithValue)
        }
      }

      /** Fully Formatted investments have been created, return them */
      if (investmentsWithValues.size.>(0)) return Some(investmentsWithValues.toList)
      else None
    }
    /** No real time values have been found */
    else None
  }

  /**
   * Helper method to retrieve and parse google finance real time data into cryptic objects
   *
   * @param symbols List[String] a list of symbols to retrieve the real time values for
   * @return        Option[Seq[GoogleFinance] ] Sequence of cryptic google finance objects
   */
  private def getGFinanceSeq(symbols:List[String]): Option[Seq[GoogleFinance]] = {

    /** Build up the URL to access and retrieve Json from */
    var googleFinanceURL = "http://finance.google.com/finance/info?client=ig&q="
    for (symbol <- symbols) {
      googleFinanceURL+= symbol + ","
    }

    /** Make a synchronous call to the Google finance URL and retrieve the Json String of results */
    val response = WS.url(googleFinanceURL).get()
    val result = Await.result(response, 10.seconds)
    val gFinances = Json.parse(result.body.replace("/",""))

    /** Get the USD to GBP exchange rate to setup the Google Finance Values as GBP */
    val exchangeRate = getUSDtoGBP

    /** If the exchange rate is available parse the Json into Google Finance Objects */
    if (exchangeRate.isDefined) {

      val gSymbols:Seq[JsValue] = (gFinances \\ "t").seq
      val gLastPrices:Seq[JsValue] = (gFinances \\ "l").seq
      val gExchanges:Seq[JsValue] = (gFinances \\ "e").seq

      val gFinanceList = new ListBuffer[GoogleFinance]()

      /** Loop through the last prices and update their values based on the exchange rate
        * Then create a Google Finance object out of each and add it to the gFinanceList to be returned
        */
      for ((symbol,index) <- gSymbols.zipWithIndex) {
        val gFinance = new GoogleFinance(symbol.as[String],BigDecimal(gLastPrices(index).as[String].replace(",","")).*(exchangeRate.get.rate).toString(),gExchanges(index).as[String])
        gFinanceList.append(gFinance)
      }

      /** Return the Google Finance List with GBP values */
      if (gFinanceList.size.>(0)) return Some(gFinanceList)
      else return None
    }
    /** no Exchange rate was found */
    else
      None
  }

  /**
   * Helper method to retrieve the real time USD to GBP echange rate from the Open Exchange rate API
   *
   * @return  Option[ExchangeRate] the echange rate object returned
   */
  def getUSDtoGBP: Option[ExchangeRate] = {
    /** Setup the Json parser to write the Json to */
    implicit val exchangeRateFormat = Json.format[ExchangeRate]

    /** Make a synchronous call to the exchange rate api and retriece a json string of results */
    val response = WS.url("http://rate-exchange.appspot.com/currency?from=USD&to=GBP").get()
    val result = Await.result(response, 10.seconds)
    Json.fromJson[ExchangeRate](Json.parse(result.body)).asOpt
  }

}
