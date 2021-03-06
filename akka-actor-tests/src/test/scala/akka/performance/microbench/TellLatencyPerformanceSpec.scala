package akka.performance.microbench

import akka.performance.workbench.PerformanceSpec
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics
import org.junit.runner.RunWith
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.PoisonPill
import akka.actor.Props
import java.util.Random
import org.apache.commons.math.stat.descriptive.SynchronizedDescriptiveStatistics

// -server -Xms512M -Xmx1024M -XX:+UseConcMarkSweepGC -Dbenchmark=true -Dbenchmark.repeatFactor=500
@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class TellLatencyPerformanceSpec extends PerformanceSpec {
  import TellLatencyPerformanceSpec._

  val clientDispatcher = system.dispatcherFactory.newDispatcher("client-dispatcher")
    .withNewThreadPoolWithLinkedBlockingQueueWithUnboundedCapacity
    .setCorePoolSize(8)
    .build

  val repeat = 200L * repeatFactor
  def clientDelayMicros = {
    System.getProperty("benchmark.clientDelayMicros", "250").toInt
  }

  var stat: DescriptiveStatistics = _

  override def beforeEach() {
    stat = new SynchronizedDescriptiveStatistics
  }

  "Tell" must {
    "warmup" in {
      runScenario(2, warmup = true)
    }
    "warmup more" in {
      runScenario(4, warmup = true)
    }
    "perform with load 1" in {
      runScenario(1)
    }
    "perform with load 2" in {
      runScenario(2)
    }
    "perform with load 4" in {
      runScenario(4)
    }
    "perform with load 6" in {
      runScenario(6)
    }
    "perform with load 8" in {
      runScenario(8)
    }

    def runScenario(numberOfClients: Int, warmup: Boolean = false) {
      if (acceptClients(numberOfClients)) {

        val latch = new CountDownLatch(numberOfClients)
        val repeatsPerClient = repeat / numberOfClients
        val clients = (for (i ← 0 until numberOfClients) yield {
          val destination = system.actorOf[Destination]
          val w4 = system.actorOf(new Waypoint(destination))
          val w3 = system.actorOf(new Waypoint(w4))
          val w2 = system.actorOf(new Waypoint(w3))
          val w1 = system.actorOf(new Waypoint(w2))
          Props(new Client(w1, latch, repeatsPerClient, clientDelayMicros, stat)).withDispatcher(clientDispatcher)
        }).toList.map(system.actorOf(_))

        val start = System.nanoTime
        clients.foreach(_ ! Run)
        val ok = latch.await((5000000 + 500 * repeat) * timeDilation, TimeUnit.MICROSECONDS)
        val durationNs = (System.nanoTime - start)

        if (!warmup) {
          ok must be(true)
          logMeasurement(numberOfClients, durationNs, stat)
        }
        clients.foreach(_ ! PoisonPill)

      }
    }
  }
}

object TellLatencyPerformanceSpec {

  val random: Random = new Random(0)

  case object Run
  case class Msg(nanoTime: Long = System.nanoTime)

  class Waypoint(next: ActorRef) extends Actor {
    def receive = {
      case msg: Msg ⇒ next forward msg
    }
  }

  class Destination extends Actor {
    def receive = {
      case msg: Msg ⇒ sender ! msg
    }
  }

  class Client(
    actor: ActorRef,
    latch: CountDownLatch,
    repeat: Long,
    delayMicros: Int,
    stat: DescriptiveStatistics) extends Actor {

    var sent = 0L
    var received = 0L

    def receive = {
      case Msg(sendTime) ⇒
        val duration = System.nanoTime - sendTime
        stat.addValue(duration)
        received += 1
        if (sent < repeat) {
          PerformanceSpec.shortDelay(delayMicros, received)
          actor ! Msg()
          sent += 1
        } else if (received >= repeat) {
          latch.countDown()
        }
      case Run ⇒
        // random initial delay to spread requests
        val initialDelay = random.nextInt(20)
        Thread.sleep(initialDelay)
        actor ! Msg()
        sent += 1
    }

  }

}