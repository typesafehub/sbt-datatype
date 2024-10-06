package sbt.contraband

import sbt.{ given, _ }
import Keys._

object JsonCodecPlugin extends AutoPlugin {
  override def requires = ContrabandPlugin
  override def trigger = noTrigger

  import ContrabandPlugin.autoImport._
  override lazy val projectSettings =
    Vector(
      Compile / generateJsonCodecs / skipGeneration := false,
      Test / generateJsonCodecs / skipGeneration := false
    )
}
