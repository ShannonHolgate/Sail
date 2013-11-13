name := "sail-version-10"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache
)     

play.Project.playScalaSettings

net.litola.SassPlugin.sassOptions := Seq("--compass","-r", "compass", "-r", "zurb-foundation")