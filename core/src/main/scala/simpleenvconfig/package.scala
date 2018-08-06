import scala.collection.immutable.Map

import scalaz.{ Reader, State }

package object simpleenvconfig {
  type EnvReader[A] = Reader[Env, State[ConfigReport, A]]
  type Env = Map[String, String]
}
