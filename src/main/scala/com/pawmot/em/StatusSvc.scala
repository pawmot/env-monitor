package com.pawmot.em

import java.util.UUID

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.scaladsl.{Flow, Source, _}
import akka.stream.{FlowShape, OverflowStrategy}
import com.pawmot.em.StatusBroadcastActor._

object StatusSvc {
  private var statusBroadcastActor: ActorRef = _

  def spinUp(implicit sys: ActorSystem): Unit = {
    statusBroadcastActor = sys.actorOf(StatusBroadcastActor.props)
    val jsonSerializer = sys.actorOf(JsonSerializerActor.props)
    jsonSerializer ! statusBroadcastActor

    Conf.environments.foreach(env => {
      sys.actorOf(EnvironmentMonitorActor.props) ! EnvironmentMonitorActor.Start(env, jsonSerializer)
    })
  }

  def websocketFlow: Flow[Message, Message, _] = {
    val actorSrc = Source.actorRef[StatusUpdate](bufferSize = 5, overflowStrategy = OverflowStrategy.dropTail)

    Flow.fromGraph(GraphDSL.create(actorSrc) {
      implicit b => {
        statusSrc =>
          import GraphDSL.Implicits._

          // TODO: we don't expect any messages from client, but we can't ignore them; otherwise the connection is shut down. Investigate.
          val fromWebsocket = b.add(
            Flow[Message].collect {
              case TextMessage.Strict(_) => ClientMessage
            })

          val toWebsocket = b.add(
            Flow[StatusUpdate].map {
              case StatusUpdate(msg) => TextMessage(msg.toString())
            }
          )

          // TODO: figure out a way to get the WS connection actor and use it instead of UUID. Switch from Map to Set in StatusBroadcastActor.
          val connectionUuid = UUID.randomUUID()

          val statusActorSink = Sink.actorRef[Event](statusBroadcastActor, UnregisterClient(connectionUuid))

          val connectedActorFlow = Flow[ActorRef].map(RegisterClient(connectionUuid, _))
          val merge = b.add(Merge[Event](2))

          fromWebsocket ~> merge.in(0)
          b.materializedValue ~> connectedActorFlow ~> merge.in(1)
          merge.out ~> statusActorSink
          statusSrc ~> toWebsocket

          FlowShape.of(fromWebsocket.in, toWebsocket.out)
      }
    })
  }
}
