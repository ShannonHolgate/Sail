/*
 * Copyright (c) 2013. Shannon Holgate.
 *
 * Sail - A personal fund management web application.
 *
 * Makes use of the Play web framework for scala, MongoDB and the salat-Play plugin
 */

import sbt._
import play.Project._
import net.litola.SassPlugin

object ApplicationBuild extends Build {

  val appName         = "sail-version-10"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    /** Add the salat-Play plugin*/
    "org.mongodb" %% "casbah" % "2.8.2",
    "com.novus" %% "salat" % "1.9.9",
    /** Add the emailer plugin*/
    "com.typesafe" %% "play-plugins-mailer" % "2.2.0",
    /** Add the BCrypt encryption plugin*/
    "org.mindrot" % "jbcrypt" % "0.3m"
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
    /** Add the new dependencies to support the salat-Play plugin*/
    routesImport += "plugins.salat.Binders._",
    routesImport += "extensions.Binders._",
    templatesImport += "org.bson.types.ObjectId"
  )

}
