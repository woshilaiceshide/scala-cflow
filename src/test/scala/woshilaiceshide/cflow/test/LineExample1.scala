package woshilaiceshide.cflow.test

object LineExample1 extends App {

  import woshilaiceshide.cflow.line._

  import scala.concurrent._
  import scala.concurrent.duration._
  import scala.concurrent.ExecutionContext.Implicits.global

  //an execution flow that results in a String
  val line = new Line[String]()

  def doThing[T](t: T) = { println(s"""do thing #(${t})"""); t }
  val thing1_1 = line.continue { doThing("1-1"); "1-1" }
  val thing1_2 = line.continue { doThing("1-2"); "1-2" }

  //write asynchronous logics in a synchronous way
  //the following flow contains several steps, the logic may be halted in the middle.
  val x: line.EndPoint = for (

    _ <- thing1_1;
    _ <- thing1_2;
    r1 <- line.continue { doThing(System.currentTimeMillis()) };
    _ <- {
      if (r1 % 2 == 0) {
        println(s"""got an even number, and continue""")
        line.justContinue()
      } else {
        println(s"""got an odd number, and failed""")
        line.fail(500, "got an odd number")
      }
    };
    j1 <- line.continue { doThing("j1") };
    j2 <- line.continue { doThing("j2") };
    j3 <- line.continue { doThing("j3") };

    j4 <- line.continue { doThing("fix bugs") };
    j5 <- line.complete("bugs are fixed");

    //j6 can not be a result because it is not of type 'String'
    j6 <- line.continue(200)
  ) yield j4

  val f = line.retrieve(x).map { println }.recover {
    case x => println(x)
  }
  scala.concurrent.Await.ready(f, Duration(6, java.util.concurrent.TimeUnit.SECONDS))
  println("test completed")

}