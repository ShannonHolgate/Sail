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
import models.TargetFund

/**
 * Tests the Target Fund model, DAO and Integration
 */
@RunWith(classOf[JUnitRunner])
class TargetFundSpec extends Specification with TestUser{
  "TargetFund" should {
    "add a target fund for a user" in new WithApplication(currentApplication){
      /** Remove any current risk target fund*/
      removeTargetFund

      val error = TargetFund.addTargetFundForUser(testUser.id,List[Double](15,5,10,10,40,20))

      error must beNull
    }

    "get the target fund for a user" in new WithApplication(currentApplication){
      /** Ensure the target fund exists */
      removeTargetFund
      addTargetFund

      val target = TargetFund.getTargetFundForUser(testUser.id)

      target.isDefined must beTrue
      target.get must equalTo(List[Double](15,5,10,10,40,20))
    }
  }
}
