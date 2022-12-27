package com.tudux

import akka.Done
import akka.actor.ActorSystem
import akka.kafka.scaladsl.Consumer.DrainingControl
import akka.kafka.scaladsl.{Committer, Consumer, Producer}
import akka.kafka.{CommitterSettings, ConsumerSettings, ProducerSettings, Subscriptions}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Source}
import com.tudux.payloads.TestPayloads.ContactInfo
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}
import upickle.default._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}


object SerializationSampleProducerTest extends App {
  val somePayload = ContactInfo(101,"johndoe","johndoe@gmail.com","78987121")
  println(write(somePayload))
  println(read[ContactInfo]("""{"id":101,"name":"johndoe","email":"johndoe@gmail.com","mobile":"78987121"}"""))
}

object ProducerAppSerialization extends App {

  implicit val system = ActorSystem("kafkaProducerSystem")
  implicit val materializer = ActorMaterializer()

  val testTopic = "serializationTopic"
  val bootStrapServers = "localhost:9092"

  val producerSettings = ProducerSettings(system, new StringSerializer, new StringSerializer)
    .withBootstrapServers(bootStrapServers)

  val contactList = List(
    ContactInfo(1, "abc1", "abc1@email.com", "123456781"),
    ContactInfo(2, "abc2", "abc2@email.com", "123456782"),
    ContactInfo(3, "abc3", "abc3@email.com", "123456783"),
    ContactInfo(4, "abc4", "abc4@email.com", "123456784"),
    ContactInfo(5, "abc5", "abc5@email.com", "123456785"),
  )

  val contactInfoFuture: Future[Done] =
    Source(contactList)
      .map(write(_)) //serialize using upickle
      .map(value => new ProducerRecord[String, String](testTopic, value))
      .runWith(Producer.plainSink(producerSettings))

  contactInfoFuture.onComplete {
    case Success(value) => println(s"Operation completed with value $value")
    case Failure(exception) => println(s"Operation failed with $exception")
  }

}


object ConsumerAppSerialization extends App {

  implicit val system = ActorSystem("kafkaConsumerSystem")
  implicit val materializer = ActorMaterializer()
  val testTopic = "serializationTopic"
  val bootStrapServers = "localhost:9092"

  val config = system.settings.config.getConfig("akka.kafka.consumer")
  val consumerSettings =
    ConsumerSettings(config, new StringDeserializer, new StringDeserializer)
      .withBootstrapServers(bootStrapServers)
      .withGroupId("group1")
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  def simpleConsumption(key: String, value: String): Future[Done] = {
    val deserializedContact = read[ContactInfo](value)
    println("Testing consumption")
    println(s"Consuming message with key $key and value ${deserializedContact.email}")
    Future.successful(Done)
  }

  val committerSettings = CommitterSettings(system)
  val control: DrainingControl[Done] =
    Consumer
      .committableSource(consumerSettings, Subscriptions.topics(testTopic))
      .mapAsync(1) { msg =>
        simpleConsumption(msg.record.key, msg.record.value)
          .map(_ => msg.committableOffset)
      }
      .toMat(Committer.sink(committerSettings))(Keep.both)
      .mapMaterializedValue(DrainingControl.apply)
      .run()

}
