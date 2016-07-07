package woshilaiceshide.cflow.test

object LineExample extends App {

  import woshilaiceshide.cflow.line._

  import scala.concurrent._
  import scala.concurrent.duration._
  import scala.concurrent.ExecutionContext.Implicits.global

  val line = new Line[String]()

  //write asynchronous logics in a synchronous way
  //the following flow contains several steps, the logic may be halted in the middle.
  val x: line.EndPoint = for (

    r0 <- line.continue {
      println("I am busy")
      Thread.sleep(3 * 1000)
      println("work is finished")
      "fine"
    };

    _ <- line.justContinue();

    //r1 <- line.continue { 2 };
    r1 <- line.continue { 1 };

    r2 <- line.continue("a string");

    r3 <- line.continue { System.currentTimeMillis() };

    r4 <- if (0 == r3 % 2) {
      line.continue("ok, it's even")
    } else {
      line.fail(400, "bad, it's odd")
    };

    r5 <- line.continue("so hot").flatMap {
      case "so hot" => line.continue("calm down")
      case _ => line.continue("good")
    };

    r6 <- r1 match {
      case 1 => line.complete("completed")
      case 2 => line.fail(500, "internal server error")
    };

    //r7 can not be a result because it is not of type 'String'
    r7 <- line.continue(200)
  ) yield r6

  val f = line.retrieve(x).map { println }.recover {
    case x => println(x)
  }
  scala.concurrent.Await.ready(f, Duration(6, java.util.concurrent.TimeUnit.SECONDS))

}