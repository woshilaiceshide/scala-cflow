package woshilaiceshide.cflow.test

object LineExample2 extends App {

  import akka.actor._
  import scala.util._
  import scala.concurrent.duration._
  import scala.concurrent.ExecutionContext.Implicits.global
  import woshilaiceshide.cflow.line._

  val factory = ActorSystem("test")

  class Worker extends Actor with ActorLogging {
    def receive = akka.event.LoggingReceive {
      case s: String if s.startsWith("do thing #") =>
        val i = s.substring("do thing #".length()).toInt
        println(s"I'll do thing #${i} right now")
        Thread.sleep(100)
        println(s"thing #${i} is done")
        sender ! "done"
    }
  }

  val worker = factory.actorOf(Props[Worker], "worker")

  //an execution flow that results in a String
  val line = new Line[String]()

  val x: line.EndPoint = for (
    r1 <- line.ask(worker, "do thing #1", 1);
    _ <- r1 match {
      case "done" => line.justContinue()
      case _ => line.fail(500, "something is wrong for thing #1")
    };
    r2 <- line.ask(worker, "do thing #2", 1);
    _ <- r2 match {
      case "done" => line.justContinue()
      case _ => line.fail(500, "something is wrong for thing #2")
    };
    r3 <- line.ask(worker, "do thing #3", 1);
    r4 <- line.ask(worker, "do thing #4", 1);
    r5 <- line.ask(worker, "do thing #5", 1);
    _ <- line.ask(worker, "an odd thing", 1);
    rx <- line.complete("ok")
  ) yield rx

  //print the final result
  val f = x.future
  f.onComplete {
    case Success(r) => println(s"""result is ${r}""")
    case Failure(cause) => println(s"""\n*** ${cause.getMessage} ***\n""")
  }

  scala.concurrent.Await.ready(f, Duration(6, java.util.concurrent.TimeUnit.SECONDS))
  factory.terminate()
  Thread.sleep(1000)
  println("test completed")

}