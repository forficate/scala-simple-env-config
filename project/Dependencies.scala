import sbt._

object Dependencies {
  private object Version {
    val scalaz           = "7.2.25"
    val shapeless        = "2.3.2"
    val specs2           = "4.0.0"
  }

  val scalaVersionNumber       = "2.12.6"

  val scalaReflect             = "org.scala-lang"            % "scala-reflect"             % scalaVersionNumber
  val scalazConcurrent         = "org.scalaz"               %% "scalaz-concurrent"         % Version.scalaz
  val scalazCore               = "org.scalaz"               %% "scalaz-core"               % Version.scalaz
  val scalazEffect             = "org.scalaz"               %% "scalaz-effect"             % Version.scalaz
  val scalazScalacheckBinding  = "org.scalaz"               %% "scalaz-scalacheck-binding" % Version.scalaz
  val specs2Core               = "org.specs2"               %% "specs2-core"               % Version.specs2
  val specs2Matchers           = "org.specs2"               %% "specs2-matcher-extra"      % Version.specs2
  val specs2ScalaCheck         = "org.specs2"               %% "specs2-scalacheck"         % Version.specs2
  val specs2Scalaz             = "org.specs2"               %% "specs2-scalaz"             % Version.specs2
}
