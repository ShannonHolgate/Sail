/*
 * Copyright (c) 2013. Shannon Holgate.
 *
 * Sail - A personal fund management web application.
 *
 * Makes use of the Play web framework for scala, MongoDB and the salat-Play plugin
 */

package models

import java.util.Date
import com.novus.salat.dao._
import com.mongodb.casbah.Imports._
import se.radley.plugin.salat._
import models.MongoContext._
import play.api.Play.current
import com.mongodb.casbah.commons.TypeImports._

case class RecentChange(
                        id: ObjectId = new ObjectId,
                        quantity: Option[Double] = None,
                        value: BigDecimal,
                        valuechanged: BigDecimal,
                        date: Date,
                        accepted: Boolean = false,
                        investment: ObjectId,
                        user: ObjectId,
                        added: Date = new Date(),
                        updated: Option[Date] = None,
                        deleted: Option[Date] = None
                        )

object RecentChange extends ModelCompanion[RecentChange, ObjectId] {

  val dao = new SalatDAO[RecentChange, ObjectId](collection = mongoCollection("recentchanges")) {}

  def getRecentChangesForInvestments(investments:List[Investment], dateFrom:Option[Date], dateTo:Option[Date]) : List[RecentChange] = {
    var histories = List[RecentChange]()
    if (dateFrom.isEmpty || dateTo.isEmpty) {
      for( investment <- investments) {
        histories = histories ++ dao.find(MongoDBObject("recentchanges" -> investment.id)).toList
      }
    }
    else {
      for( investment <- investments) {
        histories = histories ++ dao.find(MongoDBObject("recentchanges" -> investment.id, "date" -> MongoDBObject("$gte" -> dateFrom.get, "$lte" -> dateTo.get))).toList
      }
    }
    histories
  }

  def removeRecentChange(recentChangeId: ObjectId, user: User): Boolean = {
    dao.remove(MongoDBObject("_id" -> recentChangeId, "user" -> user.id))
    dao.findOne(MongoDBObject("_id" -> recentChangeId)).isEmpty
  }

}
