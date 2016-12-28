package com.trend.spn.akka.actor

import akka.actor.Status.Failure
import akka.actor._
import akka.routing._
import scala.concurrent.duration._
import scala.util.Random

/**
 * Created by GregHuang on 12/14/15.
 */
object Command {
  case object Project
  case object Layoff
  case object Bonus
  case object Salary
  case object Invest
  case object Status
  case object Bankrupt
}

case class Work(time: Duration)
case class Income(money: Int)
case class Pay(money: Int)

class Company extends Actor with Stash {
  final val DEFAULT_EMPLOYEE = 3
  var random = new Random
  val names = Array("DinDin", "DongDong", "Foo", "Bar")
  var profit = 3000
  var router : akka.routing.Router = _

  def evalCash(money: Int): Unit = {
    profit += money
//    if (cash < 0) throw new RuntimeException("Company is bankrupt")
  }

//  val router : ActorRef =
//    context.actorOf(FromConfig.props(Employee.props(names.apply(random.nextInt(4)), random.nextInt(25)+15)), "manager")



  import Command._
  import context._

  def receive = {
    case Project => {
      println("--- New Project --")
      router.route(Work(random.nextInt(10).seconds), self)
    }
    case Income(money) => {
      println(s"--- Earn $money --")
      evalCash(money)
    }
    case Invest => {
      println("--- Pay Invest --")
      evalCash(-1000)
    }
    case Bonus => {
      println("--- Pay Bonus --")
      evalCash(-router.routees.length*500)
      router.route(Broadcast(Pay(500)), self)
    }
    case Salary => {
      println("--- Pay Salary --")
      context.children.foreach(ref => {
        val money = random.nextInt(100)
        evalCash(-money)
        ref ! Pay(money)
      }
      )
    }
    case Status => {
      val cur_profit = profit
      sender ! cur_profit
    }
    case Bankrupt => {
      println(s"--- Become Bankrupt($profit) --")
      become(goBankrupt)
    }
    case _: akka.actor.Status.Failure => {
      println("Employee is not happy")
      sender ! PoisonPill
    }
    case Terminated(employee) => {
      layoffEmployee(employee)
    }
  }

  def goBankrupt: Receive = {
    case Status => sender ! profit
    case Bankrupt => {
      router.route(Broadcast(PoisonPill), self)
    }
    case Terminated(employee) => {
      layoffEmployee(employee)
      if (router.routees.length == 0) {
        val e = new RuntimeException("Company is bankrupt")
        throw e
      }
    }
    case _ => stash()
  }

  def layoffEmployee(employee: ActorRef): Unit = {
    println("Layoff employee")
    router = router.removeRoutee(employee)
    context unwatch employee

    // Hire new employee
    val employeeRef = context.actorOf(Employee.props(names.apply(random.nextInt(100) % 4), random.nextInt(25)+15))
    context watch employeeRef
    ActorRefRoutee(employeeRef)
    router.addRoutee(employeeRef)
  }

  override def postStop(): Unit = {
    println("The company profit is " + profit + " in the end")
  }

  override def preStart(): Unit = {
    println("A company is established")
    router = {
      val routees = Vector.fill(DEFAULT_EMPLOYEE) {
        evalCash(-100)
        val employeeRef = context.actorOf(Employee.props(names.apply(random.nextInt(100) % 4), random.nextInt(25)+15))
        context watch employeeRef
        ActorRefRoutee(employeeRef)
      }
      Router(RoundRobinRoutingLogic(), routees)
    }
  }

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    context.children foreach { child ⇒
      context.unwatch(child)
      context.stop(child)
      router.removeRoutee(child)
    }
    postStop()
  }
}

//      router ! Broadcast(Pay(random.nextInt(100)))
//      router ! (Work(random.nextInt(5).seconds))