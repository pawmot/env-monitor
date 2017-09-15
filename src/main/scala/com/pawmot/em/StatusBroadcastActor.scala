package com.pawmot.em

import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.pawmot.em.StatusBroadcastActor._
import spray.json.{JsObject, JsString, JsValue}

import scala.language.postfixOps

class StatusBroadcastActor extends Actor with ActorLogging {
  private var clients = Map[UUID, ActorRef]()
  private var latestStatusUpdates = Map[String, JsValue]()
  private var latestProgressUpdates = Map[String, JsValue]()

  override def receive: Receive = {
    case RegisterClient(uuid, clientHandle) =>
      log.info(s"Accepted client connection $uuid")
      clients += (uuid -> clientHandle)
      latestStatusUpdates.values.foreach(clientHandle ! _)
      latestProgressUpdates.values.foreach(clientHandle ! _)

    case UnregisterClient(uuid) =>
      log.info(s"Ending client connection $uuid")
      clients -= uuid

    case StatusUpdate(envName, msg) =>
      val extendedMsg = new JsObject(msg.asJsObject.fields + ("type" -> JsString("status")))
      latestStatusUpdates += (envName -> extendedMsg)
      clients.values.foreach(_ ! extendedMsg)

    case ProgressUpdate(envName, msg) =>
      val extendedMsg = new JsObject(msg.asJsObject.fields + ("type" -> JsString("progress")))
      latestProgressUpdates += (envName -> extendedMsg)
      clients.values.foreach(_ ! extendedMsg)

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
  case class StatusUpdate(envName: String, msg: JsValue) extends Event
  case class ProgressUpdate(envName: String, msg: JsValue) extends Event
}
