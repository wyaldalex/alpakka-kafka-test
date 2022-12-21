package com.tudux

import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}

object TestKafka extends App {

  val system = ActorSystem("testKafka")
  val producerSettings = ProducerSettings(system, new ByteArraySerializer, new StringSerializer)
    .withBootstrapServers("localhost:9092")

}
