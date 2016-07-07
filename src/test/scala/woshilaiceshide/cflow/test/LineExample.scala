package woshilaiceshide.cflow.test

object LineExample extends App {

  import woshilaiceshide.cflow.line._

  import scala.concurrent._
  import scala.concurrent.duration._
  import scala.concurrent.ExecutionContext.Implicits.global

  val line = new Line[String]()

  //write asynchronous logics in a synchronous way
  val x: line.EndPoint = for (
    //r1 <- line.continue { 1 };
    r1 <- line.continue { 2 };
    r2 <- line.continue("a string");
    r3 <- r1 match {
      case 1 => line.complete("completed")
      case 2 => line.fail(500, "internal server error")
    };
    //r4 can not be a result because it is not of type 'String'
    r4 <- line.continue(200)
  ) yield r3

  val f = line.retrieve(x).map { println }.recover {
    case x => println(x)
  }
  scala.concurrent.Await.ready(f, Duration(6, java.util.concurrent.TimeUnit.SECONDS))

}