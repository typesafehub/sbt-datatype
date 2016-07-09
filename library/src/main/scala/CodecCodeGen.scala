package sbt.datatype
import scala.compat.Platform.EOL
import java.io.File

/**
 * Code generator to produce a codec for a given type.
 *
 * @param genFile             A function that maps a `Definition` to the `File` in which we should write it.
 * @param protocolName        The name of the full codec object to generate.
 * @param codecNamespace      The package to which the full codec object should belong.
 * @param codecParents        The parents that appear in the self type of all codecs, and the full codec inherits from.
 * @param instantiateJavaLazy How to transform an expression to its lazy equivalent in Java.
 * @param formatsForType      Given a `TpeRef` t, returns the list of codecs needed to encode t.
 */
class CodecCodeGen(genFile: Definition => File,
  protocolName: Option[String],
  codecNamespace: Option[String],
  codecParents: List[String],
  instantiateJavaLazy: String => String,
  formatsForType: TpeRef => List[String]) extends CodeGenerator {

  implicit object indentationConfiguration extends IndentationConfiguration {
    override val indentElement = "  "
    override def augmentIndentAfterTrigger(s: String) =
      s.endsWith("{") ||
      (s.contains(" class ") && s.endsWith("(")) // Constructor definition
    override def reduceIndentTrigger(s: String) = s.startsWith("}")
    override def reduceIndentAfterTrigger(s: String) = s.endsWith(") {") || s.endsWith("extends Serializable {") // End of constructor definition
    override def enterMultilineJavadoc(s: String) = s == "/**"
    override def exitMultilineJavadoc(s: String) = s == "*/"
  }

  override def generate(s: Schema, e: Enumeration): Map[File, String] = {
    val readerValues = e.values map { case EnumerationValue(v, _) => s"""case "$v" => ${e.name}.$v""" }
    val writerValues = e.values map { case EnumerationValue(v, _) => s"""case ${e.name}.$v => "$v"""" }
    val selfType = makeSelfType(s, e, Nil)

    val code =
      s"""${genPackage(e)}
         |$sjsonImports
         |trait ${e.name.capitalize}Formats { $selfType
         |  implicit lazy val ${e.name}Format: JsonFormat[${e.name}] = new JsonFormat[${e.name}] {
         |    override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): ${e.name} = {
         |      jsOpt match {
         |        case Some(js) =>
         |          unbuilder.readString(js) match {
         |            ${readerValues mkString EOL}
         |          }
         |        case None =>
         |          deserializationError("Expected JsString but found None")
         |      }
         |    }
         |
         |    override def write[J](obj: ${e.name}, builder: Builder[J]): Unit = {
         |      val str = obj match {
         |        ${writerValues mkString EOL}
         |      }
         |      builder.writeString(str)
         |    }
         |  }
         |}""".stripMargin

    Map(genFile(e) -> code)
  }

  override def generate(s: Schema, r: Record, parent: Option[Interface], superFields: List[Field]): Map[File, String] = {
    def accessField(f: Field) = {
      if (f.tpe.lzy && r.targetLang == "Java") scalaifyType(instantiateJavaLazy(f.name))
      else f.name
    }
    val allFields = r.fields ++ superFields
    val getFields = allFields map (f => s"""val ${f.name} = unbuilder.readField[${genRealTpe(f.tpe)}]("${f.name}")""") mkString EOL
    val reconstruct = s"new ${r.name}(" + allFields.map(accessField).mkString(", ") + ")"
    val writeFields = allFields map (f => s"""builder.addField("${f.name}", obj.${f.name})""") mkString EOL
    val selfType = makeSelfType(s, r, superFields)

    val code =
      s"""${genPackage(r)}
         |$sjsonImports
         |trait ${r.name.capitalize}Formats { $selfType
         |  implicit lazy val ${r.name}Format: JsonFormat[${r.name}] = new JsonFormat[${r.name}] {
         |    override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): ${r.name} = {
         |      jsOpt match {
         |        case Some(js) =>
         |          unbuilder.beginObject(js)
         |          $getFields
         |          unbuilder.endObject()
         |          $reconstruct
         |        case None =>
         |          deserializationError("Expected JsObject but found None")
         |      }
         |    }
         |
         |    override def write[J](obj: ${r.name}, builder: Builder[J]): Unit = {
         |      builder.beginObject()
         |      $writeFields
         |      builder.endObject()
         |    }
         |  }
         |} """.stripMargin

    Map(genFile(r) -> code)
  }

  override def generate(s: Schema, i: Interface, parent: Option[Interface], superFields: List[Field]): Map[File, String] = {
    val name = i.name
    val code =
      i.children match {
        case Nil =>
          s"""${genPackage(i)}
             |$sjsonImports
             |trait ${name.capitalize}Formats {
             |  implicit lazy val ${name}Format: JsonFormat[${name}] = new JsonFormat[${name}] {
             |    override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): ${i.name} = {
             |      deserializationError("No known implementation of ${i.name}.")
             |    }
             |    override def write[J](obj: ${name}, builder: Builder[J]): Unit = {
             |      serializationError("No known implementation of ${name}.")
             |    }
             |  }
             |}""".stripMargin

        case xs =>
          val unionFormat = s"unionFormat${xs.length}[${name}, ${xs map (c => c.namespace.getOrElse("_root_") + "." + c.name) mkString ", "}]"

          val selfType = getAllRequiredFormats(s, i, superFields).distinct match {
            case Nil => ""
            case fms => fms.mkString("self: ", " with ", " =>")
          }
          s"""${genPackage(i)}
             |$sjsonImports
             |trait ${name.capitalize}Formats { $selfType
             |  implicit lazy val ${name}Format: JsonFormat[${name}] = $unionFormat
             |}""".stripMargin

      }

    Map(genFile(i) -> code) :: (i.children map (generate(s, _, Some(i), i.fields ++ superFields))) reduce (_ merge _)
  }

  override def generate(s: Schema): Map[File, String] = {
    val codecs = s.definitions map (generate (s, _, None, Nil)) reduce (_ merge _) mapValues (_.indented)
    protocolName match {
      case Some(x) =>
        val fullProtocol = generateFullProtocol(s, x)
        codecs merge fullProtocol
      case None =>
        codecs
    }
  }

  /**
   * Returns the list of fully qualified codec names that we (non-transitively) need to generate a codec for `d`,
   * knowing that it inherits fields `superFields` in the context of schema `s`.
   */
  private def getRequiredFormats(s: Schema, d: Definition, superFields: List[Field]): List[String] = {
    val typeFormats =
      d match {
        case Interface(name, _, namespace, _, _, fields, _, _) =>
          val allFields = fields ++ superFields
          allFields flatMap (f => formatsForType(f.tpe))

        case Record(_, _, _, _, _, fields) =>
          val allFields = fields ++ superFields
          allFields flatMap (f => formatsForType(f.tpe))

        case _: Enumeration =>
          "sjsonnew.BasicJsonProtocol" :: Nil
      }

    val unionFormat = if (requiresUnionFormats(s, d, superFields)) "sjsonnew.BasicJsonProtocol" :: Nil else Nil

    typeFormats ++ unionFormat ++ codecParents
  }

  private def fullFormatsName(d: Definition): String =
    s"""${d.namespace getOrElse "_root_"}.${d.name.capitalize}Formats"""

  private def getAllRequiredFormats(s: Schema): List[String] = getAllRequiredFormats(s, s.definitions, Nil)

  /**
   * Returns the list of fully qualified codec names that we (transitively) need to generate a codec for `ds`,
   * knowing that it inherits fields `superFields` in the context of schema `s`.
   *
   * The results are sorted topologically.
   */
  private def getAllRequiredFormats(s: Schema, ds: List[Definition], superFields: List[Field]): List[String] = {
    val seedFormats = ds map { fullFormatsName }
    def getAllDefinitions(d: Definition): List[Definition] =
      d match {
        case i: Interface => i :: (i.children flatMap {getAllDefinitions})
        case _            => d :: Nil
      }
    val allDefinitions = ds flatMap getAllDefinitions
    val dependencies: Map[String, List[String]] = Map(allDefinitions map { d =>
      val tpe = TpeRef((d.namespace.map(_ + ".").getOrElse("")) + d.name, false, false)
      fullFormatsName(d) -> (d match {
        case i: Interface =>
          i.children.map(fullFormatsName) :::
          "sjsonnew.BasicJsonProtocol" :: getRequiredFormats(s, d, superFields)
        case _            => "sjsonnew.BasicJsonProtocol" :: getRequiredFormats(s, d, superFields)
      })
    }: _*)
    val xs = sbt.Dag.topologicalSort[String](seedFormats) { s => dependencies.get(s).getOrElse(Nil) }
    xs.reverse
  }

  /**
   * Returns the list of fully qualified codec names that we (transitively) need to generate a codec for `d`,
   * knowing that it inherits fields `superFields` in the context of schema `s`.
   *
   * If `d` is an `Interface`, we recurse to get the codecs required by its children.
   */
  private def getAllRequiredFormats(s: Schema, d: Definition, superFields: List[Field]): List[String] = d match {
    case i: Interface =>
      val fmt = fullFormatsName(d)
      getAllRequiredFormats(s, i :: Nil, superFields) filter { _ != fmt }
    case r: Record =>
      getRequiredFormats(s, r, superFields)
    case e: Enumeration =>
      getRequiredFormats(s, e, Nil)
  }

  /**
   * Returns the self type declaration of the codec for `d`, knowing that it inherits fields `superFields`
   * in the context of schema `s`.
   */
  private def makeSelfType(s: Schema, d: Definition, superFields: List[Field]): String =
    getRequiredFormats(s, d, superFields).distinct match {
      case Nil => ""
      case fms => fms.mkString("self: ", " with ", " =>")
    }

  private def genPackage(d: Definition): String =
    d.namespace map (ns => s"package $ns") getOrElse ""

  private val sjsonImports: String = "import _root_.sjsonnew.{ deserializationError, serializationError, Builder, JsonFormat, Unbuilder }"

  private def scalaifyType(t: String) = t.replace("<", "[").replace(">", "]")

  private def genRealTpe(tpe: TpeRef) = {
    val scalaTpe = lookupTpe(scalaifyType(tpe.name))
    if (tpe.repeated) s"Array[$scalaTpe]" else scalaTpe
  }

  private def lookupTpe(tpe: String): String = scalaifyType(tpe) match {
    case "boolean" => "Boolean"
    case "byte"    => "Byte"
    case "char"    => "Char"
    case "float"   => "Float"
    case "int"     => "Int"
    case "long"    => "Long"
    case "short"   => "Short"
    case "double"  => "Double"
    case other     => other
  }

  private def generateFullProtocol(s: Schema, name: String): Map[File, String] = {
    val allFormats = getAllRequiredFormats(s).distinct
    val parents = allFormats.mkString("extends ", " with ", "")
    val code =
      s"""${codecNamespace map (p => s"package $p") getOrElse ""}
         |trait $name $parents
         |object $name extends $name""".stripMargin

    val syntheticDefinition = Interface(name, "Scala", codecNamespace, VersionNumber("0.0.0"), Nil, Nil, Nil, Nil)

    Map(genFile(syntheticDefinition) -> code)
  }

  private def allChildrenOf(d: Definition): List[Definition] = d match {
    case i: Interface => i :: i.children.flatMap(allChildrenOf)
    case r: Record => r :: Nil
    case e: Enumeration => e :: Nil
  }

  private def definitionsMap(s: Schema): Map[String, Definition] = {
    val allDefinitions = s.definitions flatMap allChildrenOf
    allDefinitions.map(d => d.namespace.map(_ + ".").getOrElse("") + d.name -> d).toMap
  }

  private def requiresUnionFormats(s: Schema, d: Definition, superFields: List[Field]): Boolean = d match {
    case _: Interface => true
    case r: Record =>
      val defsMap = definitionsMap(s)
      val allFields = r.fields ++ superFields
      allFields exists { f => defsMap get f.tpe.name exists { case _: Interface => true ; case _ => false } }
    case _: Enumeration => false
  }


}

object CodecCodeGen {
  /** Removes all type parameters from `tpe` */
  def removeTypeParameters(tpe: TpeRef): TpeRef = tpe.copy(name = removeTypeParameters(tpe.name))

  /** Removes all type parameters from `tpe` */
  def removeTypeParameters(tpe: String): String = tpe.replaceAll("<.+>", "").replaceAll("\\[.+\\]", "")

  /**
   * A function that, given a `TpeRef`, returns the list of `JsonFormat`s that are required to encode
   * and decode the given `TpeRef`.
   *
   * A non-primitive types `com.example.Tpe` (except java.io.File) is mapped to `com.example.TpeFormat`.
   */
  val formatsForType: TpeRef => List[String] =
    extensibleFormatsForType {
      removeTypeParameters(_) match {
        case TpeRef(name, _, _) if name contains "." =>
          val tokens: List[String] = name.split("""\.""").toList.reverse
          val cap = tokens match {
            case x :: xs => (x.capitalize :: xs).reverse.mkString(".")
            case x => x.mkString(".")
          }
          val xs = s"${cap}Formats" :: Nil
          xs
        case TpeRef(name, _, _) => s"_root_.${name.capitalize}Formats" :: Nil
      }
    }

  /**
   * Creates a mapping that maps all primitive types to provided codecs and uses `forOthers`
   * to determine mapping for non-primitive types.
   */
  def extensibleFormatsForType(forOthers: TpeRef => List[String]): TpeRef => List[String] = { tpe =>
    val basicJsonProtcol = "sjsonnew.BasicJsonProtocol"
    removeTypeParameters(tpe).name match {
      case "boolean" | "byte" | "char" | "float" | "int" | "long" | "short" | "double" | "String" => basicJsonProtcol :: Nil
      case "java.util.UUID" | "java.net.URI" | "java.net.URL" => basicJsonProtcol :: Nil
      case _ => forOthers(tpe) ++ (basicJsonProtcol :: Nil)
    }
  }
}
