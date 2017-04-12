package com.pawmot.ev

import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.scaladsl.Flow

object StatusSvc {
  def websocketFlow: Flow[Message, Message, _] = Flow[Message].map {
    case TextMessage.Strict(txt) => TextMessage("ECHO: " + txt)
    case _ => TextMessage("Not Supported")
  }
}
