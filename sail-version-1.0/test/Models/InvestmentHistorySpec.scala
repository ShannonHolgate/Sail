package Models

/*
 * Copyright (c) 2013. Shannon Holgate.
 *
 * Sail - A personal fund management web application.
 *
 * Makes use of the Play web framework for scala, MongoDB and the salat-Play plugin
 */

import java.util
import java.util.{GregorianCalendar, Date}
import models.{Investment, User, InvestmentHistory}
import org.bson.types.ObjectId
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import play.api.test.WithApplication
import play.Logger
import test_data.TestUser

/**
 * Tests the InvestmentHistory model
 */
@RunWith(classOf[JUnitRunner])
class InvestmentHistorySpec extends Specification with TestUser{
  "InvestmentHistory" should {
    "create a time series from Investment Histories" in new WithApplication(currentApplication){
      val sevenDay = new Date(System.currentTimeMillis - 7L * 24 * 3600 * 1000)
      val threeDay = new Date(System.currentTimeMillis - 3L * 24 * 3600 * 1000)
      val objectid1 = new ObjectId()
      val objectid2 = new ObjectId()

      val history = new InvestmentHistory(new ObjectId,None,BigDecimal(100.00), BigDecimal(100.00),sevenDay, objectid1 , Some(objectid1), new Date(), None, None)
      val history1 = new InvestmentHistory(new ObjectId,None,BigDecimal(200.00), BigDecimal(300.00),threeDay, objectid1, Some(objectid1), new Date(), None, None)
      val history2 = new InvestmentHistory(new ObjectId,None,BigDecimal(600.00), BigDecimal(-40.00),new Date(), objectid1,Some(objectid1), new Date(), None, None)
      val history3 = new InvestmentHistory(new ObjectId,None,BigDecimal(400.00), BigDecimal(220.00),sevenDay, objectid2,Some(objectid2), new Date(), None, None)
      val history4 = new InvestmentHistory(new ObjectId,None,BigDecimal(1200.00), BigDecimal(2100.00),new Date(), objectid2, Some(objectid2), new Date(), None, None)
      val histories = List[InvestmentHistory](history,history1,history2,history3,history4)

      val timeSeries = InvestmentHistory.getTimeSeriesForInvestmentHistories(histories)

      timeSeries.isEmpty must beFalse
    }

    "get histories for a list of bank accounts" in new WithApplication(currentApplication){
      removeTestUsers
      confirmTestUserExists
      val user = User.findByEmail(testUser.email)
      val investments = Investment.getInvestmentForAssetClass(user.get.id,"Bank Accounts")

      val histories = InvestmentHistory.getHistoryForInvestments(investments.getOrElse(List[Investment]()))

      histories.isDefined must beTrue
      histories.get.size must be_>(0)

      for(history <- histories.get.sortBy(hist => (hist.date))) {
        //Logger.info("B/A histories: " + history.date + " " + history.valuechanged)
      }
    }

    "get histories for all investments" in new WithApplication(currentApplication){
      removeTestUsers
      confirmTestUserExists
      val user = User.findByEmail(testUser.email)
      val investments = Investment.getInvestmentForUser(user.get.id)

      val histories = InvestmentHistory.getHistoryForInvestments(investments.getOrElse(List[Investment]()))

      histories.isDefined must beTrue
      histories.get.size must be_>(0)

      for(history <- histories.get.sortBy(hist => (hist.date))) {
        //Logger.info("All histories: " + history.date + " " +history.valuechanged)
      }
    }
  }

}
