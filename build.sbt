name := "akka-streams-consumer-app"

version := "0.1"

scalaVersion := "2.12.8"

lazy val akkaVersion = "2.5.23"
lazy val scalaTestVersion = "3.0.5"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "org.scalatest" %% "scalatest" % scalaTestVersion,
  "com.typesafe.akka" %% "akka-stream-kafka" % "2.0.0"
)

