import Dependencies._

ThisBuild / version := "0.7.0-SNAPSHOT"
ThisBuild / organization := "org.scala-sbt"
ThisBuild / crossScalaVersions := Seq(scala213, scala212, scala3)
ThisBuild / scalaVersion := scala212
ThisBuild / organizationName := "sbt"
ThisBuild / organizationHomepage := Some(url("http://scala-sbt.org/"))
ThisBuild / homepage := Some(url("http://scala-sbt.org/contraband"))
ThisBuild / licenses += ("Apache-2.0", url("https://github.com/sbt/contraband/blob/master/LICENSE"))
ThisBuild / scmInfo := Some(ScmInfo(url("https://github.com/sbt/contraband"), "git@github.com:sbt/contraband.git"))
ThisBuild / developers := List(
  Developer("eed3si9n", "Eugene Yokota", "@eed3si9n", url("https://github.com/eed3si9n")),
  Developer("dwijnand", "Dale Wijnand", "@dwijnand", url("https://github.com/dwijnand")),
  Developer("Duhemm", "Martin Duhem", "@Duhemm", url("https://github.com/Duhemm"))
)
ThisBuild / description := "Contraband is a description language for your datatypes and APIs, currently targeting Java and Scala."
Global / semanticdbEnabled := true
Global / semanticdbVersion := "4.10.1"
ThisBuild / scalacOptions ++= {
  scalaBinaryVersion.value match {
    case "2.12" =>
      Seq("-Xsource:3")
    case "2.13" =>
      Seq("-Xsource:3-cross")
    case _ =>
      Nil
  }
}
ThisBuild / publishMavenStyle := true

lazy val root = (project in file("."))
  .enablePlugins(TravisSitePlugin)
  .aggregate(library, plugin)
  .settings(
    name := "contraband root",
    siteGithubRepo := "sbt/contraband",
    siteEmail := { "eed3si9n" + "@" + "gmail.com" },
    publish / skip := true,
    crossScalaVersions := Nil,
  )

lazy val library = (project in file("library"))
  .enablePlugins(KeywordPlugin, SonatypePublish)
  .settings(
    name := "contraband",
    Compile / unmanagedSourceDirectories += {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, v)) if v <= 12 =>
          baseDirectory.value / "src/main/scala-2.13-"
        case _ =>
          baseDirectory.value / "src/main/scala-2.13+"
      }
    },
    testFrameworks += new TestFramework("verify.runner.Framework"),
    libraryDependencies ++= Seq(parboiled, sjsonNewScalaJson) ++ Seq(verify % Test, scalaTest % Test, diffutils % Test)
  )

lazy val plugin = (project in file("plugin"))
  .enablePlugins(SbtPlugin, SonatypePublish)
  .dependsOn(library)
  .settings(
    name := "sbt-contraband",
    description := "sbt plugin to generate growable datatypes.",
    scriptedLaunchOpts := {
      scriptedLaunchOpts.value ++
        Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
    },
    crossScalaVersions := Seq(scala212, scala3),
    pluginCrossBuild / sbtVersion := {
      scalaBinaryVersion.value match {
        case "2.13" => "1.5.8"
        case "2.12" => "1.5.8" // set minimum sbt version
        case _      => "2.0.0-M3"
      }
    },
    scriptedSbt := {
      scalaBinaryVersion.value match {
        case "2.12" => "1.10.7"
        case _      => "2.0.0-M3"
      }
    },
    publishLocal := (publishLocal dependsOn (library / publishLocal)).value,
  )
