name := "env-monitor"

version := "0.1"

scalaVersion := "2.12.1"

val akkaVer = "2.4.17"
val akkaHttpVer = "10.0.5"
val typesafeAkkaGroup = "com.typesafe.akka"
libraryDependencies ++= Seq("akka-actor")
  .map(typesafeAkkaGroup %% _ % akkaVer)
