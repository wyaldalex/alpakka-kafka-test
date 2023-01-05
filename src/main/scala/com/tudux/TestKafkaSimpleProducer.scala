package com.tudux

import akka.Done
import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import com.tudux.payloads.TestPayloads.ContactInfo
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

object TestKafkaSimpleProducer extends App {

  implicit val system = ActorSystem("kafkaProducerSystem")
  implicit val materializer = ActorMaterializer()

  val testTopic = "TestTopic1"
  val bootStrapServers = "localhost:9092"

  val producerSettings = ProducerSettings(system, new StringSerializer, new StringSerializer)
    .withBootstrapServers(bootStrapServers)


//  val stringNumbersFuture: Future[Done] =
//    Source(1 to 100)
//      .map(_.toString)
//      .map(value => new ProducerRecord[String, String](testTopic, value))
//      .runWith(Producer.plainSink(producerSettings))

  val contactList = List(
    ContactInfo(1,"abc","abc@email.com","12345678"),
    ContactInfo(2,"abc","abc@email.com","12345678"),
    ContactInfo(3,"abc","abc@email.com","12345678"),
    ContactInfo(4,"abc","abc@email.com","12345678"),
    ContactInfo(5,"abc","abc@email.com","12345678"),
  )
  val contactInfoFuture: Future[Done] =
    Source(contactList)
      .map(_.toString)
      .map(value => new ProducerRecord[String, String](testTopic, value))
      .runWith(Producer.plainSink(producerSettings))

  //numbers
//  stringNumbersFuture.onComplete {
//    case Success(value) =>  println("Operation completed with value $value")
//    case Failure(exception)  => println(s"Operation failed with $exception")
//  }

  contactInfoFuture.onComplete {
    case Success(value) => println(s"Operation completed with value $value")
    case Failure(exception) => println(s"Operation failed with $exception")
  }

}
