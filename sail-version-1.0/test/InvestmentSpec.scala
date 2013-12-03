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

@RunWith(classOf[JUnitRunner])
class InvestmentSpec extends Specification with TestUser{
  "Investment" should {
    "get investments for a user" in new WithApplication(currentApplication){
      confirmTestUserExists
      val user = User.findByEmail(testUser.email)
      val investments = Investment.getInvestmentForUser(user.get)

      investments.size must be_>(0)
      investments(0).user.equals(user.get.id) must beTrue
    }

    "get investments for bank accounts" in new WithApplication(currentApplication){
      confirmTestUserExists
      val user = User.findByEmail(testUser.email)
      val investments = Investment.getInvestmentForAssetClass(user.get,"Bank Account")

      investments.size must be_>(0)
      investments(0).user.equals(user.get.id) must beTrue
    }
  }

}