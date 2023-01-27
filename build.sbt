name := "akka-streams-consumer-app"

version := "0.1"

scalaVersion := "2.12.8"

lazy val akkaVersion = "2.5.23"
lazy val scalaTestVersion = "3.0.5"

resolvers += "confluent" at "https://packages.confluent.io/maven/"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed"           % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "org.scalatest" %% "scalatest" % scalaTestVersion,
  "com.typesafe.akka" %% "akka-stream-kafka" % "2.0.0",
  "com.lihaoyi" %% "upickle" % "2.0.0",
  "org.slf4j" % "slf4j-simple" % "2.0.5",
  "org.slf4j" % "slf4j-api" % "2.0.5",

  //serialization related
  "com.sksamuel.avro4s" %% "avro4s-core" % "2.0.4",
  "io.spray" %%  "spray-json" % "1.3.5",
  // https://mvnrepository.com/artifact/io.confluent/kafka-avro-serializer
   "io.confluent" % "kafka-avro-serializer" % "3.3.1"


)

