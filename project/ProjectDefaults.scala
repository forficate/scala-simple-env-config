import sbt._
import sbt.Keys._

import Dependencies._

import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import scalariform.formatter.preferences._
import wartremover._

object ProjectDefaults {
  private val scalacOptionsWarnings = Set(
    // "-Xfatal-warnings",
    "-Xlint",
    "-Yno-adapted-args",
    "-Yrangepos",
    "-Ywarn-dead-code",
    "-Ywarn-numeric-widen",
    // "-Ywarn-unused-import",
    "-Ywarn-value-discard"
  )

 lazy val compileScalastyle = taskKey[Unit]("compileScalastyle")

  val settings =
    SbtScalariform.globalSettings ++
    Seq(
      scalaVersion := Dependencies.scalaVersionNumber,
      scalacOptions ++= Seq(
        "-deprecation",
        "-encoding", "UTF-8",
        "-feature",
        "-language:existentials",
        "-language:higherKinds",
        "-language:implicitConversions",
        "-unchecked",
        "-Xfuture",
        "-target:jvm-1.8"
      ) ++
      scalacOptionsWarnings,

    // Disable warnings in console
    scalacOptions in (Compile, console) ~= { _ filterNot scalacOptionsWarnings.apply },
    scalacOptions in (Test, console)    ~= { _ filterNot scalacOptionsWarnings.apply },

    resolvers ++= Seq(
      Resolver.mavenLocal,
      Resolver.typesafeRepo("releases")
    ),

    //Stops the auto creation of java / scala-2.* directories
    unmanagedSourceDirectories in Compile ~= { _.filter(_.exists) },
    unmanagedSourceDirectories in Test ~= { _.filter(_.exists) },

    // Auto run scalastyle on compile
    compileScalastyle := org.scalastyle.sbt.ScalastylePlugin.autoImport.scalastyle.in(Compile).toTask(" q").value, // " q" is the quiet flag
    (compile in Compile) := ((compile in Compile) dependsOn compileScalastyle ).value,
    org.scalastyle.sbt.ScalastylePlugin.autoImport.scalastyleConfig := file("project/scalastyle-config.xml"), // Needed as intelij looks here

    addCompilerPlugin("org.spire-math"  % "kind-projector"  % "0.9.7" cross CrossVersion.binary),

    ScalariformKeys.preferences := ScalariformKeys.preferences.value
      .setPreference(AlignArguments,                    true)
      .setPreference(AlignParameters,                   false)
      .setPreference(AlignSingleLineCaseStatements,     true)
      .setPreference(CompactControlReadability,         true)
      .setPreference(CompactStringConcatenation,        false)
      .setPreference(DanglingCloseParenthesis,          Force)
      .setPreference(DoubleIndentConstructorArguments,  false)
      .setPreference(NewlineAtEndOfFile,                true)
      .setPreference(PreserveSpaceBeforeArguments,      true)
      .setPreference(RewriteArrowSymbols,               false)
      .setPreference(SpaceInsideParentheses,            false)
      .setPreference(SpacesAroundMultiImports,          true),

    wartremoverErrors in (Compile, compile) := Warts.all,
    wartremoverErrors in (Test, compile) := Warts.all
  )

}
