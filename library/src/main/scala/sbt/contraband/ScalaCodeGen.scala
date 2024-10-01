package sbt.contraband

import java.io.File
import scala.collection.immutable.ListMap
import CodeGen.bq
import ast.{ Definition => _, _ }
import AstUtil._

/**
 * Code generator for Scala.
 */
class ScalaCodeGen(
    javaLazy: String,
    javaOptional: String,
    instantiateJavaOptional: (String, String) => String,
    scalaArray: String,
    genFile: Any => File,
    scalaSealProtocols: Boolean,
    scalaPrivateConstructor: Boolean,
    wrapOption: Boolean
) extends CodeGenerator {

  implicit object indentationConfiguration extends IndentationConfiguration {
    override val indentElement = "  "
    override def augmentIndentAfterTrigger(s: String) =
      s.endsWith("{") ||
        (s.contains(" class ") && s.endsWith("(")) // Constructor definition
    override def reduceIndentTrigger(s: String) = s.startsWith("}")
    override def reduceIndentAfterTrigger(s: String) = s.endsWith(") {") || s.endsWith(" Serializable {") // End of constructor definition
    override def enterMultilineJavadoc(s: String) = s == "/**"
    override def exitMultilineJavadoc(s: String) = s == "*/"
  }

  override def generate(s: Document): ListMap[File, String] =
    (s.definitions collect { case td: TypeDefinition =>
      td
    }) map (generate(s, _)) reduce (_ merge _) mapV (_.indented)

  override def generateEnum(s: Document, e: EnumTypeDefinition): ListMap[File, String] = {
    val values =
      e.values map { case (EnumValueDefinition(name, _, comments, _)) =>
        s"""${genDoc(toDoc(comments))}
           |case object $name extends ${e.name}""".stripMargin
      } mkString EOL

    val extra: List[String] = toExtra(e)
    val code =
      s"""${genPackage(e)}
         |${genDoc(toDoc(e.comments))}
         |sealed abstract class ${e.name} extends Serializable
         |object ${e.name} {
         |  ${extra mkString EOL}
         |  $values
         |}""".stripMargin

    ListMap(genFile(e) -> code)
  }

  override def generateRecord(s: Document, r: ObjectTypeDefinition): ListMap[File, String] = {
    // println(s"generateRecord: ${r.name}")
    val allFields = r.fields filter { _.arguments.isEmpty }
    val extraParents = toExtraIntf(r)
    val parents = r.interfaces
    val parentsInSchema = lookupInterfaces(s, parents)
    val parent: Option[InterfaceTypeDefinition] = parentsInSchema.headOption
    val intfLang = interfaceLanguage(parentsInSchema)
    // println(s"parents: $parents,  parent: $parent")
    val ctorParameters = genCtorParameters(r, parent, intfLang) mkString ","
    val superFields = (parent map { _.fields }).toList.flatten
    val superCtorArguments = superFields map (_.name) mkString ", "
    val extendsCode = genExtendsCode(parent, extraParents, superCtorArguments)
    val extendsCodeCompanion = genExtendsCodeCompanion(toCompanionExtraIntfComment(r))
    val companionExtra: List[String] = toCompanionExtra(r)
    val toStringImpl: List[String] = toToStringImpl(r)
    val lazyMembers = genLazyMembers(localFields(r, parentsInSchema), intfLang) mkString EOL
    val privateCtr = if (scalaPrivateConstructor) " private " else ""

    val argsDoc: List[String] = allFields flatMap { (a: FieldDefinition) =>
      toDoc(a.comments) match {
        case Nil        => Nil
        case doc :: Nil => s"@param ${a.name} $doc" :: Nil
        case docs =>
          val prefix = s"@param ${a.name} "
          docs.mkString(prefix, EOL + " " * (prefix.length + 3), "") :: Nil
      }
    }
    val doc = toDoc(r.comments)
    val extra = toExtra(r)
    val since = getSince(r.directives)
    val mod = toModifier(r.directives).getOrElse("final")
    val code =
      s"""${genPackage(r)}
         |${genDoc(doc ++ argsDoc)}
         |$mod class ${r.name}$privateCtr($ctorParameters) $extendsCode {
         |  ${extra mkString EOL}
         |  ${genAlternativeConstructors(since, allFields, scalaPrivateConstructor, intfLang) mkString EOL}
         |  ${lazyMembers}
         |  ${genEquals(r, intfLang)}
         |  ${genHashCode(r, intfLang)}
         |  ${genToString(r, toStringImpl)}
         |  ${genCopy(r, intfLang)}
         |  ${genWith(r, intfLang)}
         |}
         |
         |object ${r.name}$extendsCodeCompanion {
         |  ${companionExtra mkString EOL}
         |  ${genApplyOverloads(r, allFields, intfLang) mkString EOL}
         |}""".stripMargin

    ListMap(genFile(r) -> code)
  }

  override def generateInterface(s: Document, i: InterfaceTypeDefinition): ListMap[File, String] = {
    val extraParents = toExtraIntf(i)
    val parents = i.interfaces
    val parentsInSchema = lookupInterfaces(s, parents)
    val intfLang = interfaceLanguage(parentsInSchema)
    val parent: Option[InterfaceTypeDefinition] = parentsInSchema.headOption
    val allFields = i.fields filter { _.arguments.isEmpty }
    val classDef = if (scalaSealProtocols) "sealed abstract class" else "abstract class"
    val ctorParameters = genCtorParameters(i, parent, intfLang) mkString ", "
    val superCtorArguments =
      parent match {
        case Some(x) => x.fields map (_.name) mkString ", "
        case _       => ""
      }
    val extendsCode = genExtendsCode(parent, extraParents, superCtorArguments)
    val extendsCodeCompanion = genExtendsCodeCompanion(toCompanionExtraIntfComment(i))
    val doc = toDoc(i.comments)
    val extra = toExtra(i)
    val since = getSince(i.directives)
    val alternativeCtors = genAlternativeConstructors(since, allFields, false, intfLang) mkString EOL
    val lazyMembers = genLazyMembers(localFields(i, parentsInSchema), intfLang) mkString EOL
    val msgs = i.fields filter { _.arguments.nonEmpty }
    val messages = genMessages(msgs, intfLang) mkString EOL
    val toStringImpl: List[String] = toToStringImpl(i)
    val companionExtra: List[String] = toCompanionExtra(i)

    val code =
      s"""${genPackage(i)}
         |${genDoc(doc)}
         |$classDef ${i.name}($ctorParameters) $extendsCode {
         |  ${extra mkString EOL}
         |  $alternativeCtors
         |  $lazyMembers
         |  $messages
         |  ${genEquals(i, intfLang)}
         |  ${genHashCode(i, intfLang)}
         |  ${genToString(i, toStringImpl)}
         |}
         |
         |object ${i.name}$extendsCodeCompanion {
         |  ${companionExtra mkString EOL}
         |}""".stripMargin

    ListMap(genFile(i) -> code)
  }

  private def interfaceLanguage(parents: List[InterfaceTypeDefinition]): String =
    if (parents.isEmpty) "Scala"
    else {
      if (parents exists { p => toTarget(p.directives) == Some("Java") }) "Java"
      else "Scala"
    }

  private def genDoc(doc: List[String]) = doc match {
    case Nil      => ""
    case l :: Nil => s"/** $l */"
    case lines =>
      val doc = lines map (l => s" * $l") mkString EOL
      s"""/**
         |$doc
         | */""".stripMargin
  }

  private def genExtendsCode(parent: Option[InterfaceTypeDefinition], extraParents: List[String], superCtorArguments: String): String = {
    val parentInterface = parent.map(p => s"${fullyQualifiedName(p)}($superCtorArguments)")
    val allParents = parentInterface.toList ::: extraParents ::: List("Serializable")
    val extendsCode = allParents.mkString(" with ")
    if (extendsCode == "") "" else s"extends $extendsCode"
  }

  private def genExtendsCodeCompanion(companion: List[String]): String = {
    val extendsCodeCompanion = companion.mkString(" with ")
    if (extendsCodeCompanion == "") "" else s" extends $extendsCodeCompanion"
  }

  private def genParam(f: FieldDefinition, intfLang: String): String = genParam(f.name, f.fieldType, intfLang)
  private def genParam(name: String, fieldType: Type, intfLang: String): String =
    s"${bq(name)}: ${genRealTpe(fieldType, isParam = true, intfLang)}"

  private def lookupTpe(tpe: String): String = tpe match {
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

  private def genRealTpe(tpe: ast.Type, isParam: Boolean, intfLang: String) =
    if (intfLang == "Scala") {
      val scalaTpe = lookupTpe(tpe.name)
      val base = tpe match {
        case x if x.isListType     => s"$scalaArray[$scalaTpe]"
        case x if !x.isNotNullType => s"Option[$scalaTpe]"
        case _                     => scalaTpe
      }
      if (tpe.isLazyType && isParam) s"=> $base" else base
    } else {
      val scalaTpe = lookupTpe(tpe.name)
      tpe match {
        case t: ast.Type if t.isLazyType && t.isListType      => s"$javaLazy[Array[${scalaTpe}]]"
        case t: ast.Type if t.isLazyType && !t.isNotNullType  => s"$javaLazy[$javaOptional[${javaLangBoxedType(scalaTpe)}]]"
        case t: ast.Type if t.isLazyType && t.isNotNullType   => s"$javaLazy[${javaLangBoxedType(scalaTpe)}]"
        case t: ast.Type if !t.isLazyType && t.isListType     => s"Array[$scalaTpe]"
        case t: ast.Type if !t.isLazyType && !t.isNotNullType => s"$javaOptional[${javaLangBoxedType(scalaTpe)}]"
        case t: ast.Type if !t.isLazyType && t.isNotNullType  => scalaTpe
      }
    }

  private def genEquals(cl: RecordLikeDefinition, intfLang: String) = {
    val allFields = cl.fields filter { _.arguments.isEmpty }

    val (x, comparisonCode) =
      intfLang match {
        case _ if allFields exists (_.fieldType.isLazyType) =>
          ("_", "super.equals(o) // We have lazy members, so use object identity to avoid circularity.")
        case _ if allFields.isEmpty =>
          ("_", "true")
        case "Scala" =>
          ("x", allFields map (f => s"(this.${bq(f.name)} == x.${bq(f.name)})") mkString " && ")
        case _ =>
          (
            "x",
            (allFields map { f =>
              genJavaEquals("this", "x", f, s"${bq(f.name)}", false)
            }).mkString(" && ")
          )
      }

    s"""override def equals(o: Any): Boolean = this.eq(o.asInstanceOf[AnyRef]) || (o match {
       |  case $x: ${cl.name} => $comparisonCode
       |  case _ => false
       |})""".stripMargin
  }

  private def genHashCode(cl: RecordLikeDefinition, intfLang: String) = {
    val allFields = cl.fields filter { _.arguments.isEmpty }
    val fqcn = cl.namespace.fold("")(_ + ".") + cl.name
    val seed = s"""37 * (17 + "$fqcn".##)"""
    val computationCode =
      intfLang match {
        case _ if allFields exists (_.fieldType.isLazyType) =>
          s"super.hashCode // Avoid evaluating lazy members in hashCode to avoid circularity."
        case "Scala" =>
          allFields.foldLeft(seed) { (acc, f) => s"37 * ($acc + ${bq(f.name)}.##)" }
        case _ =>
          allFields.foldLeft(seed) { (acc, f) => s"37 * ($acc +  ${genJavaHashCode(f, bq(f.name), false)})" }
      }

    s"""override def hashCode: Int = {
       |  $computationCode
       |}""".stripMargin
  }

  private def genToString(cl: RecordLikeDefinition, toStringImpl: List[String]) = {
    val body = if (toStringImpl.isEmpty) {
      val allFields = cl.fields filter { _.arguments.isEmpty }
      if (allFields exists (_.fieldType.isLazyType)) {
        s"super.toString // Avoid evaluating lazy members in toString to avoid circularity."
      } else if (allFields.isEmpty) {
        s""""${cl.name}()""""
      } else {
        val fieldsToString = allFields.map(f => bq(f.name)).mkString(" + ", """ + ", " + """, " + ")
        s""""${cl.name}("$fieldsToString")""""
      }
    } else toStringImpl mkString s"$EOL  "

    s"""override def toString: String = {
       |  $body
       |}""".stripMargin
  }

  private def renderScalaValue(v: Value, tpe: Type): String =
    v match {
      case x: NullValue =>
        if (tpe.isListType) "Vector()"
        else if (!tpe.isNotNullType) "None"
        else sys.error(s"Expected $tpe but found $v")
      case raw: RawValue =>
        raw.renderPretty
      case _ =>
        val str =
          v match {
            case x: ObjectValue =>
              val args = x.fields map { f => f.value.renderPretty }
              s"""${tpe.name}(${args.mkString(", ")})"""
            case _ => v.renderPretty
          }
        if (tpe.isListType) s"Vector($str)"
        else if (tpe.isNotNullType) str
        else s"Option($str)"
    }

  private def renderJavaValue(v: Value, tpe: Type): String =
    v match {
      case x: NullValue =>
        if (tpe.isListType) "Array()"
        else if (!tpe.isNotNullType) mkOptional("null", tpe, "Java")
        else sys.error(s"Expected $tpe but found $v")
      case raw: RawValue =>
        raw.renderPretty
      case _ =>
        val str =
          v match {
            case x: ObjectValue =>
              val args = x.fields map { f => f.value.renderPretty }
              s"""${tpe.name}(${args.mkString(", ")})"""
            case _ => v.renderPretty
          }
        if (tpe.isListType) "Array(${str})"
        else if (tpe.isNotNullType) str
        else mkOptional(str, tpe, "Java")
    }

  private def mkOptional(e: String, tpe: Type, intfLang: String): String =
    if (intfLang == "Scala") s"Option($e)"
    else {
      val x = instantiateJavaOptional(javaLangBoxedType(tpe.name), e)
      // java.util.Optional.<Integer>ofNullable(number)
      val JavaGenericMethod = """(.+)<([^>]+)>(\w+)\((.*)\)""".r
      x match {
        case JavaGenericMethod(pre, typearg, mtd, arg) => s"$pre$mtd[$typearg]($arg)"
        case _                                         => x
      }
    }

  private def renderDefaultValue(f: FieldDefinition, intfLang: String): String =
    f.defaultValue match {
      case Some(v) =>
        if (intfLang == "Scala") renderScalaValue(v, f.fieldType)
        else renderJavaValue(v, f.fieldType)
      case None if f.fieldType.isListType || !f.fieldType.isNotNullType =>
        if (intfLang == "Scala") renderScalaValue(NullValue(), f.fieldType)
        else renderJavaValue(NullValue(), f.fieldType)
      case _ => sys.error(s"Needs a default value for field: ${f.name}.")
    }

  private def genApplyOverloads(r: ObjectTypeDefinition, allFields: List[FieldDefinition], intfLang: String): List[String] =
    if (allFields.isEmpty) { // If there are no fields, we still need an `apply` method with an empty parameter list
      List(s"def apply(): ${r.name} = new ${r.name}()")
    } else {
      val since = getSince(r.directives)
      perVersionNumber(since, allFields) { (provided, _) =>
        val applyParameters = provided map { f => genParam(f, intfLang) } mkString ", "

        val ctorCallArguments = provided map (f => bq(f.name)) mkString ", "
        s"def apply($applyParameters): ${r.name} = new ${r.name}($ctorCallArguments)" + {
          if (!containsOptional(provided) || !wrapOption) ""
          else {
            val applyParameters2 = (provided map { f =>
              if (f.fieldType.isOptionalType) genParam(f.name, f.fieldType.notNull, intfLang)
              else genParam(f, intfLang)
            }).mkString(", ")
            val ctorCallArguments2 =
              provided
                .map { f =>
                  if (f.fieldType.isOptionalType)
                    mkOptional(bq(f.name), f.fieldType, intfLang)
                  else
                    bq(f.name)
                }
                .mkString(", ")
            EOL + s"def apply($applyParameters2): ${r.name} = new ${r.name}($ctorCallArguments2)"
          }
        }
      }
    }

  private def genAlternativeConstructors(
      since: VersionNumber,
      allFields: List[FieldDefinition],
      privateConstructor: Boolean,
      intfLang: String
  ) =
    perVersionNumber(since, allFields) {
      case (provided, byDefault) if byDefault.nonEmpty => // Don't duplicate up-to-date constructor
        val ctorParameters = provided map { f => genParam(f, intfLang) } mkString ", "
        val thisCallArguments =
          allFields map {
            case f if provided contains f  => bq(f.name)
            case f if byDefault contains f => renderDefaultValue(f, intfLang)
          } mkString ", "
        val privateCtr = if (privateConstructor) "private " else ""

        s"${privateCtr}def this($ctorParameters) = this($thisCallArguments)"

      case _ => ""
    }

  // If a class has a lazy member, it means that the class constructor will have a call-by-name
  // parameter. Because val parameters may not be call-by-name, we prefix the parameter with `_`
  // and we will create the actual lazy val as a regular class member.
  // Non-lazy fields that belong to `cl` are made val parameters.
  private def genCtorParameters(cl: RecordLikeDefinition, parent: Option[InterfaceTypeDefinition], intfLang: String): List[String] = {
    val allFields = cl.fields filter { _.arguments.isEmpty }
    val parentFields: List[FieldDefinition] =
      parent match {
        case Some(x) => x.fields filter { _.arguments.isEmpty }
        case _       => Nil
      }
    def inParent(f: FieldDefinition): Boolean = {
      val x = parentFields exists { _.name == f.name }
      x
    }
    allFields map {
      case f if !inParent(f) && f.fieldType.isLazyType =>
        EOL + "_" + genParam(f, intfLang)
      case f if !inParent(f) =>
        s"""${EOL}val ${genParam(f, intfLang)}""".stripMargin
      case f => EOL + genParam(f, intfLang)
    }
  }

  private def genLazyMembers(fields: List[FieldDefinition], intfLang: String): List[String] =
    fields filter (_.fieldType.isLazyType) map { f =>
      val doc = toDoc(f.comments)
      s"""${genDoc(doc)}
           |lazy val ${bq(f.name)}: ${genRealTpe(f.fieldType, isParam = false, intfLang)} = _${f.name}""".stripMargin
    }

  private def genMessages(messages: List[FieldDefinition], intfLang: String): List[String] =
    messages map { case FieldDefinition(name, fieldType, arguments, defaultValue, dirs, comments, _) =>
      val params = arguments map (a => s"${bq(a.name)}: ${genRealTpe(a.valueType, isParam = true, intfLang)}") mkString ", "
      val argsDoc = arguments flatMap { (a: InputValueDefinition) =>
        toDoc(a.comments) match {
          case Nil        => Nil
          case doc :: Nil => s"@param ${a.name} $doc" :: Nil
          case docs =>
            val prefix = s"@param ${a.name} "
            docs.mkString(prefix, EOL + " " * (prefix.length + 3), "") :: Nil
        }
      }
      val doc = toDoc(comments)
      s"""${genDoc(doc ++ argsDoc)}
         |def $name($params): ${genRealTpe(fieldType, isParam = false, intfLang)}"""
    }

  private def fullyQualifiedName(d: TypeDefinition): String = {
    val path = d.namespace map (ns => ns + ".") getOrElse ""
    path + d.name
  }

  private def genPackage(d: TypeDefinition): String = d.namespace map (ns => s"package $ns") getOrElse ""

  private def genCopy(r: ObjectTypeDefinition, intfLang: String) = {
    val allFields = r.fields filter { _.arguments.isEmpty }
    def genParam(f: FieldDefinition) = s"${bq(f.name)}: ${genRealTpe(f.fieldType, isParam = true, intfLang)} = ${bq(f.name)}"
    val params = allFields map genParam mkString ", "
    val constructorCall = allFields map (f => bq(f.name)) mkString ", "
    s"""private[this] def copy($params): ${r.name} = {
       |  new ${r.name}($constructorCall)
       |}""".stripMargin
  }

  private def genWith(r: ObjectTypeDefinition, intfLang: String) = {
    def capitalize(s: String) = { val (fst, rst) = s.splitAt(1); fst.toUpperCase + rst }
    r.fields map { f =>
      s"""def with${capitalize(f.name)}(${bq(f.name)}: ${genRealTpe(f.fieldType, isParam = true, intfLang)}): ${r.name} = {
         |  copy(${bq(f.name)} = ${bq(f.name)})
         |}""".stripMargin +
        (if (f.fieldType.isListType || f.fieldType.isNotNullType) ""
         else s"""
                |def with${capitalize(f.name)}(${bq(f.name)}: ${genRealTpe(f.fieldType.notNull, isParam = true, intfLang)}): ${r.name} = {
                |  copy(${bq(f.name)} = ${mkOptional(bq(f.name), f.fieldType, intfLang)})
                |}""".stripMargin)
    } mkString (EOL + EOL)
  }
}
