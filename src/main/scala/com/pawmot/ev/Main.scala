package com.pawmot.ev

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

object Main extends App {
  implicit val system = ActorSystem("env-monitor-system")
  implicit val materializer = ActorMaterializer()

  StatusSvc.spinUp
  WebServer.startServer
}
