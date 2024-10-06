package sbt.contraband

import sbt.Keys._
import sbt.{ given, _ }
import sbt.contraband.ast._
import sbt.contraband.parser.{ JsonParser, SchemaParser }

object ContrabandPlugin extends AutoPlugin {

  private def scalaDef2File(x: Any) =
    x match {
      case d: TypeDefinition =>
        d.namespace map (ns => new File(ns.replace(".", "/"))) map (new File(_, d.name + ".scala")) getOrElse new File(d.name + ".scala")
    }

  object autoImport {
    val skipGeneration = settingKey[Boolean]("skip")
    val generateContrabands = taskKey[Seq[File]]("Generate contraband classes.")
    val generateJsonCodecs = taskKey[Seq[File]]("Dummy task for generating JSON codecs.")
    val contrabandCodecsDependencies = settingKey[Seq[ModuleID]]("ModuleIDs of the default codecs.")
    val contrabandJavaLazy = settingKey[String]("Interface to use to provide laziness in Java.")
    val contrabandJavaOption = settingKey[String]("Interface to use to provide options in Java.")
    val contrabandScalaArray = settingKey[String]("Repeated type in Scala.")
    val contrabandSource = settingKey[File]("Contraband source directory.")
    val contrabandScalaFileNames = settingKey[Any => File]("Mapping from `Definition` to file for Scala generator.")
    val contrabandScalaSealInterface = settingKey[Boolean]("Seal abstract classes representing `interface`s in Scala.")
    val contrabandScalaPrivateConstructor = settingKey[Boolean]("Hide the constructors in Scala.")
    val contrabandWrapOption = settingKey[Boolean]("Provide constructors that automatically wraps the options.")
    val contrabandCodecParents = settingKey[List[String]]("Parents to add all o of the codec object.")
    val contrabandInstantiateJavaLazy =
      settingKey[String => String]("Function that instantiate a lazy expression from an expression in Java.")
    val contrabandInstantiateJavaOptional =
      settingKey[(String, String) => String]("Function that instantiate a optional expression from an expression in Java.")
    val contrabandFormatsForType = settingKey[Type => List[String]]("Function that maps types to the list of required codecs for them.")
    val contrabandSjsonNewVersion = settingKey[String]("The version of sjson-new to use")

    sealed trait ContrabandTargetLang
    object ContrabandTargetLang {
      case object Java extends ContrabandTargetLang
      case object Scala extends ContrabandTargetLang
    }

    lazy val baseContrabandSettings: Seq[Def.Setting[_]] = Seq(
      generateContrabands / skipGeneration := false,
      generateJsonCodecs / skipGeneration := true,
      generateContrabands / contrabandCodecsDependencies := Seq("com.eed3si9n" %% "sjson-new-core" % contrabandSjsonNewVersion.value),
      generateContrabands / contrabandJavaLazy := "xsbti.api.Lazy",
      generateContrabands / contrabandJavaOption := "java.util.Optional",
      generateContrabands / contrabandScalaArray := "Vector",
      generateContrabands / contrabandSource := Defaults.configSrcSub(sourceDirectory).value / "contraband",
      generateContrabands / sourceManaged := sourceManaged.value,
      generateContrabands / contrabandScalaFileNames := scalaDef2File,
      // We cannot enable this by default, because the default function for naming Scala files that we provide
      // will create a separate file for every `Definition`.
      generateContrabands / contrabandScalaSealInterface := false,
      generateContrabands / contrabandScalaPrivateConstructor := true,
      generateContrabands / contrabandWrapOption := true,
      generateContrabands / contrabandCodecParents := List("sjsonnew.BasicJsonProtocol"),
      generateContrabands / contrabandInstantiateJavaLazy := { (e: String) => s"xsbti.SafeLazy($e)" },
      generateContrabands / contrabandInstantiateJavaOptional := CodeGen.instantiateJavaOptional,
      generateContrabands / contrabandFormatsForType := CodecCodeGen.formatsForType,
      generateContrabands := {
        Generate(
          (generateContrabands / contrabandSource).value,
          !(generateContrabands / skipGeneration).value,
          !(generateJsonCodecs / skipGeneration).value,
          (generateContrabands / sourceManaged).value,
          (generateContrabands / contrabandJavaLazy).value,
          (generateContrabands / contrabandJavaOption).value,
          (generateContrabands / contrabandScalaArray).value,
          (generateContrabands / contrabandScalaFileNames).value,
          (generateContrabands / contrabandScalaSealInterface).value,
          (generateContrabands / contrabandScalaPrivateConstructor).value,
          (generateContrabands / scalaVersion).value,
          (generateContrabands / contrabandWrapOption).value,
          (generateContrabands / contrabandCodecParents).value,
          (generateContrabands / contrabandInstantiateJavaLazy).value,
          (generateContrabands / contrabandInstantiateJavaOptional).value,
          (generateContrabands / contrabandFormatsForType).value,
          streams.value
        )
      },
      Compile / sourceGenerators += generateContrabands.taskValue
    )
  }

  import autoImport._

  override def requires = sbt.plugins.JvmPlugin
  override def trigger = noTrigger

  override lazy val projectSettings =
    inConfig(Compile)(baseContrabandSettings) ++ inConfig(Test)(baseContrabandSettings) ++ Seq(
      libraryDependencies ++= {
        val addInCompile = !(Compile / generateJsonCodecs / skipGeneration).value
        val addInTest = !addInCompile && !(Test / generateJsonCodecs / skipGeneration).value

        val inCompile =
          if (addInCompile) (Compile / generateContrabands / contrabandCodecsDependencies).value
          else Seq.empty

        val inTest =
          if (addInTest) (Test / generateContrabands / contrabandCodecsDependencies).value map (_ % Test)
          else Seq.empty

        inCompile ++ inTest
      }
    )

  override def globalSettings = Seq(
    contrabandSjsonNewVersion := {
      scalaBinaryVersion.value match {
        case "3" =>
          "0.13.1"
        case _ =>
          "0.9.0"
      }
    }
  )
}

object Generate {
  private def generate(
      createDatatypes: Boolean,
      createCodecs: Boolean,
      definitions: Array[File],
      target: File,
      javaLazy: String,
      javaOption: String,
      scalaArray: String,
      scalaFileNames: Any => File,
      scalaSealInterface: Boolean,
      scalaPrivateConstructor: Boolean,
      wrapOption: Boolean,
      codecParents: List[String],
      instantiateJavaLazy: String => String,
      instantiateJavaOptional: (String, String) => String,
      formatsForType: Type => List[String],
      log: Logger
  ): Seq[File] = {
    val jsonFiles = definitions.toList collect {
      case f: File if f.getName endsWith ".json" => f
    }
    val contraFiles = definitions.toList collect {
      case f: File if (f.getName endsWith ".contra") || (f.getName endsWith ".gql") => f
    }
    val input =
      (jsonFiles map { f => JsonParser.Document.parse(IO read f) }) ++
        (contraFiles map { f =>
          val ast = SchemaParser.parse(IO read f).get
          ast
        })
    val generator = new MixedCodeGen(
      javaLazy,
      javaOption,
      instantiateJavaOptional,
      scalaArray,
      scalaFileNames,
      scalaSealInterface,
      scalaPrivateConstructor,
      wrapOption
    )
    val jsonFormatsGenerator = new CodecCodeGen(codecParents, instantiateJavaLazy, javaOption, scalaArray, formatsForType, input)

    val datatypes =
      if (createDatatypes) {
        input flatMap { s =>
          generator
            .generate(s)
            .map { case (file, code) =>
              val outputFile = new File(target, "/" + file.toString)
              IO.write(outputFile, code)
              log.debug(s"sbt-contraband created $outputFile")
              // println(code)
              // println("---------")
              outputFile
            }
            .toList
        }
      } else {
        List.empty
      }

    val formats =
      if (createCodecs) {
        input flatMap { s =>
          jsonFormatsGenerator
            .generate(s)
            .map { case (file, code) =>
              // println(code)
              // println("---------")
              val outputFile = new File(target, "/" + file.toString)
              IO.write(outputFile, code)
              log.debug(s"sbt-contraband created $outputFile")

              outputFile
            }
            .toList
        }
      } else {
        List.empty
      }
    datatypes ++ formats
  }

  def apply(
      base: File,
      createDatatypes: Boolean,
      createCodecs: Boolean,
      target: File,
      javaLazy: String,
      javaOption: String,
      scalaArray: String,
      scalaFileNames: Any => File,
      scalaSealInterface: Boolean,
      scalaPrivateConstructor: Boolean,
      scalaVersion: String,
      wrapOption: Boolean,
      codecParents: List[String],
      instantiateJavaLazy: String => String,
      instantiateJavaOptional: (String, String) => String,
      formatsForType: Type => List[String],
      s: TaskStreams
  ): Seq[File] = {
    val definitions = IO listFiles base
    def gen() = generate(
      createDatatypes,
      createCodecs,
      definitions,
      target,
      javaLazy,
      javaOption,
      scalaArray,
      scalaFileNames,
      scalaSealInterface,
      scalaPrivateConstructor,
      wrapOption,
      codecParents,
      instantiateJavaLazy,
      instantiateJavaOptional,
      formatsForType,
      s.log
    )

    val scalaVersionSubDir = scalaVersion match {
      case VersionNumber(Seq(x, y, _*), _, _) => s"scala-$x.$y"
      case _                                  => throw new IllegalArgumentException(s"Invalid Scala version: '$scalaVersion'")
    }
    val cacheDirectory = s.cacheDirectory / scalaVersionSubDir / "gen-api"

    val f = FileFunction.cached(cacheDirectory, FilesInfo.hash) { _ => gen().toSet } // TODO: check if output directory changed
    f(definitions.toSet).toSeq
  }
}
