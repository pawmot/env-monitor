package com.pawmot.em

import akka.actor.Status.Failure
import akka.actor.{Actor, ActorLogging, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.model.headers.Connection
import akka.stream.ActorMaterializer
import com.pawmot.em.HttpRequesterActor.{ConnectionRefused, Execute, Init, Timeout}
import akka.pattern._

class HttpRequesterActor extends Actor with ActorLogging {
  private var url: String = _

  import context.dispatcher
  private implicit val sys = context.system
  private implicit val mat: ActorMaterializer = ActorMaterializer()

  override def receive: Receive = {
    case Init(url) =>
      this.url = url

    case Execute =>
      val replyTo = sender()

      context.actorOf(Props(new Actor {
        Http().singleRequest(HttpRequest(uri = url).withHeaders(Connection("Keep-Alive"))).pipeTo(self)
        private var scheduleHandle = context.system.scheduler.scheduleOnce(Conf.timeout, self, Timeout)

        override def receive: Receive = {
          case resp@HttpResponse(_, _, _, _) =>
            log.info(s"Request to $url succeeded")
            // TODO: remove this when starting to parse the entity
            resp.discardEntityBytes()
            replyTo ! resp
            scheduleHandle.cancel()
            context stop self

          case Failure(_) =>
            // TODO: check if exception is an instance of StreamTcpException
            log.info(s"Request to $url failed")
            replyTo ! ConnectionRefused
            scheduleHandle.cancel()
            context stop self

          case Timeout =>
            log.info(s"Request to $url timed out")
            replyTo ! Timeout
            context stop self
        }
      }))
  }
}

object HttpRequesterActor {
  def props: Props = Props[HttpRequesterActor]

  case class Init(url: String)
  case object Execute
  case object Timeout
  case object ConnectionRefused
}
