package com.pawmot.em

import akka.actor.{Actor, ActorRef, Props}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.pawmot.em.ServiceStatus.ServiceStatus
import com.pawmot.em.StatusBroadcastActor.{ProgressUpdate, StatusUpdate}
import spray.json.{DefaultJsonProtocol, _}

class JsonSerializerActor extends Actor with JsonSupport {
  private var broadcastActor: ActorRef = _

  override def receive: Receive = {
    case ref: ActorRef =>
      broadcastActor = ref

    case report @ EnvironmentStatusReport(envName, _, _) =>
      broadcastActor ! StatusUpdate(envName, report.toJson)

    case report @ EnvironmentProgressReport(envName, _, _) =>
      broadcastActor ! ProgressUpdate(envName, report.toJson)
  }
}

object JsonSerializerActor {
  def props: Props = Props[JsonSerializerActor]
}

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val environmentStatusReportFormat: JsonFormat[EnvironmentStatusReport] = lazyFormat(jsonFormat(EnvironmentStatusReport, "name", "ordinal", "groups"))
  implicit val groupStatusReportFormat: JsonFormat[GroupStatusReport] = lazyFormat(jsonFormat(GroupStatusReport, "name", "services"))
  implicit val fullServiceStatusReportFormat: JsonFormat[FullServiceStatusReport] = lazyFormat(jsonFormat(FullServiceStatusReport, "name", "report", "additionalInfo"))
  implicit val simpleServiceStatusReportFormat: JsonFormat[SimpleServiceStatusReport] = lazyFormat(jsonFormat(SimpleServiceStatusReport, "status", "statusCode"))
  implicit val environmentProgressReportFormat: JsonFormat[EnvironmentProgressReport] = lazyFormat(jsonFormat(EnvironmentProgressReport, "envName", "ready", "total"))
  implicit val serviceStatusFormat: JsonFormat[ServiceStatus] = new JsonFormat[ServiceStatus] {
    override def write(obj: ServiceStatus): JsValue = {
      JsString(obj.toString)
    }

    override def read(json: JsValue): ServiceStatus = json match {
      case JsString(str) => ServiceStatus.withName(str)
      case _ => throw DeserializationException("Enum string expected")
    }
  }
}