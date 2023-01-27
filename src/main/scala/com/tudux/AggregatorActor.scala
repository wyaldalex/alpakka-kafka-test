package com.tudux

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.ConsumerRecord

object AggregatorActor {
  case object Ack

  case object StreamInitialized
  case object StreamCompleted
  case object LogTotal
  final case class StreamFailure(ex: Throwable)

  def props(ackWith: Any): Props = Props(new AggregatorActor(ackWith))
}

class AggregatorActor(ackWith: Any) extends Actor with ActorLogging {
  import AggregatorActor._

  var counterMap = Map[Int,Double]()

  def updateMap(payment: PaymentEntry) = {
    counterMap = counterMap + (payment.insurance_type -> (counterMap(payment.insurance_type) + payment.total))
  }

  def receive: Receive = {
    case StreamInitialized =>
      log.info("Stream initialized!")
      sender() ! Ack // ack to allow the stream to proceed sending more elements

    case el: Tuple2[ConsumerRecord[GenericRecord, GenericRecord],PaymentEntry] =>
      val payment = el._2
      log.info("Received element: {}", el)
      if(counterMap.contains(el._2.insurance_type)) updateMap(payment)
      else counterMap = counterMap + (payment.insurance_type -> payment.total)
      log.info(s"Aggregation result ${counterMap.toString()}")
      sender() ! Ack // ack to allow the stream to proceed sending more elements

    case LogTotal =>
      log.info(s"Aggregation result ${counterMap.toString()}")

    case StreamCompleted =>
      log.info("Stream completed!")
    case StreamFailure(ex) =>
      log.error(ex, "Stream failed!")
  }
}