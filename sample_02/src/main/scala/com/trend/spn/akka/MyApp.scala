package com.trend.spn.akka

import akka.actor._
import akka.util.Timeout
import com.trend.spn.akka.actor.Command.Status
import com.trend.spn.akka.actor.Command._
import com.trend.spn.akka.actor.{Outsourcer, Company}
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.Random
import akka.pattern.Patterns.gracefulStop
import akka.pattern.ask

/**
 * Created by GregHuang on 12/14/15.
 */
object MyApp extends App {
  val system = ActorSystem("Homework2")
  val company = system.actorOf(Props[Company], "myCompany")
  val outsourcer = system.actorOf(Props[Outsourcer], "vender")

  //Use system's dispatcher as ExecutionContext

  import system.dispatcher

  val salScheduler = system.scheduler.schedule(5.seconds, 5.seconds) {
    company ! Salary
  }

  implicit val timeout = Timeout(10 seconds) // for ask below

  val inbox = Inbox.create(system)
  inbox watch company

  val statusScheduler = system.scheduler.schedule(1.seconds, 1.seconds) {
    val future: Future[Int] = ask(company, Status).mapTo[Int]
    future onSuccess {
      case x => if (x < 0) company ! Bankrupt
    }
  }

  val random = new Random
  var stat: Double = 0.0
  val evtScheduler = system.scheduler.schedule(1.seconds, 1.seconds) {
    stat = random.nextGaussian()
    if (-1.5 < stat && stat < 1.5) company ! Project
    if (stat >= 1.5)  company ! Invest
    if (stat <= -1.5) company ! Bonus
  }

  sys.ShutdownHookThread {
    println("Gracefully shut down app")
    try {
      statusScheduler.cancel()
      salScheduler.cancel()
      evtScheduler.cancel()

      Await.result(gracefulStop(company, 5 seconds, PoisonPill), Duration.Inf)
    }
    catch {
      case e : akka.pattern.AskTimeoutException => System.err.println(e)
    }
    System.out.println("Application stopped")
  }
}
