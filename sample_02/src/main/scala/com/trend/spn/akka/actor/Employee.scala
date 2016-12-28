package com.trend.spn.akka.actor

import akka.actor.{ActorRef, ActorLogging, Props, Actor}
import akka.pattern.ask
import akka.util.Timeout
import com.trend.spn.akka.actor.Command.{Layoff, Project}
import scala.concurrent.duration._

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.util.Random

/**
 * Created by GregHuang on 12/14/15.
 */
object Employee {
  def props(name: String, age: Int): Props =
    Props(classOf[Employee], name, age)
}


class Employee(name: String, age: Int) extends Actor with ActorLogging {
  final val myname = name
  final val myage = age
  val random = new Random
  var deposit: Int = 0
  var mood: Long = random.nextInt(70) + 35

  log.info(s"$myname($myage) is a employee now")

  def evalCash(money: Int): Unit = {
    deposit += money
    evalMood(money/100)
  }

  def evalMood(feeling: Long): Unit = {
    mood = Math.min(mood + feeling, 100)
    if (mood < 10) sender ! akka.actor.Status.Failure(new RuntimeException(s"$myname($myage) is too tired"))
  }

  def outsourceJob(time: Duration, caller: ActorRef) = {
    implicit val timeout = Timeout(10 seconds) // for ask below
    import context.dispatcher

    val sel = context.actorSelection("/user/vender")

    val future = ask(sel, Work(time))
    future.onSuccess {
      case JobDone => caller ! Income(random.nextInt(500) + 100)
    }
  }

  def receive = {
    case Work(time) => {
      val prop = random.nextGaussian()
      if (prop < -1) {
        println("Send job to outsourcer")
        outsourceJob(time, sender)
      }
      else {
        evalMood(-(time.toSeconds))
        Thread.sleep(time.toMillis)
        println(s"$myname($myage) finished project")
        sender ! Income(random.nextInt(500) + 100)
      }
    }
    case Pay(money) => {
      println(s"$myname($myage) got money:$money")
      evalCash(money)
    }
  }

  override def postStop(): Unit = {
    println(s"Employee $myname($myage) has $deposit with $mood in the end")
  }
}
