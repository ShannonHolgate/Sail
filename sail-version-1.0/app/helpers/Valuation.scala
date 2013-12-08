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

case class GoogleFinance(
                          t: String,
                          l: String,
                          e: String
                          )

case class ExchangeRate(
                        rate: BigDecimal
                        )

case class FormattedInvestment(
                               symbol: String,
                               closingPrice: BigDecimal,
                               exchange: String
                               )

case class InvestmentWithValue(
                                formattedInvestment: FormattedInvestment,
                                value: BigDecimal,
                                quantity: Int
                                )

trait Valuation {

  def getSymbolValues(symbols:List[String]): Option[List[FormattedInvestment]] = {
    val gFSequence = getGFinanceSeq(symbols)
    if (gFSequence.isDefined && gFSequence.size.>(0)) {
      val formattedInvestments = new ListBuffer[FormattedInvestment]()
      for (gFinance <- gFSequence.get) {
        val formattedInvestment = new FormattedInvestment(gFinance.t,BigDecimal(gFinance.l.replace(",","")),gFinance.e)
        formattedInvestments.append(formattedInvestment)
      }
      return Some(formattedInvestments.toList)
    }
    else None
  }

  def getSymbolValuesWithQuantity(symbols:List[String], quantities:List[(String,Int)]): Option[List[InvestmentWithValue]] = {
    val gFSequence = getGFinanceSeq(symbols)
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
      if (investmentsWithValues.size.>(0)) return Some(investmentsWithValues.toList)
      else None
    }
    else None
  }

  private def getGFinanceSeq(symbols:List[String]): Option[Seq[GoogleFinance]] = {

    var googleFinanceURL = "http://finance.google.com/finance/info?client=ig&q="
    for (symbol <- symbols) {
      googleFinanceURL+= symbol + ","
    }
    val response = WS.url(googleFinanceURL).get()
    val result = Await.result(response, 10.seconds)
    val gFinances = Json.parse(result.body.replace("/",""))
    val exchangeRate = getUSDtoGBP


    if (exchangeRate.isDefined) {

      val gSymbols:Seq[JsValue] = (gFinances \\ "t").seq
      val gLastPrices:Seq[JsValue] = (gFinances \\ "l").seq
      val gExchanges:Seq[JsValue] = (gFinances \\ "e").seq

      val gFinanceList = new ListBuffer[GoogleFinance]()

      for ((symbol,index) <- gSymbols.zipWithIndex) {
        val gFinance = new GoogleFinance(symbol.as[String],BigDecimal(gLastPrices(index).as[String].replace(",","")).*(exchangeRate.get.rate).toString(),gExchanges(index).as[String])
        gFinanceList.append(gFinance)
      }
      if (gFinanceList.size.>(0)) return Some(gFinanceList)
      else return None
    }
    else
      None
  }

  def getUSDtoGBP: Option[ExchangeRate] = {
    implicit val exchangeRateFormat = Json.format[ExchangeRate]
    val response = WS.url("http://rate-exchange.appspot.com/currency?from=USD&to=GBP").get()
    val result = Await.result(response, 10.seconds)
    Json.fromJson[ExchangeRate](Json.parse(result.body)).asOpt
  }

}
