**心得：不寫code的話真的不知道doc再說三毀！**

**本次作業也非常簡單，拿一個之前的homework，然後修改default mailbox，為priority mailbox! 並使用config和code裡面指定的方法去assign新的mailbox**

# Mailbox

Normally each Acotr has its own mailbox, but with for a **BalancingPool** all routees will share a single mailbox instance.

## Mailbox Selection for Actor
Require a certain type of message queue for a actor by extend the parameterized trait **RequiresMessageQueue**

```scalc
import akka.dispatch.RequiresMessageQueue
```
```scala
bounded-mailbox {
```
Mailbox跟MessageQueue需要分開看待，分別提供不同的設定與功能！
比如說，MessageQueue可以有unbounded或bounded之分；而Mailbox本身還包含更多屬性

If the actor has a different mailbox configured in deployment, either directly or via a dispatcher with a specified mailbox type, then that will override this mapping. --> configuration有較高的優先權

## Mailbox Selection for Dispatcher

A dispatcher may also have a requirement for the mailbox type used by the actors running on it. Such a requirement is formulated within the dispatcher configuration section like this:

```scala
￼my-dispatcher {
```

_The given requirement names a class or interface which will then be ensured to be a supertype of the message queue’s implementation. if the actor requires a mailbox type which does not satisfy this requirement—then actor creation will fail._

## Default Mailbox

+ Unbounded
+ java.util.concurrent.ConcurrentLinkedQueue
+ **SingleConsumerOnlyUnboundedMailbox** is an even more efficient mailbox

Configuration of SingleConsumerOnlyUnboundedMailbox as default mailbox:
￼akka.actor.default-mailbox {
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
	+ Configuration name: “akka.dispatch.UnboundedControlAwareMailbox”
 More...

## Configuration Examples
### PriorityMailbox
[Source code](https://github.com/akka/akka/blob/master/akka-docs/rst/scala/code/docs/dispatcher/DispatcherDocSpec.scala)

```scala
import akka.dispatch.PriorityGenerator
```
	
	prio-dispatcher {

And then an example on how you would use it:

```scala
// We create a new Actor that just prints out what it processes
 * ’pigdog3
```
## ControlAwareMailbox

A ControlAwareMailbox can be very useful if an actor needs to be able to receive control messages immediately no matter how many other messages are already in its mailbox.
control-aware-dispatcher {
```

Control messages need to extend the ControlMessage trait:

```scala
￼import akka.dispatch.ControlMessage
```

```scala
// We create a new Actor that just prints out what it processes
```
## How the Mailbox Type is Selected

1. If the actor’s **deployment** configuration section contains a mailbox key then that names a configuration section describing the mailbox type to be used.
## Reference

**String with single quote ex. 'abc**

In Java terms, symbols are interned strings. This means, for example, that reference equality comparison (eq in Scala and == in Java) gives the same result as normal equality comparison (== in Scala and equals in Java): 'abcd eq 'abcd will return true, while "abcd" eq "abcd" might not, depending on JVM's whims (well, it should for literals, but not for strings created dynamically in general).

Other languages which use symbols are Lisp (which uses 'abcd like Scala), Ruby (:abcd), Erlang and Prolog (abcd; they are called atoms instead of symbols).

I would use a symbol when I don't care about the structure of a string and use it purely as a name for something. For example, if I have a database table representing CDs, which includes a column named "price", I don't care that the second character in "price" is "r", or about concatenating column names; so a database library in Scala could reasonably use symbols for table and column names.

**Caused by: java.lang.IllegalArgumentException: produced message queue type [class akka.dispatch.UnboundedStablePriorityMailbox$MessageQueue] does not fulfill requirement for actor class [class com.trend.spn.akka.actor.Company]. Must be a subclass of [interface akka.dispatch.DequeBasedMessageQueueSemantics].**

The root cause of this exception is due to Stash, since Stash requires _DequeBasedMessageQueueSemantics_, so Company actor can't use _UnboundedControlAwareMailbox_