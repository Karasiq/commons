name := "commons"

version := "1.0"

organization := "com.karasiq"

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  "commons-io" % "commons-io" % "2.4",
  "org.apache.httpcomponents" % "httpclient" % "4.3.3",
  "net.sourceforge.htmlunit" % "htmlunit" % "2.15" % "provided",
  "joda-time" % "joda-time" % "2.4",
  "org.joda" % "joda-convert" % "1.7",
  "com.typesafe" % "config" % "1.2.1",
  "org.scalatest" %% "scalatest" % "2.2.1" % "test"
)

scalacOptions ++= Seq("-feature", "-optimize", "-deprecation", "-Yinline-warnings")
