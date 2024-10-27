package sbt.contraband

import java.io.File

import JsonSchemaExample._
import parser.JsonParser

class JsonScalaCodeGenSpec extends GCodeGenSpec("Scala") {
  override def enumerationGenerateSimple = {
    val enumeration = JsonParser.EnumTypeDefinition.parse(simpleEnumerationExample)
    val code = mkScalaCodeGen generate enumeration

    code.head._2.unindent should equalLines("""/** Example of simple enumeration */
        |sealed abstract class simpleEnumerationExample extends Serializable
        |object simpleEnumerationExample {
        |  // Some extra code...
        |  /** First symbol */
        |  case object first extends simpleEnumerationExample
        |  case object second extends simpleEnumerationExample
        |}
        """.stripMargin.unindent)
  }

  override def interfaceGenerateSimple = {
    val protocol = JsonParser.InterfaceTypeDefinition.parseInterface(simpleInterfaceExample)
    val code = mkScalaCodeGen generate protocol

    code.head._2.unindent should equalLines("""/** example of simple interface */
        |sealed abstract class simpleInterfaceExample(
        |  val field: type) extends Interface1 with Interface2 with Serializable {
        |  // Some extra code...
        |  override def equals(o: Any): Boolean = this.eq(o.asInstanceOf[AnyRef]) || (o match {
        |    case x: simpleInterfaceExample => (this.field == x.field)
        |    case _ => false
        |  })
        |  override def hashCode: Int = {
        |    37 * (37 * (17 + "simpleInterfaceExample".##) + field.##)
        |  }
        |  override def toString: String = {
        |    return "custom";
        |  }
        |}
        |
        |object simpleInterfaceExample extends CompanionInterface1 with CompanionInterface2 {
        |  // Some extra companion code...
        |}
        |""".stripMargin.unindent)
  }

  override def interfaceGenerateOneChild = {
    val protocol = JsonParser.InterfaceTypeDefinition.parseInterface(oneChildInterfaceExample)
    val code = mkScalaCodeGen generate protocol

    code.head._2.unindent should equalLines("""/** example of interface */
        |sealed abstract class oneChildInterfaceExample(
        |    val field: Int) extends Serializable {
        |  override def equals(o: Any): Boolean = this.eq(o.asInstanceOf[AnyRef]) || (o match {
        |    case x: oneChildInterfaceExample => (this.field == x.field)
        |    case _ => false
        |  })
        |  override def hashCode: Int = {
        |    37 * (37 * (17 + "oneChildInterfaceExample".##) + field.##)
        |  }
        |  override def toString: String = {
        |    "oneChildInterfaceExample(" + field + ")"
        |  }
        |}
        |object oneChildInterfaceExample {
        |}
        |
        |final class childRecord private (
        |  field: Int,
        |  val x: Int) extends oneChildInterfaceExample(field) with Serializable {
        |  override def equals(o: Any): Boolean = this.eq(o.asInstanceOf[AnyRef]) || (o match {
        |    case x: childRecord => (this.field == x.field) && (this.x == x.x)
        |    case _ => false
        |  })
        |  override def hashCode: Int = {
        |    37 * (37 * (37 * (17 + "childRecord".##) + field.##) + x.##)
        |  }
        |  override def toString: String = {
        |    "childRecord(" + field + ", " + x + ")"
        |  }
        |  private def copy(field: Int = field, x: Int = x): childRecord = {
        |    new childRecord(field, x)
        |  }
        |  def withField(field: Int): childRecord = {
        |    copy(field = field)
        |  }
        |  def withX(x: Int): childRecord = {
        |    copy(x = x)
        |  }
        |}
        |
        |object childRecord {
        |  def apply(field: Int, x: Int): childRecord = new childRecord(field, x)
        |}
        |""".stripMargin.unindent)
  }

  override def interfaceGenerateNested = {
    val protocol = JsonParser.InterfaceTypeDefinition.parseInterface(nestedInterfaceExample)
    val code = mkScalaCodeGen generate protocol

    code.head._2.unindent should equalLines("""/** example of nested protocols */
        |sealed abstract class nestedProtocolExample() extends Serializable {
        |  override def equals(o: Any): Boolean = this.eq(o.asInstanceOf[AnyRef]) || (o match {
        |    case _: nestedProtocolExample => true
        |    case _ => false
        |  })
        |  override def hashCode: Int = {
        |    37 * (17 + "nestedProtocolExample".##)
        |  }
        |  override def toString: String = {
        |    "nestedProtocolExample()"
        |  }
        |}
        |object nestedProtocolExample {
        |}
        |
        |sealed abstract class nestedProtocol() extends nestedProtocolExample() with Serializable {
        |  override def equals(o: Any): Boolean = this.eq(o.asInstanceOf[AnyRef]) || (o match {
        |    case _: nestedProtocol => true
        |    case _ => false
        |  })
        |  override def hashCode: Int = {
        |    37 * (17 + "nestedProtocol".##)
        |  }
        |  override def toString: String = {
        |    "nestedProtocol()"
        |  }
        |}
        |
        |object nestedProtocol {
        |}
        |
        |final class ChildRecord private () extends nestedProtocol() with Serializable {
        |  override def equals(o: Any): Boolean = this.eq(o.asInstanceOf[AnyRef]) || (o match {
        |    case _: ChildRecord => true
        |    case _ => false
        |  })
        |  override def hashCode: Int = {
        |    37 * (17 + "ChildRecord".##)
        |  }
        |  override def toString: String = {
        |    "ChildRecord()"
        |  }
        |  private def copy(): ChildRecord = {
        |    new ChildRecord()
        |  }
        |}
        |object ChildRecord {
        |  def apply(): ChildRecord = new ChildRecord()
        |}
        |""".stripMargin.unindent)
  }

  override def interfaceGenerateMessages = {
    val schema = JsonParser.Document.parse(generateArgDocExample)
    val code = mkScalaCodeGen generate schema

    code.head._2.withoutEmptyLines should equalLines("""sealed abstract class generateArgDocExample(
        |  val field: Int) extends Serializable {
        |  /**
        |   * A very simple example of a message.
        |   * Messages can only appear in interface definitions.
        |   * @param arg0 The first argument of the message.
        |                 Make sure it is awesome.
        |   * @param arg1 This argument is not important, so it gets single line doc.
        |   */
        |  def messageExample(arg0: => Vector[Int], arg1: Boolean): Vector[Int]
        |  override def equals(o: Any): Boolean = this.eq(o.asInstanceOf[AnyRef]) || (o match {
        |    case x: generateArgDocExample => (this.field == x.field)
        |    case _ => false
        |  })
        |  override def hashCode: Int = {
        |    37 * (37 * (17 + "generateArgDocExample".##) + field.##)
        |  }
        |  override def toString: String = {
        |    "generateArgDocExample(" + field + ")"
        |  }
        |}
        |
        |object generateArgDocExample {
        |}
        |""".stripMargin.withoutEmptyLines)
  }

  override def recordGenerateSimple = {
    val record = JsonParser.ObjectTypeDefinition.parse(simpleRecordExample)
    val code = mkScalaCodeGen generate record

    code.head._2.unindent should equalLines("""/** Example of simple record */
        |final class simpleRecordExample private (
        |val field: java.net.URL) extends Serializable {
        |  // Some extra code...
        |  override def equals(o: Any): Boolean = this.eq(o.asInstanceOf[AnyRef]) || (o match {
        |    case x: simpleRecordExample => (this.field == x.field)
        |    case _ => false
        |  })
        |  override def hashCode: Int = {
        |    37 * (37 * (17 + "simpleRecordExample".##) + field.##)
        |  }
        |  override def toString: String = {
        |    "simpleRecordExample(" + field + ")"
        |  }
        |  private def copy(field: java.net.URL = field): simpleRecordExample = {
        |    new simpleRecordExample(field)
        |  }
        |  def withField(field: java.net.URL): simpleRecordExample = {
        |    copy(field = field)
        |  }
        |}
        |object simpleRecordExample {
        |  def apply(field: java.net.URL): simpleRecordExample = new simpleRecordExample(field)
        |}
        |""".stripMargin.unindent)
  }

  override def recordGrowZeroToOneField = {
    val record = JsonParser.ObjectTypeDefinition.parse(growableAddOneFieldExample)
    val code = mkScalaCodeGen generate record

    code.head._2.unindent should equalLines("""final class growableAddOneField private (
        |  val field: Int) extends Serializable {
        |  private def this() = this(0)
        |  override def equals(o: Any): Boolean = this.eq(o.asInstanceOf[AnyRef]) || (o match {
        |    case x: growableAddOneField => (this.field == x.field)
        |    case _ => false
        |  })
        |  override def hashCode: Int = {
        |    37 * (37 * (17 + "growableAddOneField".##) + field.##)
        |  }
        |  override def toString: String = {
        |    "growableAddOneField(" + field + ")"
        |  }
        |  private def copy(field: Int = field): growableAddOneField = {
        |    new growableAddOneField(field)
        |  }
        |  def withField(field: Int): growableAddOneField = {
        |    copy(field = field)
        |  }
        |}
        |object growableAddOneField {
        |  def apply(): growableAddOneField = new growableAddOneField()
        |  def apply(field: Int): growableAddOneField = new growableAddOneField(field)
        |}
        |""".stripMargin.unindent)
  }

  override def recordGrowZeroToOneToTwoFields = {
    val record = JsonParser.ObjectTypeDefinition.parse(growableZeroToOneToTwoFieldsExample)
    val code = mkScalaCodeGen generate record

    code.head._2.unindent should equalLines("""final class Foo private (
        |  val x: Option[Int],
        |  val y: Vector[Int]) extends Serializable {
        |  private def this() = this(Option(0), Vector(0))
        |  private def this(x: Option[Int]) = this(x, Vector(0))
        |  override def equals(o: Any): Boolean = this.eq(o.asInstanceOf[AnyRef]) || (o match {
        |    case x: Foo => (this.x == x.x) && (this.y == x.y)
        |    case _ => false
        |  })
        |  override def hashCode: Int = {
        |    37 * (37 * (37 * (17 + "Foo".##) + x.##) + y.##)
        |  }
        |  override def toString: String = {
        |    "Foo(" + x + ", " + y + ")"
        |  }
        |  private def copy(x: Option[Int] = x, y: Vector[Int] = y): Foo = {
        |    new Foo(x, y)
        |  }
        |  def withX(x: Option[Int]): Foo = {
        |    copy(x = x)
        |  }
        |  def withX(x: Int): Foo = {
        |    copy(x = Option(x))
        |  }
        |  def withY(y: Vector[Int]): Foo = {
        |    copy(y = y)
        |  }
        |}
        |object Foo {
        |  def apply(): Foo = new Foo()
        |  def apply(x: Option[Int]): Foo = new Foo(x)
        |  def apply(x: Int): Foo = new Foo(Option(x))
        |  def apply(x: Option[Int], y: Vector[Int]): Foo = new Foo(x, y)
        |  def apply(x: Int, y: Vector[Int]): Foo = new Foo(Option(x), y)
        |}
        |""".stripMargin.unindent)
  }

  override def recordPrimitives: Unit = {
    val record = JsonParser.ObjectTypeDefinition.parse(primitiveTypesExample2)
    val code = mkScalaCodeGen generate record

    code.head._2.unindent should equalLines("""final class primitiveTypesExample2 private (
  val smallBoolean: Boolean,
  val bigBoolean: Boolean) extends Serializable {



  override def equals(o: Any): Boolean = this.eq(o.asInstanceOf[AnyRef]) || (o match {
    case x: primitiveTypesExample2 => (this.smallBoolean == x.smallBoolean) && (this.bigBoolean == x.bigBoolean)
    case _ => false
  })
  override def hashCode: Int = {
    37 * (37 * (37 * (17 + "primitiveTypesExample2".##) + smallBoolean.##) + bigBoolean.##)
  }
  override def toString: String = {
    "primitiveTypesExample2(" + smallBoolean + ", " + bigBoolean + ")"
  }
  private def copy(smallBoolean: Boolean = smallBoolean, bigBoolean: Boolean = bigBoolean): primitiveTypesExample2 = {
    new primitiveTypesExample2(smallBoolean, bigBoolean)
  }
  def withSmallBoolean(smallBoolean: Boolean): primitiveTypesExample2 = {
    copy(smallBoolean = smallBoolean)
  }
  def withBigBoolean(bigBoolean: Boolean): primitiveTypesExample2 = {
    copy(bigBoolean = bigBoolean)
  }
}
object primitiveTypesExample2 {

  def apply(smallBoolean: Boolean, bigBoolean: Boolean): primitiveTypesExample2 = new primitiveTypesExample2(smallBoolean, bigBoolean)
}""".stripMargin.unindent)
  }

  override def recordWithModifier: Unit = {
    val record = JsonParser.ObjectTypeDefinition.parse(modifierExample)
    val code = mkScalaCodeGen generate record

    code.head._2.unindent should equalLines("""sealed class modifierExample private (
        |val field: Int) extends Serializable {
        |  override def equals(o: Any): Boolean = this.eq(o.asInstanceOf[AnyRef]) || (o match {
        |    case x: modifierExample => (this.field == x.field)
        |    case _ => false
        |  })
        |  override def hashCode: Int = {
        |    37 * (37 * (17 + "modifierExample".##) + field.##)
        |  }
        |  override def toString: String = {
        |    "modifierExample(" + field + ")"
        |  }
        |  private def copy(field: Int = field): modifierExample = {
        |    new modifierExample(field)
        |  }
        |  def withField(field: Int): modifierExample = {
        |    copy(field = field)
        |  }
        |}
        |object modifierExample {
        |  def apply(field: Int): modifierExample = new modifierExample(field)
        |}
        |""".stripMargin.unindent)
  }

  override def schemaGenerateTypeReferences = {
    val schema = JsonParser.Document.parse(primitiveTypesExample)
    val code = mkScalaCodeGen generate schema
    code.head._2.unindent should equalLines("""final class primitiveTypesExample private (
        |  val simpleInteger: Int,
        |  _lazyInteger: => Int,
        |  val arrayInteger: Vector[Int],
        |  val optionInteger: Option[Int],
        |  _lazyArrayInteger: => Vector[Int],
        |  _lazyOptionInteger: => Option[Int]) extends Serializable {
        |
        |
        |  lazy val lazyInteger: Int = _lazyInteger
        |  lazy val lazyArrayInteger: Vector[Int] = _lazyArrayInteger
        |  lazy val lazyOptionInteger: Option[Int] = _lazyOptionInteger
        |  override def equals(o: Any): Boolean = this.eq(o.asInstanceOf[AnyRef]) || (o match {
        |    case _: primitiveTypesExample => super.equals(o) // We have lazy members, so use object identity to avoid circularity.
        |    case _ => false
        |  })
        |  override def hashCode: Int = {
        |    super.hashCode // Avoid evaluating lazy members in hashCode to avoid circularity.
        |  }
        |  override def toString: String = {
        |    super.toString // Avoid evaluating lazy members in toString to avoid circularity.
        |  }
        |  private def copy(simpleInteger: Int = simpleInteger, lazyInteger: => Int = lazyInteger, arrayInteger: Vector[Int] = arrayInteger, optionInteger: Option[Int] = optionInteger, lazyArrayInteger: => Vector[Int] = lazyArrayInteger, lazyOptionInteger: => Option[Int] = lazyOptionInteger): primitiveTypesExample = {
        |    new primitiveTypesExample(simpleInteger, lazyInteger, arrayInteger, optionInteger, lazyArrayInteger, lazyOptionInteger)
        |  }
        |  def withSimpleInteger(simpleInteger: Int): primitiveTypesExample = {
        |    copy(simpleInteger = simpleInteger)
        |  }
        |  def withLazyInteger(lazyInteger: => Int): primitiveTypesExample = {
        |    copy(lazyInteger = lazyInteger)
        |  }
        |  def withArrayInteger(arrayInteger: Vector[Int]): primitiveTypesExample = {
        |    copy(arrayInteger = arrayInteger)
        |  }
        |  def withOptionInteger(optionInteger: Option[Int]): primitiveTypesExample = {
        |    copy(optionInteger = optionInteger)
        |  }
        |  def withOptionInteger(optionInteger: Int): primitiveTypesExample = {
        |    copy(optionInteger = Option(optionInteger))
        |  }
        |  def withLazyArrayInteger(lazyArrayInteger: => Vector[Int]): primitiveTypesExample = {
        |    copy(lazyArrayInteger = lazyArrayInteger)
        |  }
        |  def withLazyOptionInteger(lazyOptionInteger: => Option[Int]): primitiveTypesExample = {
        |    copy(lazyOptionInteger = lazyOptionInteger)
        |  }
        |  def withLazyOptionInteger(lazyOptionInteger: => Int): primitiveTypesExample = {
        |    copy(lazyOptionInteger = Option(lazyOptionInteger))
        |  }
        |}
        |
        |object primitiveTypesExample {
        |  def apply(simpleInteger: Int, lazyInteger: => Int, arrayInteger: Vector[Int], optionInteger: Option[Int], lazyArrayInteger: => Vector[Int], lazyOptionInteger: => Option[Int]): primitiveTypesExample = new primitiveTypesExample(simpleInteger, lazyInteger, arrayInteger, optionInteger, lazyArrayInteger, lazyOptionInteger)
        |  def apply(simpleInteger: Int, lazyInteger: => Int, arrayInteger: Vector[Int], optionInteger: Int, lazyArrayInteger: => Vector[Int], lazyOptionInteger: => Int): primitiveTypesExample = new primitiveTypesExample(simpleInteger, lazyInteger, arrayInteger, Option(optionInteger), lazyArrayInteger, Option(lazyOptionInteger))
        |}
        |""".stripMargin.unindent)
  }

  override def schemaGenerateTypeReferencesNoLazy = {
    val schema = JsonParser.Document.parse(primitiveTypesNoLazyExample)
    val code = mkScalaCodeGen generate schema

    code.head._2.unindent should equalLines("""final class primitiveTypesNoLazyExample private (
        |
        |  val simpleInteger: Int,
        |
        |  val arrayInteger: Vector[Int]) extends Serializable {
        |
        |
        |  override def equals(o: Any): Boolean = this.eq(o.asInstanceOf[AnyRef]) || (o match {
        |    case x: primitiveTypesNoLazyExample => (this.simpleInteger == x.simpleInteger) && (this.arrayInteger == x.arrayInteger)
        |    case _ => false
        |  })
        |  override def hashCode: Int = {
        |    37 * (37 * (37 * (17 + "primitiveTypesNoLazyExample".##) + simpleInteger.##) + arrayInteger.##)
        |  }
        |  override def toString: String = {
        |    "primitiveTypesNoLazyExample(" + simpleInteger + ", " + arrayInteger + ")"
        |  }
        |  private def copy(simpleInteger: Int = simpleInteger, arrayInteger: Vector[Int] = arrayInteger): primitiveTypesNoLazyExample = {
        |    new primitiveTypesNoLazyExample(simpleInteger, arrayInteger)
        |  }
        |  def withSimpleInteger(simpleInteger: Int): primitiveTypesNoLazyExample = {
        |    copy(simpleInteger = simpleInteger)
        |  }
        |  def withArrayInteger(arrayInteger: Vector[Int]): primitiveTypesNoLazyExample = {
        |    copy(arrayInteger = arrayInteger)
        |  }
        |}
        |
        |object primitiveTypesNoLazyExample {
        |  def apply(simpleInteger: Int, arrayInteger: Vector[Int]): primitiveTypesNoLazyExample = new primitiveTypesNoLazyExample(simpleInteger, arrayInteger)
        |}
        |""".stripMargin.unindent)
  }

  override def schemaGenerateComplete = {
    val schema = JsonParser.Document.parse(completeExample)
    val code = mkScalaCodeGen generate schema
    code.head._2.unindent should equalLines(completeExampleCodeScala.unindent)
  }

  override def schemaGenerateCompletePlusIndent = {
    val schema = JsonParser.Document.parse(completeExample)
    val code = mkScalaCodeGen generate schema
    code.head._2.withoutEmptyLines should equalLines(completeExampleCodeScala.withoutEmptyLines)
  }

  def scalaVersion: String = "2.13.15"
  def mkScalaCodeGen: ScalaCodeGen =
    new ScalaCodeGen(
      javaLazy,
      CodeGen.javaOptional,
      CodeGen.instantiateJavaOptional,
      scalaArray,
      genFileName,
      scalaSealProtocols = true,
      scalaPrivateConstructor = true,
      wrapOption = true,
      scalaVersion = scalaVersion,
    )
  val javaLazy = "com.example.Lazy"
  val outputFile = new File("output.scala")
  val scalaArray = "Vector"
  val genFileName = (_: Any) => outputFile
}
