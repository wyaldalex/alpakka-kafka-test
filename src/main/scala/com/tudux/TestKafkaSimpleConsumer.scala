package com.tudux

import akka.Done
import akka.actor.ActorSystem
import akka.kafka.{CommitterSettings, ConsumerSettings, Subscriptions}
import akka.kafka.scaladsl.{Committer, Consumer}
import akka.kafka.scaladsl.Consumer.DrainingControl
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Keep
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Success

object TestKafkaSimpleConsumer extends App {

  implicit val system = ActorSystem("kafkaConsumerSystem")
  implicit val materializer = ActorMaterializer()
  val testTopic = "testTopic1"
  val bootStrapServers = "localhost:9092"

  val config = system.settings.config.getConfig("akka.kafka.consumer")
  val consumerSettings =
    ConsumerSettings(config, new StringDeserializer, new StringDeserializer)
      .withBootstrapServers(bootStrapServers)
      .withGroupId("group1")
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  def simpleConsumption(key: String, value: String): Future[Done] = {
    println(s"Consuming message with key $key and value ${value}")
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
