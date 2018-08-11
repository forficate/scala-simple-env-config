package simpleenvconfig

import Unmarshaller.Error._
import org.scalacheck.Arbitrary
import Arbitrary._
import org.scalacheck.Gen
import org.specs2.{ ScalaCheck, Specification }
import scala.util.Try

import scalaz.{ -\/, @@, \/, Maybe, Tag }

@SuppressWarnings(Array("org.wartremover.warts.Any", "org.wartremover.warts.Nothing"))
class UnmarshallerSpec extends Specification with ScalaCheck {
  import Unmarshaller._
  import scalaz.syntax.std.option._

  def is = s2"""
    Unmarshaller for environment variable parsing should
      provide a String Unmarshaller instance returning Maybe.empty for missing values or Maybe.Just(value) for parseable values     $stringUnmarshaller
      provide a Boolean Unmarshaller instance returning Maybe.empty for missing values or Maybe.Just(value) for parseable booleans  $booleanUnmarshaller
      provide a Int Unmarshaller instance returning Maybe.empty for missing values or Maybe.Just(value) for parseable ints          $intUnmarshaller
      provide a Long Unmarshaller instance returning Maybe.empty for missing values or Maybe.Just(value) for parseable longs        $longUnmarshaller
      provide a NonEmptyString Unmarshaller instance returning Maybe.empty for missing / empty values or else Maybe.Just(value)     $nonEmptyStringUnmarshaller
      provide a Maybe[_] Unmarshaller instance allowing optional values                                                             $maybeUnmarshaller
  """

  sealed trait ReadableString
  implicit val arbReadableString: Arbitrary[String @@ ReadableString] = Arbitrary {
    for {
      booleanStr ← Gen.oneOf("true", "TRUE", "false", "false").map(Tag.of[ReadableString](_))
      longStr ← arbitrary[Long].map(i ⇒ Tag.of[ReadableString](i.toString))
      intStr ← arbitrary[Int].map(i ⇒ Tag.of[ReadableString](i.toString))
      nonEmptyString ← arbitrary[String].filter(_.nonEmpty).map(Tag.of[ReadableString](_))
      emptyString ← Gen.oneOf("", " ", "   ").map(Tag.of[ReadableString](_))
      result ← Gen.oneOf(booleanStr, longStr, intStr, nonEmptyString, emptyString)
    } yield result
  }

  private def stringUnmarshaller =
    prop { arbValue: Option[String @@ ReadableString] ⇒
      arbValue.map(Tag.unwrap).toMaybe match {
        case value @ Maybe.Just(str) ⇒ Unmarshaller[String].read(value) mustEqual \/.right(str)
        case value                   ⇒ Unmarshaller[String].read(value) mustEqual \/.left(MissingEnvValue)
      }
    }

  private def booleanUnmarshaller =
    prop { arbValue: Option[String @@ ReadableString] ⇒
      arbValue.map(Tag.unwrap).toMaybe match {
        case value @ Maybe.Just("true")                  ⇒ Unmarshaller[Boolean].read(value) mustEqual \/.right(true)
        case value @ Maybe.Just("TRUE")                  ⇒ Unmarshaller[Boolean].read(value) mustEqual \/.right(true)
        case value @ Maybe.Just("false")                 ⇒ Unmarshaller[Boolean].read(value) mustEqual \/.right(false)
        case value @ Maybe.Just("FALSE")                 ⇒ Unmarshaller[Boolean].read(value) mustEqual \/.right(false)
        case value @ Maybe.Just(str) if str.trim.isEmpty ⇒ Unmarshaller[Boolean].read(value) mustEqual \/.left(MissingEnvValue)
        case value @ Maybe.Empty()                       ⇒ Unmarshaller[Boolean].read(value) mustEqual \/.left(MissingEnvValue)
        case value ⇒ Unmarshaller[Boolean].read(value) must beLike {
          case -\/(InvalidEnvValue(_, "boolean")) ⇒ ok
        }
      }
    }

  private def intUnmarshaller =
    prop { arbValue: Option[String @@ ReadableString] ⇒
      arbValue.map(Tag.unwrap).toMaybe match {
        case value @ Maybe.Just(str) if Try(Integer.valueOf(str)).isSuccess ⇒ Unmarshaller[Int].read(value) mustEqual \/.right(Integer.valueOf(str))
        case value @ Maybe.Just(str) if str.trim.isEmpty ⇒ Unmarshaller[Int].read(value) mustEqual \/.left(MissingEnvValue)
        case value @ Maybe.Empty() ⇒ Unmarshaller[Int].read(value) mustEqual \/.left(MissingEnvValue)
        case value ⇒ Unmarshaller[Int].read(value) must beLike {
          case -\/(InvalidEnvValue(_, "integer")) ⇒ ok
        }
      }
    }

  private def longUnmarshaller =
    prop { arbValue: Option[String @@ ReadableString] ⇒
      arbValue.map(Tag.unwrap).toMaybe match {
        case value @ Maybe.Just(str) if Try(java.lang.Long.valueOf(str)).isSuccess ⇒ Unmarshaller[Long].read(value) mustEqual \/.right(java.lang.Long.valueOf(str))
        case value @ Maybe.Just(str) if str.trim.isEmpty ⇒ Unmarshaller[Long].read(value) mustEqual \/.left(MissingEnvValue)
        case value @ Maybe.Empty() ⇒ Unmarshaller[Long].read(value) mustEqual \/.left(MissingEnvValue)
        case value ⇒ Unmarshaller[Long].read(value) must beLike {
          case -\/(InvalidEnvValue(_, "long")) ⇒ ok
        }
      }
    }

  private def nonEmptyStringUnmarshaller =
    prop { arbValue: Option[String @@ ReadableString] ⇒
      arbValue.map(Tag.unwrap).toMaybe match {
        case value @ Maybe.Just(str) if str.trim.nonEmpty ⇒ implicitly[Unmarshaller[NonEmptyString]].read(value).map(_.value) mustEqual \/.right(str)
        case value                                        ⇒ implicitly[Unmarshaller[NonEmptyString]].read(value) mustEqual \/.left(MissingEnvValue)
      }
    }

  private def maybeUnmarshaller =
    prop { arbValue: Option[String @@ ReadableString] ⇒
      arbValue.map(Tag.unwrap).toMaybe match {
        case value @ Maybe.Just("true")                  ⇒ Unmarshaller[Maybe[Boolean]].read(value) mustEqual \/.right(Maybe.just(true))
        case value @ Maybe.Just("TRUE")                  ⇒ Unmarshaller[Maybe[Boolean]].read(value) mustEqual \/.right(Maybe.just(true))
        case value @ Maybe.Just("false")                 ⇒ Unmarshaller[Maybe[Boolean]].read(value) mustEqual \/.right(Maybe.just(false))
        case value @ Maybe.Just("FALSE")                 ⇒ Unmarshaller[Maybe[Boolean]].read(value) mustEqual \/.right(Maybe.just(false))
        case value @ Maybe.Just(str) if str.trim.isEmpty ⇒ Unmarshaller[Maybe[Boolean]].read(value) mustEqual \/.right(Maybe.empty)
        case value @ Maybe.Empty()                       ⇒ Unmarshaller[Maybe[Boolean]].read(value) mustEqual \/.right(Maybe.empty)
        case value ⇒ Unmarshaller[Maybe[Boolean]].read(value) must beLike {
          case -\/(InvalidEnvValue(_, "boolean")) ⇒ ok
        }
      }
    }

}
