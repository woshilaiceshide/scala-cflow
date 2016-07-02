package woshilaiceshide.cflow.will

import scala.concurrent.ExecutionContext

//TODO
//http://stackoverflow.com/questions/9619121/why-is-parameter-in-contravariant-position
sealed trait Will[+V, +R] {
  def map[V1](f: V => V1)(implicit ec: ExecutionContext): Will[V1, R] = throw new scala.NotImplementedError
  def flatMap[V1, R1 >: R](f: V => Will[V1, R1])(implicit ec: ExecutionContext): Will[V1, R1] = throw new scala.NotImplementedError
}
case class Continue[+V, +R](value: V) extends Will[V, R]
case class Complete[+V, +R](result: R) extends Will[V, R]
trait Fail[+V, +R] extends Will[V, R]
final case class FailedWithCode[+V, +R](code: Int, message: String) extends Fail[V, R]
final case class FailedWithThrowable[+V, +R](cause: Throwable) extends Fail[V, R]

object Will {

  def continue[V, R](value: => V): Continue[V, R] = Continue(value)
  def complete[V, R](r: => R): Complete[V, R] = Complete(r)
  def fail[V, R](code: Int, message: String): FailedWithCode[V, R] = FailedWithCode(code, message)
  def fail[V, R](cause: Throwable): FailedWithThrowable[V, R] = FailedWithThrowable(cause)

}
