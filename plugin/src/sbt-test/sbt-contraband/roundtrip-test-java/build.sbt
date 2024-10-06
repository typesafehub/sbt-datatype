import sbt.contraband._

lazy val root = (project in file(".")).
  enablePlugins(ContrabandPlugin, JsonCodecPlugin).
  settings(
    name := "example",
    scalaVersion := "2.13.15",
    Compile / generateContrabands / contrabandFormatsForType := { tpe =>
      val substitutions = Map("java.io.File" -> "com.foo.FileFormats")
      val name = tpe.removeTypeParameters.name
      if (substitutions contains name) substitutions(name) :: Nil
      else ((Compile / generateContrabands / contrabandFormatsForType).value)(tpe)
    },
    TaskKey[Unit]("check") := {
      val dir = (Compile / generateContrabands / sourceManaged).value
      val src = dir / "generated" / "CustomProtocol.scala"
      assert(src.isFile)
    },
    libraryDependencies += "com.eed3si9n" %% "sjson-new-scalajson" % contrabandSjsonNewVersion.value
    // scalacOptions += "-Xlog-implicits"
  )
