# scala-cflow
Some control structures written in scala. 

Currently, it just contains a class named `'woshilaiceshide.cflow.line.Line[R]'`: 

* let you write asynchronous logics in a synchronous way **without any nested braces**.

* provides an convenient `'ask'` instead of `'akka.pattern.ask'`. Especially, the `'akka.pattern.ask'` will provides nothing meaningful when facing timeouts. 

# Example
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
	
	  val worker = factory.actorOf(Props[Worker], "aActor")
	
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
	
	  val f = x.future
	  f.onComplete {
	    case Success(r) => println(s"""result is ${r}""")
	    case Failure(cause) => println; println(s"""*** ${cause.getMessage} ***"""); println
	  }
	
	  scala.concurrent.Await.ready(f, Duration(6, java.util.concurrent.TimeUnit.SECONDS))
	  factory.terminate()
	  Thread.sleep(1000)
	  println("test completed")

	
Its output is show as follow: 

	I'll do thing #1 right now
	thing #1 is done
	I'll do thing #2 right now
	thing #2 is done
	I'll do thing #3 right now
	thing #3 is done
	I'll do thing #4 right now
	thing #4 is done
	I'll do thing #5 right now
	thing #5 is done
	
	*** timeout when asking an odd thing to Actor[akka://test/user/aActor#1434941999] ***
	
	test completed

For more examples, please see https://github.com/woshilaiceshide/scala-cflow/tree/master/src/test/scala/woshilaiceshide/cflow/test .
	
# Thanks

