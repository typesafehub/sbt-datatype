import sbt.contraband._

lazy val root = (project in file(".")).
  enablePlugins(ContrabandPlugin, JsonCodecPlugin).
  settings(
    name := "example",
    scalaVersion := "2.13.15",
    libraryDependencies += "com.eed3si9n" %% "sjson-new-scalajson" % contrabandSjsonNewVersion.value
    // scalacOptions += "-Xlog-implicits"
  )
