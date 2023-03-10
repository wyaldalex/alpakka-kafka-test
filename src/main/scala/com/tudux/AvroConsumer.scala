package com.tudux

import akka.Done
import akka.actor.ActorSystem
import akka.kafka.scaladsl.Consumer.DrainingControl
import akka.kafka.scaladsl.{Committer, Consumer}
import akka.kafka.{CommitterSettings, ConsumerSettings, Subscriptions}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Keep
import io.confluent.kafka.serializers.{AbstractKafkaAvroSerDeConfig, KafkaAvroDeserializer}
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecord}
import org.apache.kafka.common.serialization.{Deserializer, StringDeserializer}
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.jdk.CollectionConverters.mapAsJavaMapConverter

//import spray everything
import spray.json._

case class PaymentEntry(payment_id: Int, notes: String, total: Double , beneficiary_id: Int, insurance_id: Int, insurance_type: Int)

object AvroConsumerApp extends App with DefaultJsonProtocol {

  private val log = LoggerFactory.getLogger(AvroConsumerApp.getClass)

  implicit val system = ActorSystem("kafkaConsumerSystem")
  implicit val materializer = ActorMaterializer()
  val testTopic = "payments.public.insurance_payments"
  val bootStrapServers = "localhost:29092"
  val schemaRegistryUrl = "http://localhost:8079"

  /*
  after/before:
  {"payment_id": 13, "notes": "Extra payment", "total": 50.5, "beneficiary_id": 33, "insurance_id": 17818, "insurance_type": 3}
   */

  val kafkaAvroDeserializerConfig = Map[String, Any] {
    AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG -> schemaRegistryUrl
  }
  val kafkaAvroDeserializer = new KafkaAvroDeserializer
  kafkaAvroDeserializer.configure(kafkaAvroDeserializerConfig.asJava, false)

  val config = system.settings.config.getConfig("akka.kafka.consumer")
  val consumerSettings =
    ConsumerSettings(config, kafkaAvroDeserializer.asInstanceOf[Deserializer[GenericRecord]],
      kafkaAvroDeserializer.asInstanceOf[Deserializer[GenericRecord]])
      .withBootstrapServers(bootStrapServers)
      .withGroupId("alpakka.group97.avro")
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  implicit val paymentEntryFormat = jsonFormat6(PaymentEntry)
  def simpleConsumption(key: String, value: GenericRecord): Future[Done] = {
    log.info(s"Consuming message with key $key and value ${value}")
    val beforeRecord = value.get("before") match {
      case null => Left("Empty Before Record")
      case x => Right(x.toString.parseJson.convertTo[PaymentEntry])
    }

    log.info(s"Before type: $beforeRecord")
    val afterRecord = value.get("after").toString.parseJson.convertTo[PaymentEntry]
    //log.info(s"Parsed Before record: [${beforeRecord.toString}]")
    log.info(s"Parsed After record: [${afterRecord.toString}]")
    Future.successful(Done)
  }

  val committerSettings = CommitterSettings(system)
  val control: DrainingControl[Done] =
    Consumer
      .committableSource(consumerSettings, Subscriptions.topics(testTopic))
      .mapAsync(1) { msg =>
        simpleConsumption(msg.record.key.toString, msg.record.value)
          .map(_ => msg.committableOffset)
      }
      .toMat(Committer.sink(committerSettings))(Keep.both)
      .mapMaterializedValue(DrainingControl.apply)
      .run()

}
