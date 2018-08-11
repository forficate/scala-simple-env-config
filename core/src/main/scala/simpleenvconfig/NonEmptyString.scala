package simpleenvconfig

import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context
import scalaz.{ Equal, Maybe, Semigroup }

final case class NonEmptyString private (val value: String) {
  def filter(predicate: Char ⇒ Boolean): Maybe[NonEmptyString] =
    NonEmptyString(value.filter(predicate))

  def +(other: String): NonEmptyString =
    copy(value + other)

  def ++(other: NonEmptyString): NonEmptyString =
    copy(value + other.value)
}

object NonEmptyString extends NonEmptyStringInstances {
  def apply(value: String): Maybe[NonEmptyString] =
    Maybe.fromNullable(value)
      .map(_.trim)
      .filter(_.nonEmpty)
      .map(new NonEmptyString(_))

  implicit class NonEmptyStringContext(val sc: StringContext) {
    def nonEmpty(args: String*): NonEmptyString = macro Macro.NonEmptyLiteral
  }

  object Macro {
    def NonEmptyLiteral(c: Context)(args: c.Expr[String]*): c.Expr[NonEmptyString] = {
      import c.universe._ // scalastyle:ignore

      c.prefix.tree match {
        case Apply(_, List(Apply(_, List(t @ Literal(Constant(const: String)))))) ⇒
          NonEmptyString(const).cata(
            _ ⇒ c.Expr[NonEmptyString](q"""_root_.simpleenvconfig.NonEmptyString.unsafeCoerce($const)"""),
            c.abort(t.pos, "Empty string given to nonEmpty, required non empty string.")
          )
      }
    }
  }

  /**
   * Unsafe method of converting String => NonEmptyString, throws if empty.
   *
   * only public so the macro can access it, do not call directly!
   */
  def unsafeCoerce(s: String): NonEmptyString = {
    assert(s.nonEmpty, "NonEmptyString construction attempt from empty String")
    new NonEmptyString(s)
  }

}

trait NonEmptyStringInstances {
  implicit val EqualNonEmptyString: Equal[NonEmptyString] =
    Equal.equalA[NonEmptyString]

  implicit val SemigroupNonEmptyString: Semigroup[NonEmptyString] =
    new Semigroup[NonEmptyString] {
      override def append(a: NonEmptyString, b: ⇒ NonEmptyString): NonEmptyString =
        a ++ b
    }
}
