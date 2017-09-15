name := "env-monitor"

version := "0.1"

scalaVersion := "2.12.3"

val typesafeAkkaGroup = "com.typesafe.akka"

// akka deps
val akkaVer = "2.4.17"
libraryDependencies ++= Seq("akka-actor", "akka-slf4j")
  .map(typesafeAkkaGroup %% _ % akkaVer)

// akka http deps
val akkaHttpVer = "10.0.5"
libraryDependencies ++= Seq("akka-http", "akka-http-spray-json")
  .map(typesafeAkkaGroup %% _ % akkaHttpVer)

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.3"

cancelable in Global := true