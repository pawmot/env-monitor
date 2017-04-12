package com.pawmot.ev

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer

object WebServer {
  def startServer(implicit sys: ActorSystem, mat: Materializer) {
    implicit val ec = implicitly[ActorSystem].dispatcher

    val staticFilesDirPath = Conf.staticFileDirPath

    val statusWsRoute = path("statusWs") {
      handleWebSocketMessages(StatusSvc.websocketFlow)
    }

    val apiRoute = pathPrefix("api" / Remaining) { path =>
      get {
        complete(HttpEntity(ContentTypes.`application/json`, s"""{ "path": "$path" }"""))
      }
    }

    val staticFilesRoute = pathPrefix(Remaining) { path =>
      encodeResponse {
        getFromFile(s"$staticFilesDirPath/$path")
      }
    } ~
      pathSingleSlash {
        encodeResponse {
          getFromFile(s"$staticFilesDirPath/index.html")
        }
      }

    val route = apiRoute ~ statusWsRoute ~ staticFilesRoute

    val bindingFuture = Http().bindAndHandle(route, Conf.host, Conf.port)

    bindingFuture.foreach(sb => {
      println(s"Server listening at ${sb.localAddress}")
    })
  }
}
