package com.pawmot.ev

import com.typesafe.config.ConfigFactory

object Conf {
  private val conf = ConfigFactory.load()

  private def envMonitorProp(name: String) = s"env-monitor.$name"

  val host: String = conf.getString(envMonitorProp("host"))
  val port: Int = conf.getInt(envMonitorProp("port"))
  val staticFileDirPath: String = conf.getString(envMonitorProp("staticFilesDir"))
}
