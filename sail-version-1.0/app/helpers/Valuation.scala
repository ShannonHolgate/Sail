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
import scala.math.BigDecimal.RoundingMode
import java.net.URLEncoder
import java.util.Date
import java.text.SimpleDateFormat
import play.api.libs.functional.syntax._

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
 * Quotes received from YQL to get historical stock prices. Prices are in USD and need to use the exchange rate to
 * convert
 * Should be parsed straight from the result of the query if a quote is returned.
 *
 * @param Symbol    String the ticker symbol of the history returned
 * @param Date      Date the date of the quote
 * @param Close     BigDecimal the value at market close, this is the value to be used
 */
case class YqlHistoricalQuote(
                                Symbol: String,
                                Date: String,
                                Close: BigDecimal
                               )

/**
 * InvestmentAtDate class created by calculating the quote value based on quantity and the respective
 * YqlHistoricalQuote. This is the final automated investment object returned byt the valuation trait for historical
 * quotes
 *
 * @param yqlQuote  YqlHistoricalQuote the simple historical quote
 * @param value     BigDecimal the value of the investment based on quantity and price
 * @param quantity  Int the quantity of the investment
 */
case class InvestmentAtDate(
                              yqlQuote: YqlHistoricalQuote,
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
  def getSymbolValuesWithQuantity(symbols:List[String], quantities:List[(String,Int)]):
  Option[List[InvestmentWithValue]] = {

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
          val formattedInvestment = new FormattedInvestment(
            gFinance.t,BigDecimal(gFinance.l.replace(",","")),gFinance.e)
          val investmentWithValue = new InvestmentWithValue(
            formattedInvestment,formattedInvestment.closingPrice.*(quantity.get._2).
              setScale(2, RoundingMode.CEILING),quantity.get._2)
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
      googleFinanceURL+= URLEncoder.encode(symbol, "UTF-8") + ","
    }

    /** Make a synchronous call to the Google finance URL and retrieve the Json String of results */
    val response = WS.url(googleFinanceURL).get()
    val result = Await.result(response, 10.seconds)

    /** Check if the symbol exists */
    if (result.body.isEmpty) return None

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
        val gFinance = new GoogleFinance(symbol.as[String],
          BigDecimal(gLastPrices(index).as[String].replace(",","")).
            *(exchangeRate.get.rate).toString(),gExchanges(index).as[String])
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
   * Helper method to retrieve the real time USD to GBP echange rate from the Open Exchange Rate API
   *
   * @return  Option[ExchangeRate] the exhange rate object returned
   */
  def getUSDtoGBP: Option[ExchangeRate] = {
    /** Make a synchronous call to the open exchange rate api and retrieve a json string of results */
    val response = WS.url("http://openexchangerates.org/api/latest.json?app_id=006d0137e6b24aa6ace57f4afdb4fdaf").get()
    val result = Await.result(response, 10.seconds)

    /** Ensure an exchange rate is found */
    if (result.status.equals(200)) {
      /** Parse the JSON to the Exchange rate object */
      val exchangeRateValue = (result.json \ "rates" \ "GBP").asOpt[BigDecimal]
      if (exchangeRateValue.isDefined) {
        val exchangeRate: ExchangeRate = ExchangeRate.apply(exchangeRateValue.get)

        /** Return the exchange rate */
        Some(exchangeRate)
      }
      else
      /** Parsing failed */
        None
    }
    else
    /** Exchange rate failed */
      None
  }

  /**
   * Helper method to retrieve the USD to GBP exchange rate at a certain date from
   * the Open Exchange Rate API
   *
   * @param date  Date the date to find the exchange rate for
   * @return      Option[ExchangeRate]  The exchange rate object returned
   */
  def getUSDtoGBPatDate(date: Date): Option[ExchangeRate] = {
    /** Format the date into a string */
    val stringDate = new SimpleDateFormat("yyyy-MM-dd").format(date)

    /** Make a synchronous call to the open exchange rate api and retrieve a json string of results */
    val response = WS.url("http://openexchangerates.org/api/historical/"+stringDate+
      ".json?app_id=006d0137e6b24aa6ace57f4afdb4fdaf").get()
    val result = Await.result(response, 10.seconds)

    /** Ensure an exchange rate is found */
    if (result.status.equals(200)) {
      /** Parse the JSON to the Exchange rate object */
      val exchangeRateValue = (result.json \ "rates" \ "GBP").asOpt[BigDecimal]
      if (exchangeRateValue.isDefined) {
        val exchangeRate: ExchangeRate = ExchangeRate.apply(exchangeRateValue.get)

        /** Return the exchange rate */
        Some(exchangeRate)
      }
      else
        /** Parsing failed */
        None
    }
    else
      /** Exchange rate failed */
      None
  }

  /**
   * Helper method to retrieve a Json String array of ticker symbols and descriptions from Yahoo Finance
   *
   * @param query String the ticker symbol or name to query Yahoo for
   * @return      Option[String] the Json String of results
   */
  def findTickerSymbols(query:String): Option[String] = {
    /** Make a synchronous call to the yahoo finance and retrieve a json string of investment results */
    val response = WS.url("http://d.yimg.com/autoc.finance.yahoo.com/autoc?query=" + query +
      "&callback=YAHOO.Finance.SymbolSuggest.ssCallback").get();
    val result = Await.result(response, 10.seconds)
    val cleanResult = result.body.split("\"Result\":").last.dropRight(3)
    /** Return the un-formatted Json String */
    Some(cleanResult)
  }

  /**
   *  Helper method to retreive a list of symbols and their closing price at a certain date
   *  Makes use of the exchange rate at a current date and YQL to make up a GBP based stock quote
   *
   * @param symbols List[String] the list of symbols to find a quote for
   * @param date    Date the date to find the quote for
   * @return        Seq[YqlHistoricalQuote] GBP quote of the symbol on the date
   */
  def getSymbolValuesAtDate(symbols:List[String], date:Date): Option[Seq[YqlHistoricalQuote]] = {
    /** Build up the URL to access and retrieve Json from */
    val yahooQueryUrl = "http://query.yahooapis.com/v1/public/yql"

    /** Build up the symbols for the query */
    var symbolString:String = "("
    for (symbol <- symbols) {
      symbolString+= "\"" + symbol + "\","
    }
    symbolString = symbolString.dropRight(1) + ")"

    /** Format the dates */
    val dateFormat = new SimpleDateFormat("yyyy-MM-dd")
    val simpleDate = dateFormat.format(date)

    /** Concatenate the YQL query string with dates and symbols */
    val queryString = "select * from yahoo.finance.historicaldata where symbol in " + symbolString +
      " and startDate = \"" +simpleDate+ "\" and endDate = \"" +simpleDate+"\"".replaceAll("//%5E","%5F")

    /** Make a synchronous call to the Google finance URL and retrieve the Json String of results */
    val response = WS.url(yahooQueryUrl).withQueryString("q" -> queryString, "env" ->
      "http://datatables.org/alltables.env", "format" -> "json").get()
    val result = Await.result(response, 10.seconds)

    if (result.status.equals(200)) {
      /** Get the exchange rate to convert on the returned quote */
      val exchangeRate = getUSDtoGBPatDate(date)

      implicit val historicalQuoteReads = (
        (__ \ "Symbol").format[String] and
          (__ \ "Date").format[String] and
          (__ \ "Close").format[BigDecimal]
        )(YqlHistoricalQuote.apply, unlift(YqlHistoricalQuote.unapply))

      var historicalQuotes = (result.json \ "query" \ "results" \ "quote" ).validate[List[YqlHistoricalQuote]].asOpt
      if (historicalQuotes.isEmpty) {
        val singleQuoteAttempt = (result.json \ "query" \ "results" \ "quote" ).validate[YqlHistoricalQuote].asOpt
        if (singleQuoteAttempt.isDefined) {
          historicalQuotes = Some(List[YqlHistoricalQuote](singleQuoteAttempt.get))
        }
      }
        //Json.fromJson[List[YqlHistoricalQuote]](result.json \ "query" \ "results" \ "quote").

      if (historicalQuotes.isDefined && exchangeRate.isDefined) {
        /** Convert the values to GBP */
        val finalHistoricalQuotes = historicalQuotes.get.map(quote => quote.copy(Close = quote.Close.
          *(exchangeRate.get.rate).setScale(2, RoundingMode.CEILING)))

        /** Return the list of GBP Formatted historical quotes */
        Some(finalHistoricalQuotes)
      }
      else
        /** Either the exchange rate failed or the historical quotes could not be mapped */
        None
    }
    else
      /** The quote request failed */
      None
  }

  /**
   * Gets a list of fully formatted stock quotes at a given date for the quantity provided
   *
   * @param symbols     List[String] the list of ticker Symbols to retrieve quotes for
   * @param quantities  List[(String,Int)] the list of symbol-quantity pairs used to apply the quantity to the quote
   * @param date        Date the simple date to get the quotes for
   * @return
   */
  def getSymbolValuesAtDateWithQuantity(symbols:List[String], quantities:List[(String,Int)], date:Date):
  Option[List[InvestmentAtDate]] = {
    /** Use the helper method to retrieve a simple representation of the real time investment from the YQL
      * API
      */
    val yqlQuotes = getSymbolValuesAtDate(symbols,date)

    /** If quotes are returned, parse them into formatted investments to be read in a more meaningful way */
    if (yqlQuotes.isDefined && yqlQuotes.size.>(0)) {
      val investmentsAtDates = new ListBuffer[InvestmentAtDate]()
      for (quote <- yqlQuotes.get) {
        val quantity = quantities.find{item => item._1.equals(quote.Symbol)}
        if (quantity.isDefined) {
          val investmentAtDate = new InvestmentAtDate(quote,quote.Close.*(quantity.get._2).
            setScale(2, RoundingMode.CEILING),quantity.get._2)
          investmentsAtDates.append(investmentAtDate)
        }
      }

      /** Fully Formatted investments have been created, return them */
      if (investmentsAtDates.size.>(0)) return Some(investmentsAtDates.toList)
      else None
    }
    /** No real quotes have been found */
    else None
  }

}
