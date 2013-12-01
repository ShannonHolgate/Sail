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

case class Investment(
                      id: ObjectId = new ObjectId,
                      quantity: Option[Int] = None,
                      value: Option[BigDecimal] = None,
                      assetclass: String,
                      name: String,
                      symbol: Option[String] = None,
                      user: ObjectId,
                      added: Date = new Date(),
                      updated: Option[Date] = None,
                      deleted: Option[Date] = None
                      )

object Investment extends ModelCompanion[Investment, ObjectId] {

  val dao = new SalatDAO[Investment, ObjectId](collection = mongoCollection("investments")) {}

  def getInvestmentForUser(user:User) : List[Investment] = {
    dao.find(MongoDBObject("user" -> user.id)).toList
  }

  def getInvestmentForAssetClass(user:User, assetClass:String) : List[Investment] = {
    dao.find(MongoDBObject("user" -> user.id, "assetclass" -> assetClass)).toList
  }

}