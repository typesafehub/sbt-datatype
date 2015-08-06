package sbt.datatype

import sbt._
import Keys._

object DatatypePlugin extends AutoPlugin {

  private def scalaDef2File(d: Definition) =
    d.namespace map (ns => new File(ns.replace(".", "/"))) map (new File(_, d.name + ".scala")) getOrElse new File(d.name + ".scala")

  object autoImport {
    val generateDatatypes = taskKey[Seq[File]]("Generate datatypes.")
    val datatypeSource = settingKey[File]("Datatype source directory.")
    val datatypeScalaFileNames = settingKey[Definition => File]("Mapping from `Definition` to file for Scala generator.")
    val datatypeScalaSealProtocols = settingKey[Boolean]("Seal abstract classes representing `Protocol`s in Scala.")

    sealed trait DatatypeTargetLang
    object DatatypeTargetLang {
      case object Java extends DatatypeTargetLang
      case object Scala extends DatatypeTargetLang
    }

    lazy val baseDatatypeSettings: Seq[Def.Setting[_]] = Seq(
      datatypeSource in generateDatatypes := sourceDirectory.value / "datatype",
      sourceManaged in generateDatatypes := sourceManaged.value,
      datatypeScalaFileNames in generateDatatypes := scalaDef2File,
      // We cannot enable this by default, because the default function for naming Scala files that we provide
      // will create a separate file for every `Definition`.
      datatypeScalaSealProtocols in generateDatatypes := false,
      sourceGenerators in Compile <+= generateDatatypes,
      generateDatatypes := {
        Generate((datatypeSource in generateDatatypes).value,
          (sourceManaged in generateDatatypes).value,
          (datatypeScalaFileNames in generateDatatypes).value,
          (datatypeScalaSealProtocols in generateDatatypes).value,
          streams.value)
      }
    )
  }

  import autoImport._

  override def requires = sbt.plugins.JvmPlugin
  override def trigger = allRequirements

  override lazy val projectSettings =
    baseDatatypeSettings

}

object Generate {

  private def generate(definitions: Array[File], target: File, scalaFileNames: Definition => File, scalaSealProtocols: Boolean, log: Logger): Seq[File] = {
    val input = definitions map (f => Schema parse (IO read f))

    val generator = new MixedCodeGen(scalaFileNames, scalaSealProtocols)

    input flatMap generator.generate map {
      case (file, code) =>
        val outputFile = new File(target, "/" + file.toString)
        IO.write(outputFile, code)
        log.info(s"sbt-datatype created $outputFile")

        outputFile
    }
  }

  def apply(base: File, target: File, scalaFileNames: Definition => File, scalaSealProtocols: Boolean, s: TaskStreams): Seq[File] = {
    val definitions = IO listFiles base
    def gen() = generate(definitions, target, scalaFileNames, scalaSealProtocols, s.log)
    val f = FileFunction.cached(s.cacheDirectory / "gen-api", FilesInfo.hash) { _ => gen().toSet } // TODO: check if output directory changed
    f(definitions.toSet).toSeq
  }
}
