package Helpers

/*
 * Copyright (c) 2013. Shannon Holgate.
 *
 * Sail - A personal fund management web application.
 *
 * Makes use of the Play web framework for scala, MongoDB and the salat-Play plugin
 */

import helpers.Valuation
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import play.api.test.WithApplication
import test_data.TestUser
import org.joda.time.DateTime
import play.i18n.Messages

/**
 * Tests the Helper traits
 */
@RunWith(classOf[JUnitRunner])
class HelperSpec extends Specification with TestUser with Valuation{

  /**
   * Test the Valuation Helper trait
   */
  "Valuation" should {
    "get current investment values for symbols" in new WithApplication(currentApplication){
      val symbolList = getSymbolValues(List[String]("GOOG","AAPL"))
      symbolList.isDefined must beTrue
      symbolList.get.size must be_==(2)
      symbolList.get(0).symbol must be_==("GOOG")
    }

    "get current investment values for symbols with quantities" in new WithApplication(currentApplication){
      val symbolList = getSymbolValuesWithQuantity(List[String]("GOOG","AAPL"),List[(String,Int)](("GOOG",20),
        ("AAPL",50)))
      symbolList.isDefined must beTrue
      symbolList.get.size must be_==(2)
      symbolList.get(0).quantity must be_==(20)
      symbolList.get(1).quantity must be_==(50)
      symbolList.get(0).value.>(0) must beTrue
    }

    "get current investment values for invalid symbols with quantities" in new WithApplication(currentApplication){
      val symbolList = getSymbolValuesWithQuantity(List[String]("invalid","AAPL"),List[(String,Int)](("GOOG",20),
        ("invalid",50)))
      symbolList.isDefined must beFalse
    }

    "find ticker symbols for google" in new WithApplication(currentApplication){
      val symbolList = findTickerSymbols("google")
      symbolList.isDefined must beTrue
      symbolList.get must contain("GOOG")
    }

    "get Symbol values at a date" in new WithApplication(currentApplication){
      val symbolList = getSymbolValuesAtDate(List[String]("AAPL"),DateTime.parse("2014-02-20").toDate)
      symbolList.isDefined must beTrue
      symbolList.get(0).Symbol must contain("AAPL")
    }

    "get Symbol values at a date with quantity" in new WithApplication(currentApplication){
      val symbolList = getSymbolValuesAtDateWithQuantity(List[String]("AAPL"),List[(String,Int)](("AAPL",5)),
        DateTime.parse("2014-02-20").toDate)
      symbolList.isDefined must beTrue
      symbolList.get(0).quantity must beEqualTo(5)
    }
  }
}
