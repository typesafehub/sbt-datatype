package sbt.contraband

import scala.compat.Platform.EOL
import java.io.File
import scala.collection.immutable.ListMap
import ast._
import AstUtil._

/**
 * Code generator for Java.
 */
class JavaCodeGen(lazyInterface: String, optionalInterface: String,
  instantiateJavaOptional: String => String) extends CodeGenerator {

  /** Indentation configuration for Java sources. */
  implicit object indentationConfiguration extends IndentationConfiguration {
    override val indentElement = "    "
    override def augmentIndentAfterTrigger(s: String) = s endsWith "{"
    override def reduceIndentTrigger(s: String) = s startsWith "}"
    override def enterMultilineJavadoc(s: String) = s == "/**"
    override def exitMultilineJavadoc(s: String) = s == "*/"
  }

  override def generate(s: Document): ListMap[File, String] =
    ListMap((s.definitions collect {
      case td: TypeDefinition => td
    }) flatMap (generate(s, _).toList): _*) mapV (_.indented)

  override def generateInterface(s: Document, i: InterfaceTypeDefinition): ListMap[File, String] = {
    val InterfaceTypeDefinition(name, namespace, interfaces, fields, dirs, comments, trailingComments, position) = i
    val extraParents = toExtraIntf(i)
    val parents = i.interfaces
    val parentsInSchema = lookupInterfaces(s, parents)
    val parent: Option[InterfaceTypeDefinition] = parentsInSchema.headOption
    val allFields = fields filter { _.arguments.isEmpty }
    val extendsCode = parent map (p => s"extends ${fullyQualifiedName(p)}") getOrElse "implements java.io.Serializable"
    val doc = toDoc(comments)
    val extra = toExtra(i)
    val msgs = i.fields filter { _.arguments.nonEmpty }
    val toStringImpl: List[String] = toToStringImpl(i)
    val lfs = localFields(i, parentsInSchema)
    val code =
      s"""${genPackage(i)}
         |${genDoc(doc)}
         |public abstract class $name $extendsCode {
         |    ${extra mkString EOL}
         |    ${genFields(lfs)}
         |    ${genConstructors(i, parent)}
         |    ${genAccessors(lfs)}
         |    ${genMessages(msgs)}
         |    ${genEquals(i)}
         |    ${genHashCode(i)}
         |    ${genToString(i, toStringImpl)}
         |}""".stripMargin

    ListMap(genFile(i) -> code)
  }

  override def generateRecord(s: Document, r: ObjectTypeDefinition): ListMap[File, String] = {
    val ObjectTypeDefinition(name, _, _, fields, directives, _, _, _) = r
    val extraParents = toExtraIntf(r)
    val parents = r.interfaces
    val parentsInSchema = lookupInterfaces(s, parents)
    val parent: Option[InterfaceTypeDefinition] = parentsInSchema.headOption
    val doc = toDoc(r.comments)
    val extra = toExtra(r)
    val extendsCode = parent map (p => s"extends ${fullyQualifiedName(p)}") getOrElse "implements java.io.Serializable"
    val toStringImpl: List[String] = toToStringImpl(r)
    val lfs = localFields(r, parentsInSchema)

    val code =
      s"""${genPackage(r)}
         |${genDoc(doc)}
         |public final class $name $extendsCode {
         |    ${extra mkString EOL}
         |    ${genFields(lfs)}
         |    ${genConstructors(r, parent)}
         |    ${genAccessors(lfs)}
         |    ${genWith(r, parentsInSchema)}
         |    ${genEquals(r)}
         |    ${genHashCode(r)}
         |    ${genToString(r, toStringImpl)}
         |}""".stripMargin

    ListMap(genFile(r) -> code)
  }

  override def generateEnum(s: Document, e: EnumTypeDefinition): ListMap[File, String] = {
    val EnumTypeDefinition(name, _, values, _, comments, _, _) = e

    val valuesCode =
      if (values.isEmpty) ""
      else (values map { case EnumValueDefinition(name, dir, comments, _) =>
        s"""${genDoc(toDoc(comments))}
           |$name""".stripMargin
      }).mkString("", "," + EOL, ";")

    val extra = toExtra(e)
    val code =
      s"""${genPackage(e)}
         |${genDoc(toDoc(comments))}
         |public enum $name {
         |    $valuesCode
         |    ${extra mkString EOL}
         |}""".stripMargin

    ListMap(genFile(e) -> code)
  }

  private def genDoc(doc: List[String]) = doc match {
    case Nil => ""
    case l :: Nil => s"/** $l */"
    case lines =>
      val doc = lines map (l => s" * $l") mkString EOL
      s"""/**
         |$doc
         | */""".stripMargin
  }

  private def genFile(d: TypeDefinition) = {
    val fileName = d.name + ".java"
    d.namespace map (ns => new File(ns.replace(".", File.separator), fileName)) getOrElse new File(fileName)
  }

  private def genFields(fields: List[FieldDefinition]) = fields map genField mkString EOL
  private def genField(f: FieldDefinition) =
    s"""${genDoc(toDoc(f.comments))}
       |private ${genRealTpe(f.fieldType)} ${f.name};""".stripMargin

  private def isPrimitive(tpe: ast.Type) = !tpe.isListType && !tpe.isLazyType && tpe.name != boxedType(tpe.name)
  private def isPrimitiveArray(tpe: ast.Type) = tpe.isListType && !tpe.isLazyType && tpe.name != boxedType(tpe.name)

  private def boxedType(tpe: String): String =
    tpe match {
      case "boolean" | "Boolean" => "Boolean"
      case "byte" | "Byte"       => "Byte"
      case "char" | "Char"       => "Character"
      case "float" | "Float"     => "Float"
      case "int" | "Int"         => "Integer"
      case "long" | "Long"       => "Long"
      case "short" | "Short"     => "Short"
      case "double" | "Double"   => "Double"
      case other     => other
    }

  private def unboxedType(tpe: String): String =
    tpe match {
      case "boolean" | "Boolean" => "boolean"
      case "byte" | "Byte"       => "byte"
      case "char" | "Char"       => "char"
      case "float" | "Float"     => "float"
      case "int" | "Int"         => "int"
      case "long" | "Long"       => "long"
      case "short" | "Short"     => "short"
      case "double" | "Double"   => "double"
      case other     => other
    }

  private def genRealTpe(tpe: ast.Type): String = tpe match {
    case t: ast.Type if t.isLazyType && t.isListType      => s"$lazyInterface<${unboxedType(t.name)}[]>"
    case t: ast.Type if t.isLazyType && !t.isNotNullType  => s"$lazyInterface<$optionalInterface<${boxedType(t.name)}>>"
    case t: ast.Type if t.isLazyType && t.isNotNullType   => s"$lazyInterface<${boxedType(t.name)}>"
    case t: ast.Type if !t.isLazyType && t.isListType     => s"${unboxedType(t.name)}[]"
    case t: ast.Type if !t.isLazyType && !t.isNotNullType => s"$optionalInterface<${boxedType(t.name)}>"
    case t: ast.Type if !t.isLazyType && t.isNotNullType  => unboxedType(t.name)
  }

  private def genAccessors(fields: List[FieldDefinition]) = fields map genAccessor mkString EOL
  private def genAccessor(field: FieldDefinition) = {
    val accessCode =
      if (field.fieldType.isLazyType) s"this.${field.name}.get()"
      else s"this.${field.name}"

    // We don't use `genRealTpe` here because the field will be evaluated
    // if it is lazy.
    val tpeSig =
      if (field.fieldType.isListType) s"${unboxedType(field.fieldType.name)}[]"
      else unboxedType(field.fieldType.name)

    s"""public $tpeSig ${field.name}() {
       |    return $accessCode;
       |}""".stripMargin
  }

  private def genMessages(messages: List[FieldDefinition]) = messages map genMessage mkString EOL
  private def genMessage(message: FieldDefinition): String =
    {
      val FieldDefinition(name, fieldType, arguments, defaultValue, dirs, comments, _) = message
      val doc = toDoc(comments)
      val argsDoc = arguments flatMap { a: InputValueDefinition =>
        toDoc(a.comments) match {
          case Nil        => Nil
          case doc :: Nil => s"@param ${a.name} $doc" :: Nil
          case docs =>
            val prefix = s"@param ${a.name} "
            docs.mkString(prefix, EOL + " " * (prefix.length + 3), "") :: Nil
        }
      }
      val params: List[String] = arguments map { a => s"${genRealTpe(a.valueType)} ${a.name}" }
      s"""${genDoc(doc ++ argsDoc)}
         |public abstract ${genRealTpe(fieldType)} ${name}(${params mkString ","});"""
    }

  private def renderJavaValue(v: Value, tpe: Type): String =
    v match {
      case x: NullValue =>
        if (tpe.isListType) "new Array {}"
        else if (!tpe.isNotNullType) s"""${instantiateJavaOptional("null")}"""
        else sys.error(s"Expected $tpe but found $v")
      case x: ScalarValue =>
        if (tpe.isListType) "new Array { ${x.renderPretty} }"
        else if (tpe.isNotNullType) x.renderPretty
        else s"${instantiateJavaOptional(x.renderPretty)}"
      case _ => v.renderPretty
    }

  private def renderDefaultValue(f: FieldDefinition): String =
    f.defaultValue match {
      case Some(v) => renderJavaValue(v, f.fieldType)
      case None if f.fieldType.isListType || !f.fieldType.isNotNullType =>
        renderJavaValue(NullValue(), f.fieldType)
      case _       => sys.error(s"Needs a default value for field ${f.name}.")
    }

  private def genConstructors(cl: RecordLikeDefinition, parent: Option[InterfaceTypeDefinition]) =
    perVersionNumber(getSince(cl.directives), cl.fields filter { _.arguments.isEmpty }) { (provided, byDefault) =>
      val lfs = localFields(cl, parent.toList)
      val ctorParameters = provided map (f => s"${genRealTpe(f.fieldType)} _${f.name}") mkString ", "
      val superFields: List[FieldDefinition] =
        parent match {
          case Some(x) => x.fields filter { _.arguments.isEmpty }
          case _       => Nil
        }
      val superFieldsValues = superFields map {
        case f if provided contains f  => s"_${f.name}"
        case f if byDefault contains f => renderDefaultValue(f)
      }
      val superCall = superFieldsValues.mkString("super(", ", ", ");")
      val assignments = lfs map {
        case f if provided contains f  => s"${f.name} = _${f.name};"
        case f if byDefault contains f => s"${f.name} = ${renderDefaultValue(f)};"
      } mkString EOL

      s"""public ${cl.name}($ctorParameters) {
         |    $superCall
         |    $assignments
         |}""".stripMargin
    } mkString (EOL + EOL)

  private def genWith(r: ObjectTypeDefinition, parents: List[InterfaceTypeDefinition]) = {
    def capitalize(s: String) = { val (fst, rst) = s.splitAt(1) ; fst.toUpperCase + rst }
    val allFields = (r.fields filter { _.arguments.isEmpty }).zipWithIndex
    val lfs = localFields(r, parents)
    def nonParam(f: (FieldDefinition, Int)): String = {
      val field = f._1
      if (lfs contains field) field.name
      else if (field.fieldType.isLazyType) {
        val tpeSig =
          if (field.fieldType.isListType) s"${field.fieldType.name}[]"
          else field.fieldType.name
        s"new ${genRealTpe(field.fieldType)}() { public ${boxedType(tpeSig)} get() { return ${field.name}(); } }"
      } else s"${f._1.name}()"
    }

    allFields map { case (f, idx) =>
      val (before, after) = allFields filterNot (_._2 == idx) splitAt idx
      val params = (before map nonParam) ::: f.name :: (after map nonParam) mkString ", "
      s"""public ${r.name} with${capitalize(f.name)}(${genRealTpe(f.fieldType)} ${f.name}) {
         |    return new ${r.name}($params);
         |}""".stripMargin
    } mkString (EOL + EOL)
  }

  private def genEquals(cl: RecordLikeDefinition) = {
    val allFields = cl.fields filter { _.arguments.isEmpty }
    val body =
      if (allFields exists { f => f.fieldType.isLazyType }) {
        "return this == obj; // We have lazy members, so use object identity to avoid circularity."
      } else {
        val comparisonCode =
          if (allFields.isEmpty) "return true;"
          else
            allFields.map {
              case f if isPrimitive(f.fieldType)      => s"(${f.name}() == o.${f.name}())"
              case f if isPrimitiveArray(f.fieldType) => s"java.util.Arrays.equals(${f.name}(), o.${f.name}())"
              case f if f.fieldType.isListType        => s"java.util.Arrays.deepEquals(${f.name}(), o.${f.name}())"
              case f                                  => s"${f.name}().equals(o.${f.name}())"
            }.mkString("return ", " && ", ";")

        s"""if (this == obj) {
           |    return true;
           |} else if (!(obj instanceof ${cl.name})) {
           |    return false;
           |} else {
           |    ${cl.name} o = (${cl.name})obj;
           |    $comparisonCode
           |}""".stripMargin
      }

    s"""public boolean equals(Object obj) {
       |    $body
       |}""".stripMargin
  }

  private def hashCode(f: FieldDefinition): String =
    if (isPrimitive(f.fieldType)) s"(new ${boxedType(f.fieldType.name)}(${f.name}())).hashCode()"
    else s"${f.name}().hashCode()"

  private def genHashCode(cl: RecordLikeDefinition) = {
    val allFields = cl.fields filter { _.arguments.isEmpty }
    val body =
      if (allFields exists { f => f.fieldType.isLazyType }) {
        "return super.hashCode(); // Avoid evaluating lazy members in hashCode to avoid circularity."
      } else {
        val computation = (allFields foldLeft ("17")) { (acc, f) => s"37 * ($acc + ${hashCode(f)})" }
        s"return $computation;"
      }

    s"""public int hashCode() {
       |    $body
       |}""".stripMargin
  }

  private def genToString(cl: RecordLikeDefinition, toString: List[String]) = {
    val body = if (toString.isEmpty) {
      val allFields = cl.fields filter { _.arguments.isEmpty }
      if (allFields exists { f => f.fieldType.isLazyType }) {
        "return super.toString(); // Avoid evaluating lazy members in toString to avoid circularity."
      } else {
        allFields.map{ f =>
          s""" + "${f.name}: " + ${f.name}()"""
        }.mkString(s"""return "${cl.name}(" """, " + \", \"", " + \")\";")
      }
    } else toString mkString s"$EOL    "

    s"""public String toString() {
       |    $body
       |}""".stripMargin
  }

  private def genPackage(d: TypeDefinition) =
    d.namespace map (ns => s"package $ns;") getOrElse ""

  private def fullyQualifiedName(d: TypeDefinition) = {
    val path = d.namespace map (ns => ns + ".") getOrElse ""
    path + d.name
  }

}