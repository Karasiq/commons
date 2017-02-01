# commons
Mixed Scala utility classes

# How to use
Add to your build.sbt:
```scala
libraryDependencies += "com.github.karasiq" %% "commons" % "1.0.5"
```

Or separate packages:
```scala
libraryDependencies ++= Seq(
  "com.github.karasiq" %% "commons-files" % "1.0.5",
  "com.github.karasiq" %% "commons-network" % "1.0.5",
  "com.github.karasiq" %% "commons-misc" % "1.0.5",
  "com.github.karasiq" %% "commons-akka" % "1.0.5"
)
```