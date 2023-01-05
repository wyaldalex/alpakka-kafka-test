package com.tudux.generic.conversion

import com.tudux.payloads.TestPayloads.ContactInfo

object Conversions {

  val rand = new scala.util.Random

  implicit def csvEntryToContact(str: String): ContactInfo = {
    val arrayString = str.split(",")
    ContactInfo(
      rand.nextInt(100000),
      arrayString(1),
      arrayString(2),
      arrayString(3),
    )
  }
}
