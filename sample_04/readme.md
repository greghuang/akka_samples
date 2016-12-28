**心得：不寫code的話真的不知道doc再說三毀！**

**本次作業也非常簡單，拿一個之前的homework，然後修改default mailbox，為priority mailbox! 並使用config和code裡面指定的方法去assign新的mailbox**

# Mailbox

Normally each Acotr has its own mailbox, but with for a **BalancingPool** all routees will share a single mailbox instance.

## Mailbox Selection for Actor
Require a certain type of message queue for a actor by extend the parameterized trait **RequiresMessageQueue**

```scalc
import akka.dispatch.RequiresMessageQueueimport akka.dispatch.BoundedMessageQueueSemanticsclass MyBoundedActor extends MyActor  with RequiresMessageQueue[BoundedMessageQueueSemantics]
```
```scala
bounded-mailbox {  mailbox-type = "akka.dispatch.BoundedMailbox"  mailbox-capacity = 1000  mailbox-push-timeout-time = 10s}akka.actor.mailbox.requirements {  "akka.dispatch.BoundedMessageQueueSemantics" = bounded-mailbox}
```
Mailbox跟MessageQueue需要分開看待，分別提供不同的設定與功能！
比如說，MessageQueue可以有unbounded或bounded之分；而Mailbox本身還包含更多屬性

If the actor has a different mailbox configured in deployment, either directly or via a dispatcher with a specified mailbox type, then that will override this mapping. --> configuration有較高的優先權

## Mailbox Selection for Dispatcher

A dispatcher may also have a requirement for the mailbox type used by the actors running on it. Such a requirement is formulated within the dispatcher configuration section like this:

```scala
￼my-dispatcher {  mailbox-requirement = org.example.MyInterface
```

_The given requirement names a class or interface which will then be ensured to be a supertype of the message queue’s implementation. if the actor requires a mailbox type which does not satisfy this requirement—then actor creation will fail._

## Default Mailbox

+ Unbounded
+ java.util.concurrent.ConcurrentLinkedQueue
+ **SingleConsumerOnlyUnboundedMailbox** is an even more efficient mailbox

Configuration of SingleConsumerOnlyUnboundedMailbox as default mailbox:```scala
￼akka.actor.default-mailbox {  mailbox-type = "akka.dispatch.SingleConsumerOnlyUnboundedMailbox"}
```

## Builtin Implementation

+ UnboundedMailbox
	+ java.util.concurrent.ConcurrentLinkedQueue
	+ Nonblocking
	+ Configuration name: “unbounded” or “akka.dispatch.UnboundedMailbox”
+ SingleConsumerOnlyUnboundedMailbox
	+ Backed by a very efficient Multiple-Producer Single-Consumer queue, cannot be used with Balanc- ingDispatcher
	+ Nonblocking
	+ Configuration name: “akka.dispatch.SingleConsumerOnlyUnboundedMailbox”
+ BoundedMailbox
	+ Backed by a java.util.concurrent.LinkedBlockingQueue
	+ Blocking
	+ Configuration name: “bounded” or “akka.dispatch.BoundedMailbox”
+ NonBlockingBoundedMailbox
	+ Backed by a very efficient MultiPle-Producer Multiple-Consumer queue
	+ Noblocking
	+ Configuration name: “akka.dispatch.NonBlockingBoundedMailbox”
+ BoundedStablePriorityMailbox
	+ Backed by a java.util.PriorityQueue wrapped in an akka.util.PriorityQueueStabilizer and an akka.util.BoundedBlockingQueue
	+ FIFO order is preserved for messages of equal priority - contrast with the BoundedPriorityMailbox
	+ Configuration name: “akka.dispatch.BoundedStablePriorityMailbox”
+ UnboundedControlAwareMailbox
	+ Delivers messages that extend akka.dispatch.ControlMessage with higher priority
	+ Backed by two java.util.concurrent.ConcurrentLinkedQueue
	+ Noblocking
	+ Configuration name: “akka.dispatch.UnboundedControlAwareMailbox”+ More...

## Configuration Examples
### PriorityMailbox
[Source code](https://github.com/akka/akka/blob/master/akka-docs/rst/scala/code/docs/dispatcher/DispatcherDocSpec.scala)

```scala
import akka.dispatch.PriorityGeneratorimport akka.dispatch.UnboundedStablePriorityMailboximport com.typesafe.config.Config// We inherit, in this case, from UnboundedStablePriorityMailbox// and seed it with the priority generatorclass MyPrioMailbox(settings: ActorSystem.Settings, config: Config)  extends UnboundedStablePriorityMailbox(    // Create a new PriorityGenerator, lower prio means more important    PriorityGenerator {      // ’highpriority messages should be treated first if possible      case ’highpriority => 0      // ’lowpriority messages should be treated last if possible      case ’lowpriority  => 2      // PoisonPill when no other left      case PoisonPill    => 3      // We default to 1, which is in between high and low      case otherwise     => 1    }) 
```And then add it to the configuration:
	
	prio-dispatcher {  	mailbox-type = "docs.dispatcher.DispatcherDocSpec$MyPrioMailbox"  	//Other dispatcher configuration goes here	}

And then an example on how you would use it:

```scala
// We create a new Actor that just prints out what it processesclass Logger extends Actor {  val log: LoggingAdapter = Logging(context.system, this)  self ! ’lowpriority  self ! ’lowpriority  self ! ’highpriority  self ! ’pigdog  self ! ’pigdog2  self ! ’pigdog3  self ! ’highpriority  self ! PoisonPill  def receive = {    case x => log.info(x.toString)} }val a = system.actorOf(Props(classOf[Logger], this).withDispatcher(  "prio-dispatcher"))/* * Logs: * ’highpriority * ’highpriority * ’pigdog * ’pigdog2
 * ’pigdog3 * ’lowpriority * ’lowpriority */
```
## ControlAwareMailbox

A ControlAwareMailbox can be very useful if an actor needs to be able to receive control messages immediately no matter how many other messages are already in its mailbox.```scala
control-aware-dispatcher {  mailbox-type = "akka.dispatch.UnboundedControlAwareMailbox"  //Other dispatcher configuration goes here}
```

Control messages need to extend the ControlMessage trait:

```scala
￼import akka.dispatch.ControlMessagecase object MyControlMessage extends ControlMessage
```

```scala
// We create a new Actor that just prints out what it processesclass Logger extends Actor {  val log: LoggingAdapter = Logging(context.system, this)  self ! ’foo  self ! ’bar  self ! MyControlMessage  self ! PoisonPill  def receive = {    case x => log.info(x.toString)} }val a = system.actorOf(Props(classOf[Logger], this).withDispatcher(  "control-aware-dispatcher"))
```
## How the Mailbox Type is Selected

1. If the actor’s **deployment** configuration section contains a mailbox key then that names a configuration section describing the mailbox type to be used.2. If the actor’s Props contains a mailbox selection—i.e. **withMailbox** was called on it—then that names a configuration section describing the mailbox type to be used.3. If the **dispatcher’s configuration** section contains a **mailbox-type** key the same section will be used to configure the mailbox type.4. If the actor requires a mailbox type as described above then the mapping for that requirement will be used to determine the mailbox type to be used; if that fails then the dispatcher’s requirement—if any—will be tried instead. 如果actor需要mailbox對應的requirement沒有話，換改成用dispatcher's取代5. If the dispatcher requires a mailbox type as described above then the mapping for that requirement will be used to determine the mailbox type to be used.6. The default mailbox akka.actor.default-mailbox will be used.
## Reference

**String with single quote ex. 'abc**

In Java terms, symbols are interned strings. This means, for example, that reference equality comparison (eq in Scala and == in Java) gives the same result as normal equality comparison (== in Scala and equals in Java): 'abcd eq 'abcd will return true, while "abcd" eq "abcd" might not, depending on JVM's whims (well, it should for literals, but not for strings created dynamically in general).

Other languages which use symbols are Lisp (which uses 'abcd like Scala), Ruby (:abcd), Erlang and Prolog (abcd; they are called atoms instead of symbols).

I would use a symbol when I don't care about the structure of a string and use it purely as a name for something. For example, if I have a database table representing CDs, which includes a column named "price", I don't care that the second character in "price" is "r", or about concatenating column names; so a database library in Scala could reasonably use symbols for table and column names.

**Caused by: java.lang.IllegalArgumentException: produced message queue type [class akka.dispatch.UnboundedStablePriorityMailbox$MessageQueue] does not fulfill requirement for actor class [class com.trend.spn.akka.actor.Company]. Must be a subclass of [interface akka.dispatch.DequeBasedMessageQueueSemantics].**

The root cause of this exception is due to Stash, since Stash requires _DequeBasedMessageQueueSemantics_, so Company actor can't use _UnboundedControlAwareMailbox_