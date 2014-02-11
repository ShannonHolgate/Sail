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
import play.Logger

/**
 * Investment class to be mapped from MongoDB using the Salat library
 * Holds details on a single investment which the User holds
 * Parent Object in heirarchy before the user
 *
 * @param id          ObjectId generated on creation
 * @param quantity    Option[Int] quantity of investments if the investment is automated
 * @param value       BigDecimal the current value of the Investment
 * @param assetclass  String the Asset Class of the Investment
 * @param name        String the name of the Investment
 * @param symbol      Option[String] the ticker symbol of the investment if it is automated
 * @param user        ObjectId the user who owns the investment
 * @param added       Option[Date] the Investment was created, needed by MongoDB
 * @param updated     Option[Date] the Investment was updated, needed by MongoDB
 * @param deleted     Option[Date] the Investment was deleted
 */
case class Investment(
                      id: ObjectId = new ObjectId,
                      quantity: Option[Int] = None,
                      value: BigDecimal,
                      assetclass: String,
                      name: String,
                      symbol: Option[String] = None,
                      user: ObjectId,
                      added: Date = new Date(),
                      updated: Option[Date] = None,
                      deleted: Option[Date] = None
                      )

/**
 * Object to hold the Investment functionality implementing getters and setters
 * extends the ModelCompanion trait from Salat
 */
object Investment extends ModelCompanion[Investment, ObjectId] {

  /** Salat Data Access Object to hook into the user collection on the MongoDB */
  val dao = new SalatDAO[Investment, ObjectId](collection = mongoCollection("investments")) {}

  /**
   * Gets a list of Investments for the given user Id
   *
   * @param user  ObjectId of the user
   * @return      Option[List[Investment] ] A list of investments related to the user, if any
   */
  def getInvestmentForUser(user:ObjectId) : Option[List[Investment]] = {

    /** Get a list of Investments from the database */
    val investments = dao.find(MongoDBObject("user" -> user))

    /** Return the list of Investments */
    if (!investments.isEmpty) Some(investments.toList)
    else None
  }

  /**
   * Gets a list of investments for the given user Id and asset class
   *
   * @param user        ObjectId of the user
   * @param assetClass  String the asset class to get the investments for
   * @return            Option[List[Investment] ] A list of investments related to the asset class and user, if any
   */
  def getInvestmentForAssetClass(user:ObjectId, assetClass:String) : Option[List[Investment]] = {

    /** Get a list of investments from the database */
    val investments = dao.find(MongoDBObject("user" -> user, "assetclass" -> assetClass))

    /** Return the list of investments */
    if (!investments.isEmpty) Some(investments.toList)
    else None
  }

  /**
   * Gets a list of automated investments with symbols for the given user Id
   *
   * @param user  ObjectId of the user
   * @return      Option[List[Investment] ] A list of automated investments with Symbols for the user, if any
   */
  def getInvestmentsWithSymbols(user:ObjectId) : Option[List[Investment]] = {

    /** Get a list of investments from the database */
    val investments = dao.find(MongoDBObject("user" -> user, "symbol" -> MongoDBObject("$exists" -> true)))

    /** Return the list of investments */
    if (!investments.isEmpty) Some(investments.toList)
    else None
  }

  /**
   * Get a single investment for the given Investment Id
   *
   * @param investmentId  ObjectId of the investment to be retrieved
   * @return              Option[Investment] the investment relating to the investment Id
   */
  def getOne(investmentId:ObjectId) : Option[Investment] = {

    /** Get the investment from the database */
    val investment = dao.findOne(MongoDBObject("_id" -> investmentId))

    /** Return the investment */
    if (investment.isDefined) Some(investment.get)
    else None
  }

  /**
   * Get a single investment for a given user and investment name
   *
   * @param name  String the name of the investment
   * @param user  User the user who owns the investment
   * @return      Option[Investment]  the investment relating to the name and user
   */
  def getOneFromName(name: String, user: User) : Option[Investment] = {
    /** Get the Investment from the database */
    val investment = dao.findOne((MongoDBObject("name" -> name, "user" -> user.id)))

    /** Return the investment */
    if (investment.isDefined) Some(investment.get)
    else None
  }

  /**
   * Get a single investment for a given user and investment symbol
   *
   * @param symbol  String the symbol of the investment
   * @param user    User the user who owns the investment
   * @return        Option[Investment]  the investment relating to the symbol and user
   */
  def getOneFromSymbol(symbol: String, user: User) : Option[Investment] = {
    /** Get the Investment from the database */
    val investment = dao.findOne((MongoDBObject("symbol" -> symbol, "user" -> user.id)))

    /** Return the investment */
    if (investment.isDefined) Some(investment.get)
    else None
  }

  /**
   * Update the Investment in the database when the value has been updated
   *
   * @param investment  Investment the Investment to be updated in the database
   * @return            Boolean whether the update was a success or not
   */
  def updateInvestmentValue(investment:Investment) : Boolean = {

    /** Update the Investment for the Investment id in the database */
    dao.update(MongoDBObject("_id" -> investment.id),investment,false, false, new WriteConcern)

    /** Check if the update was successful */
    val newInvestment = getOne(investment.id)
    if (newInvestment.isDefined) {

      /** Ensure the new Investment contains the new value and return*/
      newInvestment.get.value == investment.value
    }

    /** the new Investment does not exist */
    else false
  }

  /**
   * Create and insert a new investment into the database
   *
   * @param quantity    Optional quantity of automated investments
   * @param value       Current value of the investment
   * @param assetClass  The asset class to insert the investment into
   * @param name        The name of the investment
   * @param symbol      Optional ticker symbol of the investment
   * @param user        The user who owns the investment
   * @return            Boolean whether the insert was successful
   */
  def createOne(quantity:Option[Int],value:BigDecimal, assetClass:String, name:String,symbol:Option[String],user:User) : Option[ObjectId] = {

    /** Createt the investment object */
    val investment = Investment(quantity=quantity,value=value,assetclass=assetClass,name=name,symbol=symbol,user=user.id)

    /** Insert the investment into the database and ensure the returned id exists */
    dao.insert(investment)
  }

  /**
   * Remove a single investment for the given Investment Id
   *
   * @param investmentId  ObjectId of the investment to be removed
   * @return              Boolean whether the deletion was a success or not
   */
  def removeOne(investmentId:ObjectId) : Boolean = {

    /** Get the investment from the database */
    val investment = dao.findOne(MongoDBObject("_id" -> investmentId))

    if (investment.isDefined) {
      /** Remove the investment from the database */
      dao.remove(investment.get)

      /** Check if the deletion succeeded */
      if (dao.findOne(MongoDBObject("_id" -> investmentId)).isEmpty) true
      /** the removal failed */
      else false
    }
     /** Cannot find the investment */
    else false
  }
}