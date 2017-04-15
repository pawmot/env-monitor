package com.pawmot.em

import akka.actor.Status.Failure
import akka.actor.{Actor, ActorLogging, Cancellable, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.pattern.pipe
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, StreamTcpException}
import com.pawmot.em.HttpRequesterActor.{ConnectionRefused, Timeout}

class HttpRequesterActor extends Actor with ActorLogging {
  private val http = Http(context.system)
  private var working: Boolean = false
  private var scheduleHandle: Cancellable = _

  import context.dispatcher

  implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system))

  override def receive: Receive = {
    case url: String =>
      if (working) {
        log.error(s"${HttpRequesterActor.getClass.getSimpleName} already received a URL to request!")
      } else {
        working = true
        http.singleRequest(HttpRequest(uri = url)).pipeTo(self)
        scheduleHandle = context.system.scheduler.scheduleOnce(Conf.timeout, self, Timeout)
      }

    // TODO: investigate what happens when connection is refused
    case resp@HttpResponse(_, _, _, _) =>
      log.info("Request succeeded")
      context.parent ! resp
      scheduleHandle.cancel()
      context stop self

    case Failure(_) =>
      // TODO: check if exception is an instance of StreamTcpException
      log.info("Request failed")
      context.parent ! ConnectionRefused
      scheduleHandle.cancel()
      context stop self

    case Timeout =>
      log.info("Request timed out")
      context.parent ! Timeout
      context stop self
  }
}

object HttpRequesterActor {
  def props: Props = Props[HttpRequesterActor]

  case class Init(url: String)
  case object Execute
  case object Timeout
  case object ConnectionRefused
}
