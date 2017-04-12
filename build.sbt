name := "env-monitor"

version := "0.1"

scalaVersion := "2.12.1"

val typesafeAkkaGroup = "com.typesafe.akka"

// akka deps
val akkaVer = "2.4.17"
libraryDependencies ++= Seq("akka-actor")
  .map(typesafeAkkaGroup %% _ % akkaVer)

// akka http deps
val akkaHttpVer = "10.0.5"
libraryDependencies ++= Seq("akka-http")
  .map(typesafeAkkaGroup %% _ % akkaHttpVer)