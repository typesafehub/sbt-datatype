package sbt.contraband

import java.io.File

import JsonSchemaExample._
import parser.JsonParser

class JsonCodecCodeGenSpec extends GCodeGenSpec("Codec") {
  val codecParents = List("sjsonnew.BasicJsonProtocol")
  val instantiateJavaLazy = (s: String) => s"mkLazy($s)"
  val javaOption = "com.example.Option"
  val scalaArray = "Vector"
  val formatsForType: ast.Type => List[String] = CodecCodeGen.formatsForType
  val scalaVersion = "2.13.15"

  override def enumerationGenerateSimple = {
    val gen = new CodecCodeGen(codecParents, instantiateJavaLazy, javaOption, scalaArray, formatsForType, Nil, scalaVersion)
    val enumeration = JsonParser.EnumTypeDefinition.parse(simpleEnumerationExample)
    val code = gen generate enumeration

    code.head._2.unindent should equalLines("""/**
        | * This code is generated using [[https://www.scala-sbt.org/contraband]].
        | */
        |
        |// DO NOT EDIT MANUALLY
        |package generated
        |
        |import _root_.sjsonnew.{ Unbuilder, Builder, JsonFormat, deserializationError }
        |
        |trait SimpleEnumerationExampleFormats { self: sjsonnew.BasicJsonProtocol =>
        |  implicit lazy val simpleEnumerationExampleFormat: JsonFormat[_root_.simpleEnumerationExample] = new JsonFormat[_root_.simpleEnumerationExample] {
        |    override def read[J](__jsOpt: Option[J], unbuilder: Unbuilder[J]): _root_.simpleEnumerationExample = {
        |      __jsOpt match {
        |        case Some(__js) =>
        |          unbuilder.readString(__js) match {
        |            case "first" => _root_.simpleEnumerationExample.first
        |            case "second" => _root_.simpleEnumerationExample.second
        |          }
        |        case None =>
        |          deserializationError("Expected JsString but found None")
        |      }
        |    }
        |
        |    override def write[J](obj: _root_.simpleEnumerationExample, builder: Builder[J]): Unit = {
        |      val str = obj match {
        |        case _root_.simpleEnumerationExample.first => "first"
        |        case _root_.simpleEnumerationExample.second => "second"
        |      }
        |      builder.writeString(str)
        |    }
        |  }
        |}""".stripMargin.unindent)
  }

  override def interfaceGenerateSimple = {
    val gen = new CodecCodeGen(codecParents, instantiateJavaLazy, javaOption, scalaArray, formatsForType, Nil, scalaVersion)
    val intf = JsonParser.InterfaceTypeDefinition.parseInterface(simpleInterfaceExample)
    val code = gen generate intf

    code.head._2.unindent should equalLines("""/**
        | * This code is generated using [[https://www.scala-sbt.org/contraband]].
        | */
        |
        |// DO NOT EDIT MANUALLY
        |package generated
        |
        |import _root_.sjsonnew.{ deserializationError, serializationError, Builder, JsonFormat, Unbuilder }
        |
        |trait SimpleInterfaceExampleFormats {
        |  implicit lazy val simpleInterfaceExampleFormat: JsonFormat[_root_.simpleInterfaceExample] = new JsonFormat[_root_.simpleInterfaceExample] {
        |    override def read[J](__jsOpt: Option[J], unbuilder: Unbuilder[J]): _root_.simpleInterfaceExample = {
        |      deserializationError("No known implementation of simpleInterfaceExample.")
        |    }
        |    override def write[J](obj: _root_.simpleInterfaceExample, builder: Builder[J]): Unit = {
        |      serializationError("No known implementation of simpleInterfaceExample.")
        |    }
        |  }
        |}""".stripMargin.unindent)
  }

  override def interfaceGenerateOneChild = {
    val gen = new CodecCodeGen(codecParents, instantiateJavaLazy, javaOption, scalaArray, formatsForType, Nil, scalaVersion)
    val intf = JsonParser.InterfaceTypeDefinition.parseInterface(oneChildInterfaceExample)
    val code = gen generate intf

    code(new File("generated", "oneChildInterfaceExampleFormats.scala")).unindent should equalLines("""/**
        | * This code is generated using [[https://www.scala-sbt.org/contraband]].
        | */
        |
        |// DO NOT EDIT MANUALLY
        |package generated
        |
        |import _root_.sjsonnew.JsonFormat
        |
        |trait OneChildInterfaceExampleFormats { self: sjsonnew.BasicJsonProtocol with generated.ChildRecordFormats =>
        |  implicit lazy val oneChildInterfaceExampleFormat: JsonFormat[_root_.oneChildInterfaceExample] = flatUnionFormat1[_root_.oneChildInterfaceExample, _root_.childRecord]("type")
        |}""".stripMargin.unindent)
    code(new File("generated", "childRecordFormats.scala")).unindent should equalLines("""/**
        | * This code is generated using [[https://www.scala-sbt.org/contraband]].
        | */
        |
        |// DO NOT EDIT MANUALLY
        |package generated
        |
        |import _root_.sjsonnew.{ Unbuilder, Builder, JsonFormat, deserializationError }
        |
        |trait ChildRecordFormats { self: sjsonnew.BasicJsonProtocol =>
        |  implicit lazy val childRecordFormat: JsonFormat[_root_.childRecord] = new JsonFormat[_root_.childRecord] {
        |    override def read[J](__jsOpt: Option[J], unbuilder: Unbuilder[J]): _root_.childRecord = {
        |      __jsOpt match {
        |        case Some(__js) =>
        |          unbuilder.beginObject(__js)
        |          val field = unbuilder.readField[Int]("field")
        |          val x = unbuilder.readField[Int]("x")
        |          unbuilder.endObject()
        |          _root_.childRecord(field, x)
        |        case None =>
        |          deserializationError("Expected JsObject but found None")
        |      }
        |    }
        |    override def write[J](obj: _root_.childRecord, builder: Builder[J]): Unit = {
        |      builder.beginObject()
        |      builder.addField("field", obj.field)
        |      builder.addField("x", obj.x)
        |      builder.endObject()
        |    }
        |  }
        |}""".stripMargin.unindent)
  }

  override def interfaceGenerateNested = {
    val gen = new CodecCodeGen(codecParents, instantiateJavaLazy, javaOption, scalaArray, formatsForType, Nil, scalaVersion)
    val intf = JsonParser.InterfaceTypeDefinition.parseInterface(nestedInterfaceExample)
    val code = gen generate intf

    code(new File("generated", "nestedProtocolExampleFormats.scala")).unindent should equalLines("""/**
        | * This code is generated using [[https://www.scala-sbt.org/contraband]].
        | */
        |
        |// DO NOT EDIT MANUALLY
        |package generated
        |
        |import _root_.sjsonnew.JsonFormat
        |
        |trait NestedProtocolExampleFormats { self: sjsonnew.BasicJsonProtocol with generated.ChildRecordFormats =>
        |  implicit lazy val nestedProtocolExampleFormat: JsonFormat[_root_.nestedProtocolExample] = flatUnionFormat1[_root_.nestedProtocolExample, _root_.ChildRecord]("type")
        |}""".stripMargin.unindent)
    code.contains(new File("generated", "nestedProtocolFormats.scala")) shouldEqual false
  }

  def interfaceGenerateMessages = {
    val schema = JsonParser.Document.parse(generateArgDocExample)
    val gen = new CodecCodeGen(codecParents, instantiateJavaLazy, javaOption, scalaArray, formatsForType, schema :: Nil, scalaVersion)
    val code = gen generate schema

    code.head._2.unindent should equalLines("""/**
        | * This code is generated using [[https://www.scala-sbt.org/contraband]].
        | */
        |
        |// DO NOT EDIT MANUALLY
        |package generated
        |
        |import _root_.sjsonnew.{ deserializationError, serializationError, Builder, JsonFormat, Unbuilder }
        |
        |trait GenerateArgDocExampleFormats {
        |  implicit lazy val generateArgDocExampleFormat: JsonFormat[_root_.generateArgDocExample] = new JsonFormat[_root_.generateArgDocExample] {
        |    override def read[J](__jsOpt: Option[J], unbuilder: Unbuilder[J]): _root_.generateArgDocExample = {
        |      deserializationError("No known implementation of generateArgDocExample.")
        |    }
        |    override def write[J](obj: _root_.generateArgDocExample, builder: Builder[J]): Unit = {
        |      serializationError("No known implementation of generateArgDocExample.")
        |    }
        |  }
        |}""".stripMargin.unindent)
  }

  override def recordGenerateSimple = {
    val gen = new CodecCodeGen(codecParents, instantiateJavaLazy, javaOption, scalaArray, formatsForType, Nil, scalaVersion)
    val record = JsonParser.ObjectTypeDefinition.parse(simpleRecordExample)
    val code = gen generate record

    code.head._2.unindent should equalLines("""/**
        | * This code is generated using [[https://www.scala-sbt.org/contraband]].
        | */
        |
        |// DO NOT EDIT MANUALLY
        |package generated
        |
        |import _root_.sjsonnew.{ Unbuilder, Builder, JsonFormat, deserializationError }
        |
        |trait SimpleRecordExampleFormats { self: sjsonnew.BasicJsonProtocol =>
        |  implicit lazy val simpleRecordExampleFormat: JsonFormat[_root_.simpleRecordExample] = new JsonFormat[_root_.simpleRecordExample] {
        |    override def read[J](__jsOpt: Option[J], unbuilder: Unbuilder[J]): _root_.simpleRecordExample = {
        |      __jsOpt match {
        |        case Some(__js) =>
        |          unbuilder.beginObject(__js)
        |          val field = unbuilder.readField[java.net.URL]("field")
        |          unbuilder.endObject()
        |          _root_.simpleRecordExample(field)
        |        case None =>
        |          deserializationError("Expected JsObject but found None")
        |      }
        |    }
        |    override def write[J](obj: _root_.simpleRecordExample, builder: Builder[J]): Unit = {
        |      builder.beginObject()
        |      builder.addField("field", obj.field)
        |      builder.endObject()
        |    }
        |  }
        |}""".stripMargin.unindent)
  }

  override def recordGrowZeroToOneField = {
    val gen = new CodecCodeGen(codecParents, instantiateJavaLazy, javaOption, scalaArray, formatsForType, Nil, scalaVersion)
    val record = JsonParser.ObjectTypeDefinition.parse(growableAddOneFieldExample)
    val code = gen generate record

    code.head._2.unindent should equalLines("""/**
        | * This code is generated using [[https://www.scala-sbt.org/contraband]].
        | */
        |
        |// DO NOT EDIT MANUALLY
        |package generated
        |
        |import _root_.sjsonnew.{ Unbuilder, Builder, JsonFormat, deserializationError }
        |
        |trait GrowableAddOneFieldFormats { self: sjsonnew.BasicJsonProtocol =>
        |  implicit lazy val growableAddOneFieldFormat: JsonFormat[_root_.growableAddOneField] = new JsonFormat[_root_.growableAddOneField] {
        |    override def read[J](__jsOpt: Option[J], unbuilder: Unbuilder[J]): _root_.growableAddOneField = {
        |      __jsOpt match {
        |        case Some(__js) =>
        |          unbuilder.beginObject(__js)
        |          val field = unbuilder.readField[Int]("field")
        |          unbuilder.endObject()
        |          _root_.growableAddOneField(field)
        |        case None =>
        |          deserializationError("Expected JsObject but found None")
        |      }
        |    }
        |    override def write[J](obj: _root_.growableAddOneField, builder: Builder[J]): Unit = {
        |      builder.beginObject()
        |      builder.addField("field", obj.field)
        |      builder.endObject()
        |    }
        |  }
        |}""".stripMargin.unindent)
  }

  override def recordGrowZeroToOneToTwoFields: Unit = {
    val gen = new CodecCodeGen(codecParents, instantiateJavaLazy, javaOption, scalaArray, formatsForType, Nil, scalaVersion)
    val record = JsonParser.ObjectTypeDefinition.parse(growableZeroToOneToTwoFieldsExample)
    val code = gen generate record

    code.head._2.unindent should equalLines("""/**
        | * This code is generated using [[https://www.scala-sbt.org/contraband]].
        | */
        |
        |// DO NOT EDIT MANUALLY
        |package generated
        |
        |import _root_.sjsonnew.{ Unbuilder, Builder, JsonFormat, deserializationError }
        |
        |trait FooFormats { self: sjsonnew.BasicJsonProtocol =>
        |  implicit lazy val FooFormat: JsonFormat[_root_.Foo] = new JsonFormat[_root_.Foo] {
        |    override def read[J](__jsOpt: Option[J], unbuilder: Unbuilder[J]): _root_.Foo = {
        |      __jsOpt match {
        |        case Some(__js) =>
        |          unbuilder.beginObject(__js)
        |          val x = unbuilder.readField[Option[Int]]("x")
        |          val y = unbuilder.readField[Vector[Int]]("y")
        |          unbuilder.endObject()
        |          _root_.Foo(x, y)
        |        case None =>
        |          deserializationError("Expected JsObject but found None")
        |      }
        |    }
        |    override def write[J](obj: _root_.Foo, builder: Builder[J]): Unit = {
        |      builder.beginObject()
        |      builder.addField("x", obj.x)
        |      builder.addField("y", obj.y)
        |      builder.endObject()
        |    }
        |  }
        |}""".stripMargin.unindent)
  }

  override def recordPrimitives: Unit = {
    val gen = new CodecCodeGen(codecParents, instantiateJavaLazy, javaOption, scalaArray, formatsForType, Nil, scalaVersion)
    val record = JsonParser.ObjectTypeDefinition.parse(primitiveTypesExample2)
    val code = gen generate record

    // println(code)

    code.head._2.unindent should equalLines("""/**
 * This code is generated using [[https://www.scala-sbt.org/contraband]].
 */

// DO NOT EDIT MANUALLY
package generated
import _root_.sjsonnew.{ Unbuilder, Builder, JsonFormat, deserializationError }
trait PrimitiveTypesExample2Formats { self: sjsonnew.BasicJsonProtocol =>
implicit lazy val primitiveTypesExample2Format: JsonFormat[_root_.primitiveTypesExample2] = new JsonFormat[_root_.primitiveTypesExample2] {
  override def read[J](__jsOpt: Option[J], unbuilder: Unbuilder[J]): _root_.primitiveTypesExample2 = {
    __jsOpt match {
      case Some(__js) =>
      unbuilder.beginObject(__js)
      val smallBoolean = unbuilder.readField[Boolean]("smallBoolean")
      val bigBoolean = unbuilder.readField[Boolean]("bigBoolean")
      unbuilder.endObject()
      _root_.primitiveTypesExample2(smallBoolean, bigBoolean)
      case None =>
      deserializationError("Expected JsObject but found None")
    }
  }
  override def write[J](obj: _root_.primitiveTypesExample2, builder: Builder[J]): Unit = {
    builder.beginObject()
    builder.addField("smallBoolean", obj.smallBoolean)
    builder.addField("bigBoolean", obj.bigBoolean)
    builder.endObject()
  }
}
}""".stripMargin.unindent)
  }

  override def recordWithModifier: Unit = {}

  override def schemaGenerateTypeReferences = {
    val schema = JsonParser.Document.parse(primitiveTypesExample)
    val gen = new CodecCodeGen(codecParents, instantiateJavaLazy, javaOption, scalaArray, formatsForType, schema :: Nil, scalaVersion)
    val code = gen generate schema

    code.head._2.unindent should equalLines("""/**
        | * This code is generated using [[https://www.scala-sbt.org/contraband]].
        | */
        |
        |// DO NOT EDIT MANUALLY
        |
        |import _root_.sjsonnew.{ Unbuilder, Builder, JsonFormat, deserializationError }
        |
        |trait PrimitiveTypesExampleFormats { self: sjsonnew.BasicJsonProtocol =>
        |  implicit lazy val primitiveTypesExampleFormat: JsonFormat[_root_.primitiveTypesExample] = new JsonFormat[_root_.primitiveTypesExample] {
        |    override def read[J](__jsOpt: Option[J], unbuilder: Unbuilder[J]): _root_.primitiveTypesExample = {
        |      __jsOpt match {
        |        case Some(__js) =>
        |          unbuilder.beginObject(__js)
        |          val simpleInteger = unbuilder.readField[Int]("simpleInteger")
        |          val lazyInteger = unbuilder.readField[Int]("lazyInteger")
        |          val arrayInteger = unbuilder.readField[Vector[Int]]("arrayInteger")
        |          val optionInteger = unbuilder.readField[Option[Int]]("optionInteger")
        |          val lazyArrayInteger = unbuilder.readField[Vector[Int]]("lazyArrayInteger")
        |          val lazyOptionInteger = unbuilder.readField[Option[Int]]("lazyOptionInteger")
        |          unbuilder.endObject()
        |          _root_.primitiveTypesExample(simpleInteger, lazyInteger, arrayInteger, optionInteger, lazyArrayInteger, lazyOptionInteger)
        |        case None =>
        |          deserializationError("Expected JsObject but found None")
        |      }
        |    }
        |
        |    override def write[J](obj: _root_.primitiveTypesExample, builder: Builder[J]): Unit = {
        |      builder.beginObject()
        |      builder.addField("simpleInteger", obj.simpleInteger)
        |      builder.addField("lazyInteger", obj.lazyInteger)
        |      builder.addField("arrayInteger", obj.arrayInteger)
        |      builder.addField("optionInteger", obj.optionInteger)
        |      builder.addField("lazyArrayInteger", obj.lazyArrayInteger)
        |      builder.addField("lazyOptionInteger", obj.lazyOptionInteger)
        |      builder.endObject()
        |    }
        |  }
        |}""".stripMargin.unindent)
  }

  override def schemaGenerateTypeReferencesNoLazy = {
    val schema = JsonParser.Document.parse(primitiveTypesNoLazyExample)
    val gen = new CodecCodeGen(codecParents, instantiateJavaLazy, javaOption, scalaArray, formatsForType, schema :: Nil, scalaVersion)
    val code = gen generate schema

    code.head._2.unindent should equalLines("""/**
        | * This code is generated using [[https://www.scala-sbt.org/contraband]].
        | */
        |
        |// DO NOT EDIT MANUALLY
        |
        |import _root_.sjsonnew.{ Unbuilder, Builder, JsonFormat, deserializationError }
        |
        |trait PrimitiveTypesNoLazyExampleFormats { self: sjsonnew.BasicJsonProtocol =>
        |  implicit lazy val primitiveTypesNoLazyExampleFormat: JsonFormat[_root_.primitiveTypesNoLazyExample] = new JsonFormat[_root_.primitiveTypesNoLazyExample] {
        |    override def read[J](__jsOpt: Option[J], unbuilder: Unbuilder[J]): _root_.primitiveTypesNoLazyExample = {
        |      __jsOpt match {
        |        case Some(__js) =>
        |          unbuilder.beginObject(__js)
        |          val simpleInteger = unbuilder.readField[Int]("simpleInteger")
        |          val arrayInteger = unbuilder.readField[Vector[Int]]("arrayInteger")
        |          unbuilder.endObject()
        |          _root_.primitiveTypesNoLazyExample(simpleInteger, arrayInteger)
        |        case None =>
        |          deserializationError("Expected JsObject but found None")
        |      }
        |    }
        |    override def write[J](obj: _root_.primitiveTypesNoLazyExample, builder: Builder[J]): Unit = {
        |      builder.beginObject()
        |      builder.addField("simpleInteger", obj.simpleInteger)
        |      builder.addField("arrayInteger", obj.arrayInteger)
        |      builder.endObject()
        |    }
        |  }
        |}""".stripMargin.unindent)
  }

  override def schemaGenerateComplete = {
    val schema = JsonParser.Document.parse(completeExample)
    val gen = new CodecCodeGen(codecParents, instantiateJavaLazy, javaOption, scalaArray, formatsForType, schema :: Nil, scalaVersion)
    val code = gen generate schema
    code.values.mkString.unindent should equalLines(completeExampleCodeCodec.unindent)
  }

  override def schemaGenerateCompletePlusIndent = {
    val schema = JsonParser.Document.parse(completeExample)
    val gen = new CodecCodeGen(codecParents, instantiateJavaLazy, javaOption, scalaArray, formatsForType, schema :: Nil, scalaVersion)
    val code = gen generate schema

    code.values.mkString.withoutEmptyLines should equalLines(completeExampleCodeCodec.withoutEmptyLines)
  }

  "The full codec object" should "include the codec of all protocol defined in the schema" in {
    val schema = JsonParser.Document.parse(s"""{
                                 |  "types": [
                                 |    {
                                 |      "name": "Greeting",
                                 |      "target": "Java",
                                 |      "type": "interface"
                                 |    }
                                 |  ]
                                 |}""".stripMargin)
    val gen = new CodecCodeGen(codecParents, instantiateJavaLazy, javaOption, scalaArray, formatsForType, schema :: Nil, scalaVersion)
    val code = gen generate schema

    code.head._2.unindent should equalLines("""/**
        | * This code is generated using [[https://www.scala-sbt.org/contraband]].
        | */
        |
        |// DO NOT EDIT MANUALLY
        |
        |import _root_.sjsonnew.{ deserializationError, serializationError, Builder, JsonFormat, Unbuilder }
        | 
        |trait GreetingFormats {
        |  implicit lazy val GreetingFormat: JsonFormat[_root_.Greeting] = new JsonFormat[_root_.Greeting] {
        |    override def read[J](__jsOpt: Option[J], unbuilder: Unbuilder[J]): _root_.Greeting = {
        |      deserializationError("No known implementation of Greeting.")
        |    }
        |    override def write[J](obj: _root_.Greeting, builder: Builder[J]): Unit = {
        |      serializationError("No known implementation of Greeting.")
        |    }
        |  }
        |}""".stripMargin.unindent)
  }
}
