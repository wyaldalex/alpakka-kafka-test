package com.tudux.generic.settings

import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import com.tudux.ProducerAppSerialization.system
import org.apache.kafka.common.serialization.StringSerializer

object Settings {

  def generateProducerSettings (bootStrapServers: String, system: ActorSystem): ProducerSettings[String,String] = {
    ProducerSettings(system, new StringSerializer, new StringSerializer)
      .withBootstrapServers(bootStrapServers)
  }

}
