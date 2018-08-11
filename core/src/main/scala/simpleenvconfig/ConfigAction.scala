package simpleenvconfig

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
  def map[B](f: A ⇒ B): ConfigAction[B] =
    ConfigAction { run.map { _.map(_.map(f)) } }

  def ensure(onFalse: ⇒ ConfigError)(f: A ⇒ Boolean): ConfigAction[A] =
    ConfigAction {
      Reader { env: Env ⇒
        State { read: ConfigReport ⇒
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
      Unmarshaller[A].recover { case Unmarshaller.Error.MissingEnvValue ⇒ default }
    }

  def fromUnmarshaller[A](key: String)(unmarshaller: Unmarshaller[A]): ConfigAction[A] =
    ConfigAction {
      Reader { env: Env ⇒
        State { readValues: ConfigReport ⇒
          val confValue = env.get(key).toMaybe

          val result = unmarshaller
            .read { confValue }
            .leftMap(ConfigError(key, _))
            .validationNel

          confValue.cata(
            value ⇒ (readValues + (key -> value), result),
            (readValues, result)
          )
        }
      }
    }
}

trait ConfigActionInstances {
  implicit val applicative: Applicative[ConfigAction] = new Applicative[ConfigAction] {
    override def point[A](a: ⇒ A): ConfigAction[A] =
      ConfigAction(Reader((_: Env) ⇒ State((_, Validation.success[NonEmptyList[ConfigError], A](a)))))

    override def ap[A, B](c1: ⇒ ConfigAction[A])(c2: ⇒ ConfigAction[(A) ⇒ B]): ConfigAction[B] =
      ConfigAction {
        c1.run.flatMap { s1: State[ConfigReport, ValidationNel[ConfigError, A]] ⇒
          c2.run.map { s2 ⇒
            s1.flatMap { v1: ValidationNel[ConfigError, A] ⇒
              s2.map(v1.ap(_))
            }
          }
        }
      }
  }

}
