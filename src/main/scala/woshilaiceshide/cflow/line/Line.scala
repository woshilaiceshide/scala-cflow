package woshilaiceshide.cflow.line

import scala.util._
import scala.concurrent._

import akka.actor._
import akka.util._

import java.util.concurrent.TimeUnit

object Line {

  final case class CausewithCode(code: Int, message: String) extends Exception(s"""${code} with ${message}""")

  final case class SomethingIsCompleted() extends Exception("something is bad")

  final case class WrappedAskTimeoutException(actor: ActorRef, asked: Any) extends Exception(s"""timeout when asking ${asked} to ${actor}""")

}

class Line[R] {

  import Line._
  import scala.util._

  final class Point[+V] private[Line] (value: Future[Either[V, R]]) {

    def map[V1](f: V => V1)(implicit ec: ExecutionContext): Point[V1] = {
      val promise = Promise[Either[V1, R]]()
      value.onComplete {
        case Success(Left(v)) => promise.complete { Success(Left(f(v))) }
        case Success(Right(r)) => promise.complete { Success(Right(r)) }
        case Failure(cause) => promise.failure(cause)
      }
      new Point(promise.future)
    }

    def flatMap[V1](f: V => Point[V1])(implicit ec: ExecutionContext): Point[V1] = {
      val promise = Promise[Either[V1, R]]()
      value.onComplete {
        case Success(Left(v)) =>
          f(v).future.onComplete {
            case Success(Left(v1)) => promise.complete { Success(Left(v1)) }
            case Success(Right(r)) => promise.complete { Success(Right(r)) }
            case Failure(cause) => promise.failure(cause)
          }

        case Success(Right(r)) => promise.complete { Success(Right(r)) }
        case Failure(cause) => promise.failure(cause)
      }
      new Point(promise.future)
    }

    def future: Future[Either[V, R]] = value

    def mapTo[S](implicit tag: scala.reflect.ClassTag[S]): Point[S] = {
      new Point(value.mapTo[Either[S, R]])
    }
  }

  type EndPoint = Point[R]

  def retrieve(x: Line[R]#EndPoint)(implicit ec: ExecutionContext): Future[R] = {
    val promise = Promise[R]
    x.future.map {
      case Left(r) => promise.complete(Success(r))
      case Right(r) => promise.complete(Success(r))
    }.onFailure {
      case cause => promise.failure(cause)
    }
    promise.future
  }

  def sequence[T](points: Seq[Line[R]#Point[T]])(implicit executor: ExecutionContext): Point[Seq[T]] = {
    val promise = Promise[Either[Seq[T], R]]()
    val futures = Future.sequence(points.map { _.future })
    futures.map { x =>
      if (x.exists(_.isRight)) {
        promise.failure(new SomethingIsCompleted())
      } else {
        val filtered = x.filter(_.isLeft).map { x => x.asInstanceOf[Left[T, R]].a }
        promise.complete(Success(Left(filtered)))
      }
    }.onFailure {
      case cause => promise.failure(cause)
    }
    new Point(promise.future)
  }

  def continue[V](value: => V)(implicit executor: ExecutionContext): Point[V] = new Point(Future(value).map { Left(_) })
  /**
   * a convenient method
   */
  def justContinue()(implicit executor: ExecutionContext): Point[Unit] = continue(())
  def fromFuture[V](value: Future[V])(implicit executor: ExecutionContext): Point[V] = new Point(value.map { Left(_) })
  def complete[V](result: => R)(implicit executor: ExecutionContext): Point[V] = new Point(Future(result).map { Right(_) })
  def fail(code: Int, message: String)(implicit executor: ExecutionContext): Point[Nothing] = fail(CausewithCode(code, message))
  def fail(cause: Throwable)(implicit executor: ExecutionContext): Point[Nothing] = new Point(Future.failed(cause))

  def ask(a: ActorRef, message: Any, timeoutInSeconds: Int)(implicit executor: ExecutionContext): Point[Any] = {
    ask(a, message, Timeout(timeoutInSeconds, TimeUnit.SECONDS))
  }

  def ask(a: ActorRef, message: Any, timeout: Timeout)(implicit executor: ExecutionContext): Point[Any] = {
    val promise = Promise[Any]()
    new akka.pattern.AskableActorRef(a).ask(message)(timeout).onComplete {
      case Success(r) => promise.complete { Success(r) }
      case Failure(_: akka.pattern.AskTimeoutException) => promise.failure(new WrappedAskTimeoutException(a, message))
      case Failure(cause) => promise.failure(cause)
    }
    fromFuture(promise.future)

  }

  def send(a: ActorRef, message: Any)(implicit executor: ExecutionContext) = {
    a ! message
    justContinue()
  }

}

