package simpleenvconfig

import org.scalacheck.{ Arbitrary, Gen }
import org.specs2.{ ScalaCheck, Specification }
import org.specs2.matcher.DisjunctionMatchers
import scalaz.{ @@, Tag }
import scalaz.syntax.applicative._
import Arbitrary._

@SuppressWarnings(Array("org.wartremover.warts.Any", "org.wartremover.warts.Nothing"))
class ConfigActionSpec extends Specification with DisjunctionMatchers with ScalaCheck {

  def is = s2"""
    ConfigAction read should unmarshall environment variable values returning failure if value missing else success $read
    ConfigAction should accumulate successfully read values in a report $reportCollectsReadValue
  """

  sealed trait Key
  implicit val arbConfigKey = Arbitrary {
    Gen.oneOf(arbitrary[String], Gen.const(""))
      .map(s ⇒ Tag.of[Key](s.trim))
  }

  private def read =
    prop { (arbKey: String @@ Key, value: Int) ⇒
      val key = Tag.unwrap(arbKey)

      if (key.nonEmpty) {
        val (_, result) = ConfigAction.read[Int](key).disjunctioned(Map(key → value.toString))
        result must be_\/-(value)
      }
      else {
        val (_, result) = ConfigAction.read[Int](key).disjunctioned(Map.empty)
        result must be_-\/.like {
          case errors ⇒
            errors.list.toList must contain(exactly(ConfigError(key, Unmarshaller.Error.MissingEnvValue)))
        }
      }
    }

  private def reportCollectsReadValue = {
    val env = Map("a" -> "1", "b" -> "2", "c" -> "3")

    val (report, _) = (ConfigAction.read[Int]("a") |@|
      ConfigAction.read[Int]("b") |@|
      ConfigAction.read[Int]("c")).tupled.disjunctioned(env)

    report.readValues mustEqual (env)
  }
}
