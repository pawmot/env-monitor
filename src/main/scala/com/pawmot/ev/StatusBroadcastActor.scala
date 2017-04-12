package com.pawmot.ev

import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

import scala.concurrent.duration._
import com.pawmot.ev.StatusBroadcastActor._

import scala.language.postfixOps

class StatusBroadcastActor extends Actor with ActorLogging {
  private var clients = Map[UUID, ActorRef]()

  override def receive: Receive = {
    case RegisterClient(uuid, clientHandle) =>
      log.debug(s"Accepted client connection $uuid")
      clients += (uuid -> clientHandle)

    case UnregisterClient(uuid) =>
      log.debug(s"Ending client connection $uuid")
      clients -= uuid

    case msg @ StatusUpdate(_) =>
      clients.values.foreach(_ ! msg)

      // TODO: two cases below can be deleted once there are actors providing Statuses.
    case Start =>
      implicit val ec = context.dispatcher
      context.system.scheduler.schedule(Duration.Zero, 5 seconds, self, Tick)

    case Tick =>
      log.debug("tick")
      self ! StatusUpdate("tick tock!")

    case msg =>
      log.info(s"Unknown message: $msg")
  }
}

object StatusBroadcastActor {
  def props: Props = Props[StatusBroadcastActor]

  sealed trait Event
  case class RegisterClient(uuid: UUID, clientHandle: ActorRef) extends Event
  case class UnregisterClient(uuid: UUID) extends Event
  case object ClientMessage extends Event
  case class StatusUpdate(msg: String) extends Event

  case object Start
  private case object Tick
}
