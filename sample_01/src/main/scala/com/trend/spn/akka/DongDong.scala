/**
 * HW1: Simply modify this program or write a small one your own
 * - Must create at least 2 Actors.
 * - Actor interaction: send a message from one actor to one of the others.
 * - Define what action should be taken when an actor receives a message.
 */

package com.trend.spn.akka

import akka.actor.Actor
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.ActorRef
import akka.actor.actorRef2Scala

trait Question
case class Interest() extends Question
case class Why() extends Question

trait Answer
case class Three(name: String, a: String, b: String, c: String) extends Answer
case class Two(name: String, a: String, b: String) extends Answer
case class Because(name: String, msg: String) extends Answer

/**
 * Penguin
 */
class Penguin(val name: String) extends Actor {
  def receive = {
    case Interest() =>
      sender() ! Three(name, "Eat", "Sleep", "Punch DongDong")
  }

  override def preStart() = {
    println(s"$name start")
  }
}

/**
 * Penguin DongDong
 */
class DongDong extends Penguin("DongDong") {
  override def receive = {
    case Interest() =>
      sender() ! Two(name, "Eat", "Sleep")
    case Why() => sender() ! Because(name, "I am " + name)
  }
}

/**
 * Reporter
 */
class Reporter extends Actor {
  def receive = {
    /* Reply three interests */
    case Three(name, a, b, c) => 
      println(s"$name: $a, $b, $c")

    /* Only reply two interests, then ask why? */      
    case Two(name, a, b) =>
      println(s"$name: $a, $b")
      sender() ! Why()

      /* Reply to case Why() */
    case Because(name, msg) =>
      println(s"$name: $msg")
  }
}

object JokeApp extends App {
  val system = ActorSystem("Joke")

  val reporter = system.actorOf(Props[Reporter], "reporter")

  val penguins = new Array[ActorRef](10)

  for (i <- 0 to 8) {
    penguins(i) = system.actorOf(Props(classOf[Penguin], s"Penguin-$i"))
  }

  penguins(9) = system.actorOf(Props[DongDong])

  /* Reporter ask each penguin about there interest */
  penguins.foreach(_.tell(Interest(), reporter))

  /* Ask main thread to sleep to make sure all the method calls can be finishd. 
   * (But it is not a clever way)*/
  Thread.sleep(5000)
  system.shutdown
  println("end")
}
