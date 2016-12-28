package com.trend.spn.akka

import akka.actor.{PoisonPill, ActorSystem}
import akka.dispatch.{PriorityGenerator, UnboundedStablePriorityMailbox}
import com.trend.spn.akka.actor.{Gossip, Nap, Meeting}
import com.typesafe.config.Config

/**
 * Created by GregHuang on 1/25/16.
 */
class MyPriorityMailbox(settings: ActorSystem.Settings, config: Config)
  extends UnboundedStablePriorityMailbox(
    PriorityGenerator {
      case Nap => 0
      case Meeting => 1
      case Gossip => 3
      // PoisonPill when no other left
      case PoisonPill => 4
      // We default to 1, which is in between high and low
      case _ => 2
    })

