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
    "se.radley" %% "play-plugins-salat" % "1.3.0",
    /** Add the emailer plugin*/
    "com.typesafe" %% "play-plugins-mailer" % "2.2.0"
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
    /** Add the new dependencies to support the salat-Play plugin*/
    routesImport += "se.radley.plugin.salat.Binders._",
    templatesImport += "org.bson.types.ObjectId"

  )

}