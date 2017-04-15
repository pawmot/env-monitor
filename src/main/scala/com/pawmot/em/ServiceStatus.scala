package com.pawmot.em

import akka.http.scaladsl.model.StatusCode
import com.pawmot.em.ServiceStatus.ServiceStatus

object ServiceStatus extends Enumeration {
  type ServiceStatus = Value
  val Healthy = Value("Healthy")
  val Unhealthy = Value("Unhealthy")
  val Unreachable = Value("Unreachable")
  val TimedOut = Value("TimedOut")
}

object SimpleServiceStatus {
  def healthy = SimpleServiceStatusReport(ServiceStatus.Healthy, 200)
  def unhealthy(statusCode: StatusCode) = SimpleServiceStatusReport(ServiceStatus.Healthy, statusCode.intValue())
  def unreachable = SimpleServiceStatusReport(ServiceStatus.Unreachable)
  def timedOut = SimpleServiceStatusReport(ServiceStatus.TimedOut)
}

case class SimpleServiceStatusReport(status: ServiceStatus, statusCode: Int = -1)

case class FullServiceStatusReport(name: String, simpleReport: SimpleServiceStatusReport, additionalInfo: Map[String, String])
case class GroupStatusReport(name: String, services: List[FullServiceStatusReport])
case class EnvironmentStatusReport(name: String, groups: List[GroupStatusReport])