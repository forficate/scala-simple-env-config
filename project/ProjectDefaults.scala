import Dependencies._
import sbt._, Keys._
import com.typesafe.sbt.SbtScalariform, SbtScalariform.autoImport._
import org.scalastyle.sbt.ScalastylePlugin, ScalastylePlugin.autoImport._
import scalariform.formatter.preferences._
import wartremover._

object ProjectDefaults extends AutoPlugin {
  // Set plugin to auto load
  override def trigger = allRequirements

  lazy val compileScalastyle = taskKey[Unit]("compileScalastyle")

  override lazy val projectSettings = {
    val scalacWarnings =
      "-deprecation" ::
        "-feature" ::
        "-unchecked" ::
        "-Ywarn-dead-code" ::
        "-Ywarn-inaccessible" ::
        "-Ywarn-infer-any" ::
        "-Ywarn-nullary-override" ::
        "-Ywarn-nullary-unit" ::
        "-Ywarn-numeric-widen" ::
        "-Ywarn-value-discard" ::
        "-Xfatal-warnings" ::
        "-Xlint:_,-type-parameter-shadow" ::
        Nil

    Seq(
      scalacOptions := Seq(
        "-encoding", "UTF-8",
        "-explaintypes",
        "-Yrangepos",
        "-Xfuture",
        "-Ypartial-unification",
        "-language:higherKinds",
        "-language:existentials",
        "-Yno-adapted-args",
        "-Xsource:2.13",
        "-target:jvm-1.8"
      ) ++ scalacWarnings,

      scalaVersion := Dependencies.scalaVersionNumber,

      // Disable warnings in the console as it becomes unusable
      scalacOptions in (Compile, console) ~= { _ filterNot scalacWarnings.contains },
      scalacOptions in (Test, console)    ~= { _ filterNot scalacWarnings.contains },

      updateOptions := updateOptions.value.withCachedResolution(true),

      resolvers ++= Seq(
        Resolver.mavenLocal,
        Resolver.typesafeRepo("releases")
      ),

      // Prevent java / scala-2.12 directories being created
      Compile / unmanagedSourceDirectories ~= { _.filter(_.exists) },

      addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.7"),

      // Scalastyle configuration in a place compatible with IntelliJ
      scalastyleConfig := file("project/scalastyle-config.xml"),

      // Auto run scalastyle on compile, " q" is the quiet flag
      compileScalastyle := scalastyle.in(Compile).toTask(" q").value,

      // Auto code formatting
      scalariformAutoformat := true,
      scalariformPreferences := SbtScalariform.defaultPreferences
        .setPreference(AlignArguments,                    true)
        .setPreference(AlignParameters,                   false)
        .setPreference(AlignSingleLineCaseStatements,     true)
        .setPreference(CompactControlReadability,         true)
        .setPreference(CompactStringConcatenation,        false)
        .setPreference(DanglingCloseParenthesis,          Force)
        .setPreference(DoubleIndentConstructorArguments,  false)
        .setPreference(NewlineAtEndOfFile,                true)
        .setPreference(PreserveSpaceBeforeArguments,      true)
        .setPreference(RewriteArrowSymbols,               true)
        .setPreference(SpaceInsideParentheses,            false)
        .setPreference(SpacesAroundMultiImports,          true),

      // Default test library for every project
      libraryDependencies ++= Seq(specs2Core, specs2Matchers, specs2ScalaCheck).map(_ % "test"),

      wartremoverErrors := Warts.all
    )
  }




}
