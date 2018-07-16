import sbt.Keys._

name := "commons"

val commonDeps = Seq(
  "commons-io" % "commons-io" % "2.5",
  "com.typesafe" % "config" % "1.3.1"
)

val testDeps = Seq(
  "org.scalatest" %% "scalatest" % "3.0.4" % "test"
)

val networkDeps = Seq(
  "org.apache.httpcomponents" % "httpclient" % "4.5.3" % "provided",
  "net.sourceforge.htmlunit" % "htmlunit" % "2.27" % "provided",
  "joda-time" % "joda-time" % "2.9.9",
  "org.joda" % "joda-convert" % "1.8.3"
)

val akkaDeps = Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.5.4" % "provided"
)

lazy val commonSettings = Seq(
  version := "1.0.9",
  isSnapshot := version.value.endsWith("SNAPSHOT"),
  organization := "com.github.karasiq",
  scalaVersion := "2.12.3",
  crossScalaVersions := Seq("2.11.11", "2.12.3"),
  libraryDependencies ++= testDeps,
  publishMavenStyle := true,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  publishArtifact in Test := false,
  pomIncludeRepository := { _ ⇒ false },
  licenses := Seq("Apache License, Version 2.0" → url("http://opensource.org/licenses/Apache-2.0")),
  homepage := Some(url("https://github.com/Karasiq/commons")),
  pomExtra := <scm>
    <url>git@github.com:Karasiq/commons.git</url>
    <connection>scm:git:git@github.com:Karasiq/commons.git</connection>
  </scm>
    <developers>
      <developer>
        <id>karasiq</id>
        <name>Piston Karasiq</name>
        <url>https://github.com/Karasiq</url>
      </developer>
    </developers>
)

lazy val rootSettings = Seq(
  name := "commons"
)

lazy val miscSettings = Seq(
  name := "commons-misc"
)

lazy val filesSettings = Seq(
  name := "commons-files",
  libraryDependencies ++= commonDeps
)

lazy val networkSettings = Seq(
  name := "commons-network",
  libraryDependencies ++= commonDeps ++ networkDeps
)

lazy val akkaSettings = Seq(
  name := "commons-akka",
  libraryDependencies ++= commonDeps ++ akkaDeps ++ networkDeps
)

lazy val configsSettings = Seq(
  name := "commons-configs"
)

lazy val misc = Project("commons-misc", file("misc"))
  .settings(commonSettings, miscSettings)

lazy val files = Project("commons-files", file("files"))
  .settings(commonSettings, filesSettings)

lazy val network = Project("commons-network", file("network"))
  .settings(commonSettings, networkSettings)
  .dependsOn(misc, files)

lazy val akka = Project("commons-akka", file("akka"))
  .settings(commonSettings, akkaSettings)
  .dependsOn(network, files)

lazy val configs = (crossProject in file("configs"))
  .settings(commonSettings, configsSettings)
  .jvmSettings(libraryDependencies ++= akkaDeps)
  .jsSettings(libraryDependencies ++= Seq("org.akka-js" %%% "akkajsactor" % "1.2.5.4" % "provided"))

lazy val configsJVM = configs.jvm

lazy val configsJS = configs.js

lazy val `commons` = Project("commons", file("."))
  .settings(commonSettings, rootSettings)
  .dependsOn(misc, files, network)
  .aggregate(misc, files, network, akka, configsJVM, configsJS)