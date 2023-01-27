package com.tudux

import akka.Done
import akka.actor.ActorSystem
import akka.kafka.scaladsl.Consumer.DrainingControl
import akka.kafka.scaladsl.{Committer, Consumer}
import akka.kafka.{CommitterSettings, ConsumerMessage, ConsumerSettings, Subscriptions}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, Sink}
import io.confluent.kafka.serializers.{AbstractKafkaAvroSerDeConfig, KafkaAvroDeserializer}
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecord}
import org.apache.kafka.common.serialization.Deserializer
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.jdk.CollectionConverters.mapAsJavaMapConverter

//import spray everything
import spray.json._


object AvroConsumerAggregationApp extends App with DefaultJsonProtocol {

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
      //.withProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true")
      .withProperty(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "10")

  implicit val paymentEntryFormat = jsonFormat6(PaymentEntry)

  def transformToPayment(msg: ConsumerRecord[GenericRecord, GenericRecord], value: GenericRecord): Tuple2[ConsumerRecord[GenericRecord, GenericRecord],PaymentEntry] = {
    val afterRecord = value.get("after").toString.parseJson.convertTo[PaymentEntry]
    (msg,afterRecord)
  }


  val committerSettings = CommitterSettings(system)

  val AckMessage = AggregatorActor.Ack
  val InitMessage = AggregatorActor.StreamInitialized
  val OnCompleteMessage = AggregatorActor.StreamCompleted
  val onErrorMessage = (ex: Throwable) => AggregatorActor.StreamFailure(ex)

  val receiver = system.actorOf(AggregatorActor.props(AckMessage))

  val aggregatorSink = Sink.actorRefWithAck(
    receiver,
    onInitMessage = InitMessage,
    ackMessage = AckMessage,
    onCompleteMessage = OnCompleteMessage,
    onFailureMessage = onErrorMessage)

  val (consumerControl, streamComplete) =
    Consumer
      .plainSource(consumerSettings,
        Subscriptions.topics(testTopic))
      .map(msg => transformToPayment(msg, msg.value()))
      //.map(msg => (msg,Done))
      .toMat(aggregatorSink)(Keep.both)
      .run()

  //consumerControl.shutdown()

}
