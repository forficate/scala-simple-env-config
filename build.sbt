import Dependencies._

Global / version := "0.1.0-SNAPSHOT"

lazy val core =
  (project in file("core"))
    .settings(
      name := "simpleenvconfig-core",
      libraryDependencies ++= Seq(
        scalaReflect,
        scalazCore
      )
    )

lazy val `tagged-type-support` =
  (project in file("tagged-type-support"))
    .dependsOn(core)
    .settings(name := "simpleenvconfig-taggedtypes")
