package com.pawmot.em

import com.typesafe.config.{ConfigFactory, ConfigObject, ConfigValue}

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.language.postfixOps

object Conf {
  private val conf = ConfigFactory.load()
  private val envConf = ConfigFactory.load("envs")

  val host: String = conf.getString(envMonitorProp("host"))
  val port: Int = conf.getInt(envMonitorProp("port"))
  val staticFileDirPath: String = conf.getString(envMonitorProp("staticFilesDir"))
  // TODO: make these two configurable through API
  val refreshInterval: FiniteDuration = conf.getInt(envMonitorProp("refresh")) seconds
  val timeout: FiniteDuration = conf.getInt(envMonitorProp("timeout")) seconds

  val environments: List[Environment] = getEnvironments

  case class Environment(name: String, ordinal: Int, groups: List[Group])
  case class Group(name: String, services: List[Service])
  case class Service(name: String, healthEndpoint: String)

  private def envMonitorProp(name: String) = s"env-monitor.$name"

  private def getEnvironments: List[Environment] = {
    envConf.getObjectList(envMonitorProp("envs")).asScala.toList
      .map(obj => Environment(obj.get("name").render.replace("\"", ""), obj.get("ordinal").render().toInt, getGroups(obj.get("groups"))))
  }

  private def getGroups(value: ConfigValue): List[Group] = {
    value.asInstanceOf[java.util.List[ConfigObject]].asScala.toList
      .map(obj => Group(obj.get("name").render.replace("\"", ""), getServices(obj.get("services"))))
  }

  private def getServices(value: ConfigValue): List[Service] = {
    value.asInstanceOf[java.util.List[ConfigObject]].asScala.toList
      .map(obj => Service(obj.get("name").render.replace("\"", ""), obj.get("healthAddress").render.replace("\"", "")))
  }
}
