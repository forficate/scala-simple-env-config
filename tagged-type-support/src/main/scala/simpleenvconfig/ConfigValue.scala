package simpleenvconfig

import scalaz.{ @@, Tag }
import scalaz.syntax.std.option._

abstract class ConfigValue[A: Unmarshaller] {
  val EnvValueUnmarshaller: Unmarshaller[Type] =
    implicitly[Unmarshaller[A]].map { apply }

  sealed trait Marker
  final type Type = A @@ Marker

  def apply(a: A): Type =
    Tag[A, Marker](a)

  def unapply(tagged: Type): Option[A] =
    unwrapped(tagged).some

  def unwrapped(tagged: Type): A =
    Tag.unwrap(tagged)
}
