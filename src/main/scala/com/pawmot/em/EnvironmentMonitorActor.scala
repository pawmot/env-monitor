package com.pawmot.em

import akka.actor.{Actor, ActorRef, Props}
import com.pawmot.em.Conf.Environment
import com.pawmot.em.EnvironmentMonitorActor.{Start, Tick}

import scala.concurrent.duration._
import scala.language.postfixOps

class EnvironmentMonitorActor extends Actor {
  private var env: Environment = _
  private var totalServicesNumber: Int = _
  private var statusReceiver: ActorRef = _
  private var groupMonitors: List[ActorRef] = Nil

  override def receive: Receive = {
    case Start(env, statusSink) =>
      this.env = env
      totalServicesNumber = env.groups.map(_.services.size).sum
      this.statusReceiver = statusSink
      groupMonitors = env.groups.map(g => {
        val ref = context.actorOf(GroupMonitorActor.props)
        ref ! GroupMonitorActor.Init(g)
        ref
      })

      implicit val ec = context.dispatcher
      context.system.scheduler.schedule(Duration.Zero, Conf.refreshInterval, self, Tick)

    case Tick =>
      // TODO: don't start if already in progress
      context.actorOf(Props(new Actor {
        import scala.collection.mutable
        private val statuses = mutable.ListBuffer[GroupStatusReport]()
        private val progressReports = mutable.Map[ActorRef, ProgressReport]()

        override def receive: Receive = {
          case r @ GroupStatusReport(_, _) =>
            statuses.append(r)
            if (statuses.size == env.groups.size) {
              statusReceiver ! EnvironmentStatusReport(env.name, env.ordinal, statuses.sortBy(_.name).toList)
              context stop self
            }

          case pr @ ProgressReport(r, t) =>
            progressReports(sender()) = pr
            val ready = progressReports.map(kvp => kvp._2.ready).sum
            statusReceiver ! ProgressReport(ready, totalServicesNumber)
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
