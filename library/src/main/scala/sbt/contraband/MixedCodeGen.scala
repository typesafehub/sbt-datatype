package sbt.contraband

import java.io.File
import scala.collection.immutable.ListMap
import ast._
import AstUtil._

/**
 * Generator that produces both Scala and Java code.
 */
class MixedCodeGen(
    javaLazy: String,
    javaOptional: String,
    instantiateJavaOptional: (String, String) => String,
    scalaArray: String,
    genScalaFileName: Any => File,
    scalaSealProtocols: Boolean,
    scalaPrivateConstructor: Boolean,
    wrapOption: Boolean,
    scalaVersion: String,
) extends CodeGenerator {
  val javaGen = new JavaCodeGen(javaLazy, javaOptional, instantiateJavaOptional, wrapOption)
  val scalaGen = new ScalaCodeGen(
    javaLazy,
    javaOptional,
    instantiateJavaOptional,
    scalaArray,
    genScalaFileName,
    scalaSealProtocols,
    scalaPrivateConstructor,
    wrapOption,
    scalaVersion,
  )

  def generate(s: Document): ListMap[File, String] =
    s.definitions collect { case td: TypeDefinition =>
      td
    } map (generate(s, _)) reduce (_ merge _) map { case (k, v) =>
      (k, generateHeader + v)
    }

  def generateInterface(s: Document, i: InterfaceTypeDefinition): ListMap[File, String] = {
    // We generate the code that corresponds to this protocol, but without its children, because they
    // may target another language. In this case, we have to make sure that we won't generate them
    // in the wrong language.
    val childLessProtocol = i
    val targetLang = toTarget(i.directives)
    val parentResult =
      targetLang match {
        case Some("Scala") => scalaGen.generateInterface(s, childLessProtocol) mapV (_ indentWith scalaGen.indentationConfiguration)
        case Some("Java")  => javaGen.generateInterface(s, childLessProtocol) mapV (_ indentWith javaGen.indentationConfiguration)
        case Some(x)       => sys.error(s"unknown target language $x!")
        case None          => sys.error(s"target language was not specified for ${i.name}!")
      }
    parentResult
  }

  def generateRecord(s: Document, r: ObjectTypeDefinition): ListMap[File, String] = {
    toTarget(r.directives) match {
      case Some("Java") => javaGen.generateRecord(s, r) mapV (_ indentWith javaGen.indentationConfiguration)
      case _            => scalaGen.generateRecord(s, r) mapV (_ indentWith scalaGen.indentationConfiguration)
    }
  }

  def generateEnum(s: Document, e: EnumTypeDefinition): ListMap[File, String] =
    toTarget(e.directives) match {
      case Some("Java") => javaGen.generateEnum(s, e) mapV (_ indentWith javaGen.indentationConfiguration)
      case _            => scalaGen.generateEnum(s, e) mapV (_ indentWith scalaGen.indentationConfiguration)
    }
}
