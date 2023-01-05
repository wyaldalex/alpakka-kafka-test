package com.tudux

import akka.Done
import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{FileIO, Framing}
import akka.util.ByteString
import com.tudux.generic.settings.Settings._
import org.apache.kafka.clients.producer.ProducerRecord
import upickle.default._

import java.nio.file.Paths
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

object CSVTaxiStatsProducer extends App {

  implicit val system = ActorSystem("kafkaProducerSystem")
  implicit val materializer = ActorMaterializer()

 val producerSettings: ProducerSettings[String,String] = generateProducerSettings("localhost:9092",system)
  val testTopic = "TestTopic2"

  val file = Paths.get("src/main/resources/100sample.csv")
  val fileSource = FileIO.fromPath(file)
  val toStringFlow = Framing.delimiter(ByteString("\n"), 256, true).map(_.utf8String)

  val contactInfoFuture: Future[Done] =
    fileSource.via(toStringFlow) //importing an implicit to convert String to ContactInfo
      .map(write(_)) //serialize using upickle
      .map(value => new ProducerRecord[String, String](testTopic, value))
      .runWith(Producer.plainSink(producerSettings))

  contactInfoFuture.onComplete {
    case Success(value) => println(s"Operation completed with value $value")
    case Failure(exception) => println(s"Operation failed with $exception")
  }

}
