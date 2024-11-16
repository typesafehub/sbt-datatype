package sbt.contraband

import java.io.File
import scala.collection.immutable.ListMap
import CodeGen.bq
import ast._
import AstUtil._

/**
 * Code generator to produce a codec for a given type.
 *
 * @param codecParents        The parents that appear in the self type of all codecs, and the full codec inherits from.
 * @param instantiateJavaLazy How to transform an expression to its lazy equivalent in Java.
 * @param formatsForType      Given a `TpeRef` t, returns the list of codecs needed to encode t.
 * @param includedSchemas     List of schemas that could be referenced.
 */
class CodecCodeGen(
    codecParents: List[String],
    instantiateJavaLazy: String => String,
    javaOption: String,
    scalaArray: String,
    formatsForType: ast.Type => List[String],
    includedSchemas: List[Document],
    scalaVersion: String,
) extends CodeGenerator {
  import CodecCodeGen._

  implicit object indentationConfiguration extends IndentationConfiguration {
    override val indentElement = "  "
    override def augmentIndentAfterTrigger(s: String) =
      s.endsWith("{") ||
        (s.contains(" class ") && s.endsWith("(")) // Constructor definition
    override def reduceIndentTrigger(s: String) = s.startsWith("}")
    override def reduceIndentAfterTrigger(s: String) =
      s.endsWith(") {") || s.endsWith("extends Serializable {") // End of constructor definition
    override def enterMultilineJavadoc(s: String) = s == "/**"
    override def exitMultilineJavadoc(s: String) = s == "*/"
  }

  private def intersection: String = ScalaCodeGen.intersection(scalaVersion)
  private def typeClassInstance: String = ScalaCodeGen.typeClassInstance(scalaVersion)

  override def generateEnum(s: Document, e: EnumTypeDefinition): ListMap[File, String] = {
    val fqn = fullyQualifiedName(e)
    // Java enum can have additional parameter such as MERCURY (3.303e+23, 2.4397e6)
    val EnumPattern = """([^\(]+)(\([^\)]*\))?""".r
    def stripParam(s: String): String =
      s match {
        case EnumPattern(x, _) => x.trim
        case _                 => s
      }
    val readerValues =
      e.values map { case EnumValueDefinition(v0, _, _, _) =>
        val v = stripParam(v0)
        s"""case "$v" => $fqn.$v"""
      }
    val writerValues =
      e.values map { case EnumValueDefinition(v0, _, _, _) =>
        val v = stripParam(v0)
        s"""case $fqn.$v => "$v""""
      }
    val selfType = makeSelfType(s, e)

    val code =
      s"""${genPackage(s)}
         |
         |import _root_.sjsonnew.{ Unbuilder, Builder, JsonFormat, deserializationError } 
         |
         |trait ${e.name.capitalize}Formats { $selfType
         |  ${typeClassInstance} ${e.name}Format: JsonFormat[$fqn] = new JsonFormat[$fqn] {
         |    override def read[J](__jsOpt: Option[J], unbuilder: Unbuilder[J]): $fqn = {
         |      __jsOpt match {
         |        case Some(__js) =>
         |          unbuilder.readString(__js) match {
         |            ${readerValues mkString EOL}
         |          }
         |        case None =>
         |          deserializationError("Expected JsString but found None")
         |      }
         |    }
         |
         |    override def write[J](obj: $fqn, builder: Builder[J]): Unit = {
         |      val str = obj match {
         |        ${writerValues mkString EOL}
         |      }
         |      builder.writeString(str)
         |    }
         |  }
         |}""".stripMargin

    ListMap(genFile(s, e) -> code)
  }

  override def generateRecord(s: Document, r: ObjectTypeDefinition): ListMap[File, String] = {
    val parents = r.interfaces
    val parentsInSchema = lookupInterfaces(s, parents)
    val targetLang = toTarget(r.directives) match {
      case Some(x) => x
      case _       => sys.error(s"@target is missing for ${r.name}")
    }
    val intfLanguage = interfaceLanguage(parentsInSchema, targetLang)
    def accessField(f: FieldDefinition) = {
      if (f.fieldType.isLazyType && intfLanguage == "Java") scalaifyType(instantiateJavaLazy(f.name))
      else bq(f.name)
    }
    val fqn = fullyQualifiedName(r)
    val allFields = r.fields // superFields ++ r.fields
    val getFields = allFields map (f =>
      s"""val ${bq(f.name)} = unbuilder.readField[${genRealTpe(f.fieldType, intfLanguage)}]("${f.name}")"""
    ) mkString EOL
    val factoryMethodName = "of"
    val reconstruct =
      if (targetLang == "Scala") s"$fqn(" + allFields.map(accessField).mkString(", ") + ")"
      else s"$fqn.$factoryMethodName(" + allFields.map(accessField).mkString(", ") + ")"
    val writeFields = allFields map (f => s"""builder.addField("${f.name}", obj.${bq(f.name)})""") mkString EOL
    val selfType = makeSelfType(s, r)

    val code =
      s"""${genPackage(s)}
         |
         |import _root_.sjsonnew.{ Unbuilder, Builder, JsonFormat, deserializationError } 
         |
         |trait ${r.name.capitalize}Formats { $selfType
         |  ${typeClassInstance} ${r.name}Format: JsonFormat[$fqn] = new JsonFormat[$fqn] {
         |    override def read[J](__jsOpt: Option[J], unbuilder: Unbuilder[J]): $fqn = {
         |      __jsOpt match {
         |        case Some(__js) =>
         |          unbuilder.beginObject(__js)
         |          $getFields
         |          unbuilder.endObject()
         |          $reconstruct
         |        case None =>
         |          deserializationError("Expected JsObject but found None")
         |      }
         |    }
         |
         |    override def write[J](obj: $fqn, builder: Builder[J]): Unit = {
         |      builder.beginObject()
         |      $writeFields
         |      builder.endObject()
         |    }
         |  }
         |} """.stripMargin

    ListMap(genFile(s, r) -> code)
  }

  override def generateInterface(s: Document, i: InterfaceTypeDefinition): ListMap[File, String] = {
    val name = i.name
    val fqn = fullyQualifiedName(i)
    val children: List[TypeDefinition] = lookupChildLeaves(s, i)
    val code =
      children match {
        case Nil =>
          s"""${genPackage(s)}
             |
             |import _root_.sjsonnew.{ deserializationError, serializationError, Builder, JsonFormat, Unbuilder } 
             |
             |trait ${name.capitalize}Formats {
             |  ${typeClassInstance} ${name}Format: JsonFormat[$fqn] = new JsonFormat[$fqn] {
             |    override def read[J](__jsOpt: Option[J], unbuilder: Unbuilder[J]): $fqn = {
             |      deserializationError("No known implementation of ${i.name}.")
             |    }
             |    override def write[J](obj: $fqn, builder: Builder[J]): Unit = {
             |      serializationError("No known implementation of ${name}.")
             |    }
             |  }
             |}""".stripMargin

        case xs =>
          val fmt = fullFormatsName(s, i)
          val rfs = getAllRequiredFormats(s, i).distinct filter { _ != fmt }
          val selfType = rfs match {
            case Nil => ""
            case fms => fms.mkString("self: ", intersection, " =>")
          }
          val typeFieldName = (toCodecTypeField(i.directives) orElse toCodecTypeField(s)).getOrElse("type")
          val flatUnionFormat =
            s"""flatUnionFormat${xs.length}[$fqn, ${xs map (c => fullyQualifiedName(c)) mkString ", "}]("$typeFieldName")"""
          s"""${genPackage(s)}
             | 
             |import _root_.sjsonnew.JsonFormat 
             |
             |trait ${name.capitalize}Formats { $selfType
             |  ${typeClassInstance} ${name}Format: JsonFormat[$fqn] = $flatUnionFormat
             |}""".stripMargin

      }

    ListMap(genFile(s, i) -> code)
  }

  private def interfaceLanguage(parents: List[InterfaceTypeDefinition], targetLang: String): String =
    if (parents.isEmpty) targetLang
    else {
      if (parents exists { p => toTarget(p.directives) == Some("Java") }) "Java"
      else targetLang
    }

  override def generate(s: Document): ListMap[File, String] = {
    val codecs: ListMap[File, String] = ((s.definitions collect { case td: TypeDefinition =>
      td
    } map { d =>
      ListMap(generate(s, d).toSeq: _*)
    }) reduce (_ merge _)) mapV (_.indented)
    val result = toFullCodec(s) match {
      case Some(x) =>
        val full = generateFullCodec(s, x)
        codecs merge full
      case None =>
        codecs
    }
    result map { case (k, v) => (k, generateHeader + v) }
  }

  protected override def generate(s: Document, d: TypeDefinition): ListMap[File, String] =
    if (!getGenerateCodec(d.directives)) ListMap.empty
    else super.generate(s, d)

  private def genFile(s: Document, d: TypeDefinition): File =
    toCodecPackage(s) match {
      case Some(ns) =>
        val dir = new File(ns.replace(".", "/"))
        new File(dir, s"${d.name}Formats.scala")
      case _ =>
        new File(s"${d.name}Formats.scala")
    }

  private def getAllFormatsForSchema(s: Document): List[String] =
    getAllRequiredFormats(
      s,
      (s.definitions collect {
        case td: TypeDefinition if getGenerateCodec(td.directives) => td
      })
    )

  /**
   * Returns the list of fully qualified codec names that we (transitively) need to generate a codec for `ds`,
   * knowing that it inherits fields `superFields` in the context of schema `s`.
   *
   * The results are sorted topologically.
   */
  private def getAllRequiredFormats(s: Document, ds: List[TypeDefinition]): List[String] = {
    val seedFormats = ds map { d => fullFormatsName(s, d) }
    def getAllDefinitions(d: TypeDefinition): List[TypeDefinition] =
      d match {
        case i: InterfaceTypeDefinition =>
          i :: (lookupChildLeaves(s, i) flatMap { getAllDefinitions })
        case _ => d :: Nil
      }
    val allDefinitions = ds flatMap getAllDefinitions
    val dependencies: Map[String, List[String]] = Map(allDefinitions map { d =>
      val requiredFormats =
        if (ds.contains(d)) getRequiredFormats(s, d)
        else getAllRequiredFormats(s, d :: Nil)
      fullFormatsName(s, d) -> (d match {
        case i: InterfaceTypeDefinition =>
          lookupChildLeaves(s, i).map(c => fullFormatsName(s, c)) ::: requiredFormats
        case _ => requiredFormats
      })
    }: _*)
    val xs = Dag.topologicalSortUnchecked[String](seedFormats) { s => dependencies.get(s).getOrElse(Nil) }
    xs.reverse
  }

  /**
   * Returns the list of fully qualified codec names that we (transitively) need to generate a codec for `d`,
   * knowing that it inherits fields `superFields` in the context of schema `s`.
   *
   * If `d` is an `Interface`, we recurse to get the codecs required by its children.
   */
  private def getAllRequiredFormats(s: Document, d: TypeDefinition): List[String] = d match {
    case i: InterfaceTypeDefinition => getAllRequiredFormats(s, i :: Nil)
    case r: ObjectTypeDefinition    => getRequiredFormats(s, r)
    case e: EnumTypeDefinition      => getRequiredFormats(s, e)
  }

  /**
   * Returns the list of fully qualified codec names that we (non-transitively) need to generate a codec for `d`,
   * knowing that it inherits fields `superFields` in the context of schema `s`.
   */
  private def getRequiredFormats(s: Document, d: TypeDefinition): List[String] = {
    val typeFormats =
      d match {
        case _: EnumTypeDefinition => Nil
        case c: RecordLikeDefinition =>
          c.fields flatMap { f =>
            // if the field type is d, we don't need additional codec
            lookupDefinition(s, f.fieldType.name) match {
              case Some((s1, d1)) if s1 == s && d1 == d => Nil
              case _                                    => lookupFormats(s, f.fieldType)
            }
          }
      }
    typeFormats ++ codecParents
  }

  private def fullyQualifiedName(d: TypeDefinition): String =
    s"""${d.namespace getOrElse "_root_"}.${bq(d.name)}"""

  /**
   * Returns the self type declaration of the codec for `d`, knowing that it inherits fields `superFields`
   * in the context of schema `s`.
   */
  private def makeSelfType(s: Document, d: TypeDefinition): String =
    getRequiredFormats(s, d).distinct match {
      case Nil => ""
      case fms => fms.mkString("self: ", intersection, " =>")
    }

  private def genPackage(s: Document): String =
    toCodecPackage(s) map (p => s"package $p") getOrElse ""

  private def scalaifyType(t: String) = t.replace("<", "[").replace(">", "]")

  private def genRealTpe(tpe: ast.Type, targetLang: String) = {
    val scalaTpe = lookupTpe(scalaifyType(tpe.name))
    tpe match {
      case x if x.isListType && targetLang == "Java"     => s"Array[${scalaTpe}]"
      case x if x.isListType                             => s"$scalaArray[$scalaTpe]"
      case x if !x.isNotNullType && targetLang == "Java" => s"$javaOption[${javaLangBoxedType(scalaTpe)}]"
      case x if !x.isNotNullType                         => s"Option[$scalaTpe]"
      case _                                             => scalaTpe
    }
  }

  private def lookupTpe(tpe: String): String = scalaifyType(tpe) match {
    case "boolean"         => "Boolean"
    case "byte"            => "Byte"
    case "char"            => "Char"
    case "float"           => "Float"
    case "int"             => "Int"
    case "long"            => "Long"
    case "short"           => "Short"
    case "double"          => "Double"
    case "StringStringMap" => "scala.collection.immutable.Map[String, String]"
    case other             => other
  }

  private def generateFullCodec(s: Document, name: String): ListMap[File, String] = {
    val allFormats = getAllFormatsForSchema(s).distinct
    val parents = allFormats.mkString("extends ", EOL + "  with ", "")
    val code =
      s"""${genPackage(s)}
         |trait $name $parents
         |object $name extends $name""".stripMargin
    val syntheticDefinition = InterfaceTypeDefinition(name, None, Nil, Nil, Directive.targetScala :: Nil, Nil, Nil, None)
    ListMap(new File(genFile(s, syntheticDefinition).getParentFile, s"$name.scala") -> code)
  }

  private def lookupFormats(s0: Document, tpe: ast.Type): List[String] =
    tpe match {
      case LazyType(t, _)      => lookupFormats(s0, t)
      case ListType(t, _)      => lookupFormats(s0, t)
      case NotNullType(t, _)   => lookupFormats(s0, t)
      case NamedType(names, _) =>
        // check if this is a locally defined type, which we know the format
        lookupDefinition(s0, tpe.name) match {
          case Some((s, d)) =>
            fullFormatsName(s, d) :: getRequiredFormats(s, d)
          case _ =>
            formatsForType(tpe)
        }
    }

  private def lookupDefinition(s0: Document, fullName: String): Option[(Document, TypeDefinition)] = {
    val (ns, name) = splitName(fullName)
    ((s0 :: includedSchemas) flatMap { s =>
      s.definitions collect {
        case d: TypeDefinition if d.name == name && d.namespace == ns => (s, d)
      }
    }).headOption
  }
}

object CodecCodeGen {
  def fullFormatsName(s: Document, d: TypeDefinition): String =
    s"""${toCodecPackage(s) map (_ + ".") getOrElse ""}${d.name.capitalize}Formats"""

  /**
   * A function that, given a `TpeRef`, returns the list of `JsonFormat`s that are required to encode
   * and decode the given `TpeRef`.
   *
   * A non-primitive types `com.example.Tpe` (except java.io.File) is mapped to `com.example.TpeFormat`.
   */
  val formatsForType: ast.Type => List[String] =
    extensibleFormatsForType { ref =>
      val tpe = ref.removeTypeParameters
      val (ns, name) = splitName(tpe.name)
      s"${ns getOrElse "_root_"}.${name.capitalize}Formats" :: Nil
    }

  private def splitName(fullName: String): (Option[String], String) =
    fullName.split("""\.""").toList.reverse match {
      case List()  => (None, "")
      case List(x) => (None, x)
      case x :: xs => (Some(xs.reverse.mkString(".")), x)
    }

  /**
   * Creates a mapping that maps all primitive types to provided codecs and uses `forOthers`
   * to determine mapping for non-primitive types.
   */
  def extensibleFormatsForType(forOthers: ast.Type => List[String]): ast.Type => List[String] = { tpe =>
    tpe.removeTypeParameters.name match {
      case "boolean" | "byte" | "char" | "float" | "int" | "long" | "short" | "double" | "String" => Nil
      case "Boolean" | "Byte" | "Char" | "Float" | "Int" | "Long" | "Short" | "Double"            => Nil
      case "java.util.UUID" | "java.net.URI" | "java.net.URL" | "java.util.Date" | "java.util.Calendar" | "java.math.BigInteger" |
          "java.math.BigDecimal" | "java.io.File" =>
        Nil
      case "StringStringMap"                   => Nil
      case "Throwable" | "java.lang.Throwable" => Nil
      case _                                   => forOthers(tpe)
    }
  }
}
