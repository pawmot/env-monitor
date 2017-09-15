package com.pawmot.em

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.pawmot.em.Conf.Group
import com.pawmot.em.GroupMonitorActor.{CheckGroupHealth, Init}

class GroupMonitorActor extends Actor with ActorLogging {
  private var group: Group = _
  private var serviceMonitors: List[ActorRef] = Nil

  override def receive: Receive = {
    case Init(group) =>
      this.group = group
      this.serviceMonitors = group.services.map(s => {
        val ref = context.actorOf(ServiceMonitorActor.props)
        ref ! ServiceMonitorActor.Init(s)
        ref
      })

    case CheckGroupHealth =>
      val replyTo = sender()

      context.actorOf(Props(new Actor {
        import scala.collection.mutable
        private val statuses = mutable.ListBuffer[FullServiceStatusReport]()

        override def receive: Receive = {
          case r @ FullServiceStatusReport(_, _, _) =>
            statuses.append(r)
            if (statuses.size == group.services.size) {
              replyTo ! ProgressReport(statuses.size, group.services.size)
              replyTo ! GroupStatusReport(group.name, statuses.sortBy(_.name).toList)
              context stop self
            } else {
              replyTo ! ProgressReport(statuses.size, group.services.size)
            }
        }

        serviceMonitors.foreach(_ ! ServiceMonitorActor.CheckServiceHealth)
      }))
  }
}

object GroupMonitorActor {
  def props: Props = Props[GroupMonitorActor]

  case class Init(group: Group)
  case object CheckGroupHealth
}