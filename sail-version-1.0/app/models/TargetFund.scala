/*
 * Copyright (c) 2013. Shannon Holgate.
 *
 * Sail - A personal fund management web application.
 *
 * Makes use of the Play web framework for scala, MongoDB and the salat-Play plugin
 */

package models

import play.api.Play.current
import com.novus.salat.dao._
import com.mongodb.casbah.Imports._
import se.radley.plugin.salat._
import models.MongoContext._
import com.mongodb.casbah.commons.TypeImports.ObjectId
import java.util.Date

case class TargetFund(
                       id: ObjectId = new ObjectId,
                       assetClassPercentages: List[BigDecimal],
                       user: ObjectId,
                       added: Date = new Date(),
                       updated: Option[Date] = None,
                       deleted: Option[Date] = None
                       )

object TargetFund extends ModelCompanion[TargetFund, ObjectId] {

  val dao = new SalatDAO[TargetFund, ObjectId](collection = mongoCollection("targetfund")) {}

  def getTargetFundForUser(user:User) : Option[List[BigDecimal]] = {
    val targetFund: Option[TargetFund] = dao.findOne(MongoDBObject("user" -> user.id))
    if (targetFund.isDefined) {
      return  Option(targetFund.get.assetClassPercentages)
    }
    else
      None
  }

}