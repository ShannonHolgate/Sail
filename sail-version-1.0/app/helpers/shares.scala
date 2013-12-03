package helpers

import play.api.libs.ws._
import scala.concurrent.Future
import play.api.libs.ws.WS.WSRequestHolder
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class Share(
                 tickerSymbol:String,
                 exchange:String,
                 price: BigDecimal,
                 change: BigDecimal
)

trait Shares {

/**  def getSharesForSymbols(symbols: List[String]): List[Share] = {

    implicit val context = scala.concurrent.ExecutionContext.Implicits.global

    var googleFinanceURL = "http://finance.google.com/finance/info?client=ig&q="
    for (symbol <- symbols) {
      googleFinanceURL+= symbol + ","
    }

    val requestHolder : WSRequestHolder = WS.url(googleFinanceURL)
    val futureResponse : Future[String] = requestHolder.get().map{
      response => (response.json \ "person" \ "name").as[String]
    }


  }
**/
}
