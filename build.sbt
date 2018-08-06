import Dependencies._

val projectName = "simple-env-config"

scalaVersion := Dependencies.scalaVersionNumber

lazy val core =
  (project in file("core"))
    .settings(
      libraryDependencies ++= Seq(
        scalaReflect,
        scalazCore
      )
    )

lazy val taggedTypeSupport =
  (project in file("tagged-type-support"))
    .dependsOn(core)
