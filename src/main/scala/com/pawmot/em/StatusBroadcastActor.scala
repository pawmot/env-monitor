package com.pawmot.em

import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.pawmot.em.StatusBroadcastActor._
import spray.json.JsValue

import scala.language.postfixOps

class StatusBroadcastActor extends Actor with ActorLogging {
  private var clients = Map[UUID, ActorRef]()

  override def receive: Receive = {
    case RegisterClient(uuid, clientHandle) =>
      log.info(s"Accepted client connection $uuid")
      clients += (uuid -> clientHandle)
    // TODO: when the status providers are ready, request the cached status and send it to the new client.

    case UnregisterClient(uuid) =>
      log.info(s"Ending client connection $uuid")
      clients -= uuid

    case msg@StatusUpdate(_) =>
      clients.values.foreach(_ ! msg)

    case msg =>
      log.debug(s"Unknown message: $msg")
  }
}

object StatusBroadcastActor {
  def props: Props = Props[StatusBroadcastActor]

  sealed trait Event

  case class RegisterClient(uuid: UUID, clientHandle: ActorRef) extends Event
  case class UnregisterClient(uuid: UUID) extends Event
  case object ClientMessage extends Event
  case class StatusUpdate(msg: JsValue) extends Event
}
