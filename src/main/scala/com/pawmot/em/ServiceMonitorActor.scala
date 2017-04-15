package com.pawmot.em

import akka.actor.{Actor, ActorRef, Props}
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import com.pawmot.em.Conf.Service
import com.pawmot.em.HttpRequesterActor.{ConnectionRefused, Execute, Timeout}
import com.pawmot.em.ServiceMonitorActor.{CheckServiceHealth, Init}

class ServiceMonitorActor extends Actor {
  import SimpleServiceStatus._

  private var service: Service = _
  private var requester: ActorRef = _

  override def receive: Receive = {
    case Init(service) =>
      this.service = service
      requester = context.actorOf(HttpRequesterActor.props)
      requester ! HttpRequesterActor.Init(service.healthEndpoint)

    case CheckServiceHealth =>
      val replyTo = sender()

      context.actorOf(Props(new Actor {
        override def receive: Receive = {
          case HttpResponse(StatusCodes.OK, _, entity, _) =>
            // TODO: extract additional information from entity if it's a JSON
            replyTo ! FullServiceStatusReport(service.name, healthy, Map("healthURL" -> service.healthEndpoint))

          case HttpResponse(code, _, entity, _) =>
            // TODO: extract additional information from entity if it's a JSON
            replyTo ! FullServiceStatusReport(service.name, unhealthy(code), Map("healthURL" -> service.healthEndpoint))

          case ConnectionRefused =>
            replyTo ! FullServiceStatusReport(service.name, unreachable, Map("healthURL" -> service.healthEndpoint))

          case Timeout =>
            replyTo ! FullServiceStatusReport(service.name, timedOut, Map("healthURL" -> service.healthEndpoint))
        }

        requester ! Execute
      }))
  }
}

object ServiceMonitorActor {
  def props: Props = Props[ServiceMonitorActor]

  case class Init(service: Service)
  case object CheckServiceHealth
}