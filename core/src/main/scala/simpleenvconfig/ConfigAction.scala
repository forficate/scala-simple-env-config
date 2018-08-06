package simpleenvconfig

import scala.annotation.tailrec

import scalaz.{ \/, Applicative, NonEmptyList, Reader, State, Validation, ValidationNel }
import scalaz.syntax.nel._
import scalaz.syntax.std.option._

/**
 * Supplies syntactical sugar for testable environment variable
 * configuration parsing with error accumulation using Applicative builder syntax.
 *
 * State is used to capture read values for Config reporting
 */
final case class ConfigAction[A](run: EnvReader[ValidationNel[ConfigError, A]]) {
  def map[B](f: A => B): ConfigAction[B] =
    ConfigAction { run.map { _.map(_.map(f)) } }

  def ensure(onFalse: => ConfigError)(f: A => Boolean): ConfigAction[A] =
    ConfigAction {
      Reader { env: Env =>
        State { read: ConfigReport =>
          (read, run(env).run(read)._2.ensure[NonEmptyList[ConfigError]](onFalse.wrapNel)(f))
        }
      }
    }

  def disjunctioned(env: Map[String, String]): (ConfigReport, NonEmptyList[ConfigError] \/ A) =
    run.map { _.map(_.disjunction) }.run(env).run(ConfigReport.empty)
}

object ConfigAction extends ConfigActionInstances {
  // Safely parse a environment variable to a given type
  def read[A: Unmarshaller](key: String): ConfigAction[A] =
    fromUnmarshaller(key) { Unmarshaller[A] }

  def readWithDefault[A: Unmarshaller](key: String)(default: A): ConfigAction[A] =
    fromUnmarshaller(key) {
      Unmarshaller[A].recover { case Unmarshaller.Error.MissingEnvValue => default }
    }

  def fromUnmarshaller[A](key: String)(unmarshaller: Unmarshaller[A]): ConfigAction[A] =
    ConfigAction {
      Reader { env: Env =>
        State { readValues: ConfigReport =>
          val confValue = env.get(key).toMaybe

          val result = unmarshaller
            .read { confValue }
            .leftMap(ConfigError(key, _))
            .validationNel

          confValue.cata(
            value => (readValues + (key -> value), result),
            (readValues, result)
          )
        }
      }
    }
}

object ConfigReport {
  def empty: ConfigReport =
    ConfigReport(Map.empty[String, String])
}

final case class ConfigReport(readValues: Map[String, String]) {
  def +(other: (String, String)): ConfigReport =
    copy(readValues + other)

  def prettyPrint: String =
    prettyPrintWithMask(_ => false)

  def prettyPrintMaskPasswords: String =
    prettyPrintWithMask(s => List("secret", "password").exists(s.toLowerCase.contains))

  @SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
  def prettyPrintWithMask(maskPredicate: String => Boolean): String = {
    val sb = new StringBuilder
    sb.append("Config:\n")

    @tailrec def loop(item: List[(String, String)]): Unit =
      item match {
        case x :: xs =>
          val value = if (maskPredicate(x._1)) "******" else x._1
          sb.append("  - ").append(x._1).append(": ").append(value)
          if (xs.nonEmpty) sb.append('\n')
          loop(xs)
        case Nil =>
          ()
      }

    loop(readValues.toList.sortBy(_._1))

    sb.toString()
  }
}

trait ConfigActionInstances {
  implicit val applicative: Applicative[ConfigAction] = new Applicative[ConfigAction] {
    override def point[A](a: => A): ConfigAction[A] =
      ConfigAction(Reader((_: Env) => State((_, Validation.success[NonEmptyList[ConfigError], A](a)))))

    override def ap[A, B](c1: => ConfigAction[A])(c2: => ConfigAction[(A) => B]): ConfigAction[B] =
      ConfigAction {
        c1.run.flatMap { s1: State[ConfigReport, ValidationNel[ConfigError, A]] =>
          c2.run.map { s2 =>
            s1.flatMap { v1: ValidationNel[ConfigError, A] =>
              s2.map(v1.ap(_))
            }
          }
        }
      }
  }

}
