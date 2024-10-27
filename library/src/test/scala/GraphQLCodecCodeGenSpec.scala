package sbt.contraband

import verify._
import java.io.File
import parser.SchemaParser
import ast.{ NamedType, Type }
import GraphQLExample._
import scala.util.Success

object GraphQLCodecCodeGenSpec extends BasicTestSuite with EqualLines {
  test("generate(Interface) should generate a codec for an interface") {
    val Success(ast) = SchemaParser.parse(intfExample)
    val code = mkCodecCodeGen.generate(ast)
    assertEquals(
      code(new File("generated", "InterfaceExampleFormats.scala")).stripSpace,
      """/**
        | * This code is generated using [[https://www.scala-sbt.org/contraband]].
        | */
        |
        |// DO NOT EDIT MANUALLY
        |package generated
        |
        |import _root_.sjsonnew.JsonFormat
        |
        |trait InterfaceExampleFormats { self: sjsonnew.BasicJsonProtocol & generated.ChildTypeFormats =>
        |  implicit lazy val InterfaceExampleFormat: JsonFormat[com.example.InterfaceExample] = flatUnionFormat1[com.example.InterfaceExample, com.example.ChildType]("type")
        |}""".stripMargin.stripSpace
    )

    assertEquals(
      code(new File("generated", "ChildTypeFormats.scala")).stripSpace,
      """/**
        | * This code is generated using [[https://www.scala-sbt.org/contraband]].
        | */
        |
        |// DO NOT EDIT MANUALLY
        |package generated
        |
        |import _root_.sjsonnew.{ Unbuilder, Builder, JsonFormat, deserializationError }
        |
        |trait ChildTypeFormats { self: sjsonnew.BasicJsonProtocol =>
        |  implicit lazy val ChildTypeFormat: JsonFormat[com.example.ChildType] = new JsonFormat[com.example.ChildType] {
        |    override def read[J](__jsOpt: Option[J], unbuilder: Unbuilder[J]): com.example.ChildType = {
        |      __jsOpt match {
        |        case Some(__js) =>
        |          unbuilder.beginObject(__js)
        |          val name = unbuilder.readField[Option[String]]("name")
        |          val field = unbuilder.readField[Option[Int]]("field")
        |          unbuilder.endObject()
        |          com.example.ChildType(name, field)
        |        case None =>
        |          deserializationError("Expected JsObject but found None")
        |      }
        |    }
        |    override def write[J](obj: com.example.ChildType, builder: Builder[J]): Unit = {
        |      builder.beginObject()
        |      builder.addField("name", obj.name)
        |      builder.addField("field", obj.field)
        |      builder.endObject()
        |    }
        |  }
        |}""".stripMargin.stripSpace
    )
  }

  test("generate a codec for a two-level hierarchy of interfaces") {
    val Success(ast) = SchemaParser.parse(twoLevelIntfExample)
    val code = mkCodecCodeGen.generate(ast)

    assertEquals(
      code(new File("generated", "InterfaceExampleFormats.scala")).stripSpace,
      """/**
        | * This code is generated using [[https://www.scala-sbt.org/contraband]].
        | */
        |
        |// DO NOT EDIT MANUALLY
        |package generated
        |
        |import _root_.sjsonnew.JsonFormat
        |
        |trait InterfaceExampleFormats { self: sjsonnew.BasicJsonProtocol & generated.ChildTypeFormats =>
        |  implicit lazy val InterfaceExampleFormat: JsonFormat[com.example.InterfaceExample] = flatUnionFormat1[com.example.InterfaceExample, com.example.ChildType]("type")
        |}""".stripMargin.stripSpace
    )

    assert(!code.contains(new File("generated", "MiddleInterfaceFormats.scala")))

    assertEquals(
      code(new File("generated", "ChildTypeFormats.scala")).stripSpace,
      """/**
        | * This code is generated using [[https://www.scala-sbt.org/contraband]].
        | */
        |
        |// DO NOT EDIT MANUALLY
        |package generated
        |
        |import _root_.sjsonnew.{ Unbuilder, Builder, JsonFormat, deserializationError }
        |
        |trait ChildTypeFormats { self: sjsonnew.BasicJsonProtocol =>
        |  implicit lazy val ChildTypeFormat: JsonFormat[com.example.ChildType] = new JsonFormat[com.example.ChildType] {
        |    override def read[J](__jsOpt: Option[J], unbuilder: Unbuilder[J]): com.example.ChildType = {
        |      __jsOpt match {
        |        case Some(__js) =>
        |          unbuilder.beginObject(__js)
        |          val name = unbuilder.readField[Option[String]]("name")
        |          val field = unbuilder.readField[Option[Int]]("field")
        |          unbuilder.endObject()
        |          com.example.ChildType(name, field)
        |        case None =>
        |          deserializationError("Expected JsObject but found None")
        |      }
        |    }
        |    override def write[J](obj: com.example.ChildType, builder: Builder[J]): Unit = {
        |      builder.beginObject()
        |      builder.addField("name", obj.name)
        |      builder.addField("field", obj.field)
        |      builder.endObject()
        |    }
        |  }
        |}""".stripMargin.stripSpace
    )
  }

  test("generate a codec for an interfaces, whose child contains custom field") {
    val Success(ast) = SchemaParser.parse(intfExampleWithEmbed)
    val code = mkCodecCodeGen.generate(ast)

    assertEquals(
      code(new File("generated", "InterfaceExampleFormats.scala")).stripSpace,
      """/**
        | * This code is generated using [[https://www.scala-sbt.org/contraband]].
        | */
        |
        |// DO NOT EDIT MANUALLY
        |package generated
        |
        |import _root_.sjsonnew.JsonFormat
        |
        |trait InterfaceExampleFormats { self: generated.TestItemDetailFormats & com.example.StatusFormats & sjsonnew.BasicJsonProtocol & generated.ChildTypeFormats =>
        |  implicit lazy val InterfaceExampleFormat: JsonFormat[com.example.InterfaceExample] = flatUnionFormat1[com.example.InterfaceExample, com.example.ChildType]("type")
        |}""".stripMargin.stripSpace
    )
  }

  val codecParents = List("sjsonnew.BasicJsonProtocol")
  val instantiateJavaLazy = (s: String) => s"mkLazy($s)"
  val javaOption = "com.example.Option"
  val scalaArray = "Vector"
  val formatsForType: ast.Type => List[String] =
    CodecCodeGen.extensibleFormatsForType {
      case NamedType(List("com", "example", "Status"), _) =>
        "com.example.StatusFormats" :: Nil
      case other =>
        CodecCodeGen.formatsForType(other)
    }
  val scalaVersion = "3.5.1"

  def mkCodecCodeGen: CodecCodeGen =
    new CodecCodeGen(
      codecParents = codecParents,
      instantiateJavaLazy = instantiateJavaLazy,
      javaOption = javaOption,
      scalaArray = scalaArray,
      formatsForType = formatsForType,
      includedSchemas = Nil,
      scalaVersion = scalaVersion,
    )
}
