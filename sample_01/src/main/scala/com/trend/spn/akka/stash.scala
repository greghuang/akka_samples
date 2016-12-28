/**
 * HW1-2:
 * The Stash trait enables an actor to temporarily stash away messages that can not
 * or should not be handled using the actor's current behavior.
 * - Write a simple program to try stash() and unstash All.
 * - You should import all the packages needed by your own!
 */

import akka.actor._
import scala.concurrent.duration._

import scala.concurrent.Await

case object ByeBye
case object Weekend
case object Weekday
case class Invite(what: String)

// An short example ...
class MyGirlFriend extends Actor with Stash {
  import context._
  var answer = ""
  def weekend: Receive = {
    case Invite(action) => System.out.println(s"Yes, it is weekend, we are going to $action")
    case _ => unbecome()
  }

  def weekday: Receive = {
    case Weekend =>
      unstashAll()
      become(weekend)
    case ByeBye => unbecome()
    case _ => stash()
  }

  def receive = {
    case Weekday => become(weekday)
    case Weekend => become(weekend)
    case ByeBye =>
      unstashAll()
      sender ! "Bye Bye"
    case Invite(what) => println(what)
  }
}

object MyHomeWork1 extends App {
  val system = ActorSystem("Homework1")

  val myGF = system.actorOf(Props[MyGirlFriend], "myGF")

  val inbox = Inbox.create(system)

  inbox.send(myGF, Weekday)

  myGF ! Invite("dinner")
  myGF ! Invite("see movie")

  myGF ! Weekend
  myGF ! Invite("Punch DongDong")

  //inbox.send(myGF, Weekday)
  myGF ! Weekday
  Thread.sleep(500)
  myGF ! Invite("xxxx")

  sys.ShutdownHookThread {
    try {
      System.out.println("Time to say goodbye")
      inbox.send(myGF, ByeBye)
      val msg = inbox.receive(5 seconds)
      System.out.println("My girl say " + msg)
    }
    catch {
      case e : akka.pattern.AskTimeoutException => System.err.println(e)
    }
    System.out.println("Application stopped")
  }
}