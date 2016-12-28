package com.trend.spn.akka

import java.util.concurrent.ConcurrentLinkedQueue

import akka.actor.{ActorSystem, ActorRef}
import akka.dispatch.{ProducesMessageQueue, MailboxType, Envelope, MessageQueue}
import com.typesafe.config.Config

/**
 * Created by GregHuang on 1/25/16.
 */

// Marker trait used for mailbox requirements mapping
trait MyUnboundedMessageQueueSemantics

object MyUnboundedMailbox {
  class MyMessageQueue extends MessageQueue with MyUnboundedMessageQueueSemantics {
    private final val queue = new ConcurrentLinkedQueue[Envelope]()

    override def enqueue(receiver: ActorRef, handle: Envelope): Unit = queue.offer(handle)

    override def numberOfMessages: Int = queue.size()

    override def dequeue(): Envelope = queue.poll()

    override def cleanUp(owner: ActorRef, deadLetters: MessageQueue): Unit = {
      while (hasMessages) {
        deadLetters.enqueue(owner, dequeue())
      }
    }
    override def hasMessages: Boolean = !queue.isEmpty
  }
}

// This is the Mailbox implementation
class MyUnboundedMailbox extends MailboxType with ProducesMessageQueue[MyUnboundedMailbox.MyMessageQueue] {

  import MyUnboundedMailbox._

  // This constructor signature must exist, it will be called by Akka
  def this(settings: ActorSystem.Settings, config: Config) = {
    // put your initialization code here
    this()
    println("MyUnboundedMailbox is instantiated")
  }

  override def create(owner: Option[ActorRef], system: Option[ActorSystem]): MessageQueue = new MyMessageQueue()
}


