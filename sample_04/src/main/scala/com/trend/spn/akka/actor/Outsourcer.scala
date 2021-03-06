package com.trend.spn.akka.actor

import akka.actor.{ActorLogging, Actor}
import akka.dispatch.RequiresMessageQueue
import com.trend.spn.akka.MyUnboundedMessageQueueSemantics

import scala.util.Random

/**
 * Created by GregHuang on 12/28/15.
 */
case object JobDone

class Outsourcer extends Actor with ActorLogging with RequiresMessageQueue[MyUnboundedMessageQueueSemantics]{
  lazy val factor:Int = (new Random).nextInt(10)

  def receive = {
    case Work(time) => {
      Thread.sleep(time.toMillis / factor)
      sender ! JobDone
    }
  }

  override def preStart(): Unit = {
    log.info(s"A vender($factor) is created")
  }
}
