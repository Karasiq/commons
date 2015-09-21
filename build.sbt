name := "commons"

version := "1.0"

isSnapshot := false

organization := "com.github.karasiq"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "commons-io" % "commons-io" % "2.4",
  "org.apache.httpcomponents" % "httpclient" % "4.3.3",
  "net.sourceforge.htmlunit" % "htmlunit" % "2.18" % "provided",
  "joda-time" % "joda-time" % "2.4",
  "org.joda" % "joda-convert" % "1.7",
  "com.typesafe" % "config" % "1.3.0",
  "org.scalatest" %% "scalatest" % "2.2.1" % "test"
)

scalacOptions ++= Seq("-feature", "-optimize", "-deprecation", "-Yinline-warnings")

publishMavenStyle := true

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

publishArtifact in Test := false

pomIncludeRepository := { _ ⇒ false }

licenses := Seq("Apache License, Version 2.0" -> url("http://opensource.org/licenses/Apache-2.0"))

homepage := Some(url("https://github.com/Karasiq/" + name.value))

pomExtra := <scm>
  <url>git@github.com:Karasiq/{name.value}.git</url>
  <connection>scm:git:git@github.com:Karasiq/{name.value}.git</connection>
</scm>
  <developers>
    <developer>
      <id>karasiq</id>
      <name>Piston Karasiq</name>
      <url>https://github.com/Karasiq</url>
    </developer>
  </developers>