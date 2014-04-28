/*
 * Copyright (c) 2014. Shannon Holgate.
 *
 * Sail - A personal fund management web application.
 *
 * Makes use of the Play web framework for scala, MongoDB and the salat-Play plugin
 */

package Models

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.mutable.Specification
import test_data.TestUser
import play.api.test.WithApplication
import models.RiskAppetite

/**
 * Tests the Risk Appetite model, DAO and Integration
 */
@RunWith(classOf[JUnitRunner])
class RiskAppetiteSpec extends Specification with TestUser{
  "RiskAppetite" should {
    "add a risk appetite for a user" in new WithApplication(currentApplication){
      /** Remove any current risk appetites */
      removeRiskAppetite

      val error = RiskAppetite.addRiskAppetiteForUser(testUser.id,List[String](
        "3", "3", "1", "1", "2", "2", "3","3", "1", "1", "1", "1"),4)

      error must beNull
    }

    "get the risk appetite for a user" in new WithApplication(currentApplication){
      /** Ensure the risk appetite exists */
      removeRiskAppetite
      addRiskAppetite

      val appetite = RiskAppetite.getRiskAppetiteForUser(testUser.id)

      appetite.isDefined must beTrue
      appetite.get must equalTo(4)
    }
  }
}
