# commons [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.karasiq/commons_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.karasiq/commons_2.12)
Mixed Scala utility classes

# How to use
Add to your build.sbt:
```scala
libraryDependencies += "com.github.karasiq" %% "commons" % "1.0.9"
```

Or separate packages:
```scala
libraryDependencies ++= Seq(
  "com.github.karasiq" %% "commons-files" % "1.0.9",
  "com.github.karasiq" %% "commons-network" % "1.0.9",
  "com.github.karasiq" %% "commons-misc" % "1.0.9",
  "com.github.karasiq" %% "commons-akka" % "1.0.9",
  "com.github.karasiq" %% "commons-configs" % "1.0.9",
)
```