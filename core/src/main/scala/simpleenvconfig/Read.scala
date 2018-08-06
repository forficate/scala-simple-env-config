package simpleenvconfig

import java.net.URI

import scalaz.Maybe

// Safe string conversion / parsing
final case class Read[A](run: String => Maybe[A]) {
  def read(value: String): Maybe[A] =
    run(value)

  def map[B](f: A => B): Read[B] =
    Read { run(_).map(f) }

  def mapMaybe[B](f: A => Maybe[B]): Read[B] =
    Read { run(_).flatMap(f) }

  def flatMap[B](f: A => Read[B]): Read[B] =
    Read { str => run(str).map(f).cata(_.run(str), Maybe.empty) }
}

object Read extends ReadInstances {
  @SuppressWarnings(Array("org.wartremover.warts.Overloading"))
  def apply[A: Read]: Read[A] =
    implicitly[Read[A]]
}

trait ReadInstances {
  implicit val nonEmptyStringReader: Read[NonEmptyString] =
    Read { NonEmptyString(_) }

  implicit val booleanReader: Read[Boolean] =
    Read[NonEmptyString].mapMaybe {
      _.value.toLowerCase match {
        case "true"  => Maybe.just(true)
        case "false" => Maybe.just(false)
        case _       => Maybe.empty
      }
    }

  implicit val uriReader: Read[URI] =
    Read[NonEmptyString].mapMaybe(txt => Maybe.fromTryCatchNonFatal(new URI(txt.value)))

  implicit val intReader: Read[Int] =
    Read[NonEmptyString].mapMaybe(txt => Maybe.fromTryCatchNonFatal(txt.value.toInt))

  implicit val longReader: Read[Long] =
    Read[NonEmptyString].mapMaybe(txt => Maybe.fromTryCatchNonFatal(txt.value.toLong))
}
