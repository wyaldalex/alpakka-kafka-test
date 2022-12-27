package com.tudux.payloads
import upickle.default.{ReadWriter => RW, macroRW}

object TestPayloads {

  case class ContactInfo(id: Int, name: String, email: String, mobile: String)
  //used by uPickle
  object ContactInfo {
    implicit val rw: RW[ContactInfo] = macroRW
  }
}
