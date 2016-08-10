organization  := "com.softwaremill"

name := "streams-pres"

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.8"

val akkaVersion = "2.4.8"

libraryDependencies ++= Seq(
  // akka
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  // scalaz
  "org.scalaz.stream" %% "scalaz-stream" % "0.8.3",
  // util
  "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2",
  "ch.qos.logback" % "logback-classic" % "1.1.7",
  "joda-time" % "joda-time" % "2.9.4",
  "org.joda" % "joda-convert" % "1.8.1",
  "org.scalacheck" %% "scalacheck" % "1.13.2"
)
