package Models

/*
 * Copyright (c) 2013. Shannon Holgate.
 *
 * Sail - A personal fund management web application.
 *
 * Makes use of the Play web framework for scala, MongoDB and the salat-Play plugin
 */

import models.{Investment, User}
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import play.api.test.WithApplication
import test_data.TestUser

/**
 * Tests the Investment model
 */
@RunWith(classOf[JUnitRunner])
class InvestmentSpec extends Specification with TestUser{
  "Investment" should {
    "get investments for a user" in new WithApplication(currentApplication){
      removeTestUsers
      confirmTestUserExists
      val user = User.findByEmail(testUser.email)
      val investments = Investment.getInvestmentForUser(user.get.id)

      investments.isDefined must beTrue
      investments.get.size must be_>(0)
      investments.get(0).user.equals(user.get.id) must beTrue
    }

    "get investments for bank accounts" in new WithApplication(currentApplication){
      removeTestUsers
      confirmTestUserExists
      val user = User.findByEmail(testUser.email)
      val investments = Investment.getInvestmentForAssetClass(user.get.id,"Bank Accounts")

      investments.isDefined must beTrue
      investments.get.size must be_>(0)
      investments.get(0).user.equals(user.get.id) must beTrue
    }

    "get investments with Symbols" in new WithApplication(currentApplication){
      removeTestUsers
      confirmTestUserExists
      val user = User.findByEmail(testUser.email)
      val investments = Investment.getInvestmentsWithSymbols(user.get.id)

      investments.isDefined must beTrue
      investments.get.size must be_>(0)
      investments.get(0).user.equals(user.get.id) must beTrue
    }
  }

}