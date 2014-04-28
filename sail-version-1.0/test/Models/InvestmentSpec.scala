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
import org.bson.types.ObjectId

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
      val investments = Investment.getInvestmentsWithSymbols(testUser.id)

      investments.isDefined must beTrue
      investments.get.size must be_>(0)
      investments.get(0).user.equals(testUser.id) must beTrue
    }

    "get an investment from it's ID" in new WithApplication(currentApplication){
      val investment = Investment.getOne(new ObjectId("5283e32d03649c70127432d7"))

      investment.isDefined must beTrue
    }

    "get an investment from name" in new WithApplication(currentApplication){
      removeTestUsers
      confirmTestUserExists
      val user = User.findByEmail(testUser.email)

      val investment = Investment.getOneFromName("Cash",user.get)

      investment.isDefined must beTrue
    }

    "get an investment from it's symbol" in new WithApplication(currentApplication){
      removeTestUsers
      confirmTestUserExists
      val user = User.findByEmail(testUser.email)

      val investment = Investment.getOneFromSymbol("AAPL",user.get)

      investment.isDefined must beTrue
    }

    "update an investments value" in new WithApplication(currentApplication){
      val investment = Investment.getOne(new ObjectId("5283e32d03649c70127432d7"))

      val update = Investment.updateInvestmentValue(investment.get.copy(value=5000))

      update must beTrue
    }

    "create an investment" in new WithApplication(currentApplication){
      removeTestUsers
      confirmTestUserExists
      val user = User.findByEmail(testUser.email)

      val newId = Investment.createOne(None,500,"Property","Test House",None,user.get)

      newId.isDefined must beTrue
    }

    "remove an investment" in new WithApplication(currentApplication){
      removeTestUsers
      confirmTestUserExists
      val user = User.findByEmail(testUser.email)

      val newId = Investment.createOne(None,500,"Property","Test House delete",None,user.get)

      val success = Investment.removeOne(newId.get)

      success must beTrue
    }

    "get the percentage breakdown of the fund" in new WithApplication(currentApplication){
      val breakdown = Investment.getPercentageBreakdown(testUser.id)

      breakdown.isDefined must beTrue
      breakdown.get.length must be_>(0)
    }
  }

}