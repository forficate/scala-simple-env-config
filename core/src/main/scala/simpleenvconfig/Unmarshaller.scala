package simpleenvconfig

import java.net.URI

import scala.annotation.tailrec

import Unmarshaller.Error
import scalaz.{ -\/, \/, \/-, Maybe, NonEmptyList }
import scalaz.syntax.either._
import scalaz.syntax.id._
import scalaz.syntax.maybe._

// Typeclass for safe typed environment variable parsing
final case class Unmarshaller[A](run: Maybe[String] ⇒ Unmarshaller.Error \/ A) {
  def read(s: Maybe[String]): Unmarshaller.Error \/ A =
    run(s)

  def map[B](f: A ⇒ B): Unmarshaller[B] =
    Unmarshaller { run(_).map(f) }

  def mapError[B](f: A ⇒ Unmarshaller.Error \/ B): Unmarshaller[B] =
    Unmarshaller { run(_).flatMap(f) }

  def flatMap[B](f: A ⇒ Unmarshaller[B]): Unmarshaller[B] =
    Unmarshaller {
      str ⇒ run(str).flatMap { f(_).run(str) }
    }

  def recover(err: PartialFunction[Unmarshaller.Error, A]): Unmarshaller[A] =
    Unmarshaller { run(_).recover(err) }
}

object Unmarshaller extends UnmarshallerInstances {

  sealed trait Error
  object Error {
    case object MissingEnvValue extends Error
    final case class InvalidEnvValue(provided: String, expected: String) extends Error
  }

  @SuppressWarnings(Array("org.wartremover.warts.Overloading"))
  def apply[A: Unmarshaller]: Unmarshaller[A] =
    implicitly[Unmarshaller[A]]
}

trait UnmarshallerInstances {

  implicit val stringValueUnmarshaller: Unmarshaller[String] =
    Unmarshaller { _ \/> Error.MissingEnvValue }

  implicit val nonEmptyStringValueUnmarshaller: Unmarshaller[NonEmptyString] =
    Unmarshaller { a ⇒
      Read[NonEmptyString].read(a.getOrElse("")) \/> Error.MissingEnvValue
    }

  implicit val boolValueUnmarshaller: Unmarshaller[Boolean] =
    Unmarshaller[NonEmptyString].mapError {
      value ⇒ Read[Boolean].read(value.value) \/> Error.InvalidEnvValue(value.value, "boolean")
    }

  implicit val intValueUnmarshaller: Unmarshaller[Int] =
    Unmarshaller[NonEmptyString].mapError {
      value ⇒ Read[Int].read(value.value) \/> Error.InvalidEnvValue(value.value, "integer")
    }

  implicit val longValueUnmarshaller: Unmarshaller[Long] =
    Unmarshaller[NonEmptyString].mapError {
      value ⇒ Read[Long].read(value.value) \/> Error.InvalidEnvValue(value.value, "long")
    }

  implicit val uriValueUnmarshaller: Unmarshaller[URI] =
    Unmarshaller[NonEmptyString].mapError {
      value ⇒ Read[URI].read(value.value) \/> Error.InvalidEnvValue(value.value, "uri")
    }

  implicit def maybeValueUnmarshaller[A: Unmarshaller]: Unmarshaller[Maybe[A]] =
    Unmarshaller {
      Unmarshaller[A].read(_) match {
        case -\/(Error.MissingEnvValue) ⇒ \/.right[Unmarshaller.Error, Maybe[A]](Maybe.empty)
        case other                      ⇒ other.map(Maybe.just)
      }
    }

  implicit def nonEmptyListValueUnmarshaller[A: Unmarshaller]: Unmarshaller[NonEmptyList[A]] =
    Unmarshaller { value ⇒
      val list = value.getOrElse("").split(",").map(_.trim).filter(_.nonEmpty).toList

      list match {
        case x :: xs ⇒
          NonEmptyList(x, xs: _*).traverse1[Unmarshaller.Error \/ ?, A](_.just |> Unmarshaller[A].read)
        case Nil ⇒
          Unmarshaller.Error.InvalidEnvValue(value.getOrElse("EMPTY_VALUE"), "nonemptylist").left[NonEmptyList[A]]
      }
    }

  implicit def listValueUnmarshaller[A: Unmarshaller]: Unmarshaller[List[A]] =
    Unmarshaller { value ⇒
      @tailrec
      def loop(items: List[String], accum: List[A]): Unmarshaller.Error \/ List[A] =
        items match {
          case x :: xs ⇒
            Unmarshaller[A].read(x.just) match {
              case \/-(a) ⇒
                loop(xs, a +: accum)
              case -\/(_) ⇒
                Unmarshaller.Error.InvalidEnvValue(value.getOrElse("EMPTY_VALUE"), "list").left[List[A]]
            }
          case Nil ⇒
            accum.right[Unmarshaller.Error]
        }

      val list = value.getOrElse("").split(",").map(_.trim).filter(_.nonEmpty).toList
      loop(list, List.empty[A])
    }

}
