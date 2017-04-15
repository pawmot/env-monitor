package com.pawmot.em

import akka.actor.{Actor, ActorRef, Props}
import com.pawmot.em.Conf.Environment
import com.pawmot.em.EnvironmentMonitorActor.{Start, Tick}

import scala.concurrent.duration._
import scala.language.postfixOps

class EnvironmentMonitorActor extends Actor {
  private var env: Environment = _
  private var statusReceiver: ActorRef = _
  private var groupMonitors: List[ActorRef] = Nil

  override def receive: Receive = {
    case Start(env, statusSink) =>
      this.env = env
      this.statusReceiver = statusSink
      groupMonitors = env.groups.map(g => {
        val ref = context.actorOf(GroupMonitorActor.props)
        ref ! GroupMonitorActor.Init(g)
        ref
      })

      implicit val ec = context.dispatcher
      context.system.scheduler.schedule(Duration.Zero, Conf.refreshInterval, self, Tick)

    case Tick =>
      context.actorOf(Props(new Actor {
        import scala.collection.mutable
        private val statuses = mutable.ListBuffer[GroupStatusReport]()

        override def receive: Receive = {
          case r @ GroupStatusReport(_, _) =>
            statuses.append(r)
            if (statuses.size == env.groups.size) {
              statusReceiver ! EnvironmentStatusReport(env.name, statuses.sortBy(_.name).toList)
              context stop self
            }
        }

        groupMonitors.foreach(_ ! GroupMonitorActor.CheckGroupHealth)
      }))
  }
}

object EnvironmentMonitorActor {
  def props: Props = Props[EnvironmentMonitorActor]

  case class Start(env: Environment, statusSink: ActorRef)
  private case object Tick
}
