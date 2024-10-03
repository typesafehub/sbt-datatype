package sbt.contraband

import org.scalatest._
import java.io.File
import parser.SchemaParser
import GraphQLExample._
import scala.util.Success
import scala.collection.immutable._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class GraphQLMixedCodeGenSpec extends AnyFlatSpec with Matchers with Inside with EqualLines {
  "generate(Record)" should "handle mixed Java-Scala inheritance" in {
    val Success(ast) = SchemaParser.parse(mixedExample)
    // println(ast)
    val gen = new MixedCodeGen(
      javaLazy,
      CodeGen.javaOptional,
      CodeGen.instantiateJavaOptional,
      scalaArray,
      genFileName,
      scalaSealProtocols = true,
      scalaPrivateConstructor = true,
      wrapOption = true
    )
    val code = gen.generate(ast)

    code.mapValues(_.unindent).toMap should equalMapLines(
      ListMap(
        new File("com/example/Greeting.java") ->
          """/**
 * This code is generated using [[https://www.scala-sbt.org/contraband]].
 */

// DO NOT EDIT MANUALLY
package com.example;
public abstract class Greeting implements java.io.Serializable {


    private String message;
    private java.util.Optional<String> s;
    protected Greeting(String _message) {
        super();
        message = _message;
        s = java.util.Optional.<String>ofNullable("1");
    }
    protected Greeting(String _message, java.util.Optional<String> _s) {
        super();
        message = _message;
        s = _s;
    }
    protected Greeting(String _message, String _s) {
        super();
        message = _message;
        s = java.util.Optional.<String>ofNullable(_s);
    }
    public String message() {
        return this.message;
    }
    public java.util.Optional<String> s() {
        return this.s;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof Greeting)) {
            return false;
        } else {
            Greeting o = (Greeting)obj;
            return this.message().equals(o.message()) && this.s().equals(o.s());
        }
    }
    public int hashCode() {
        return 37 * (37 * (37 * (17 + "com.example.Greeting".hashCode()) + message().hashCode()) + s().hashCode());
    }
    public String toString() {
        return "Greeting("  + "message: " + message() + ", " + "s: " + s() + ")";
    }
}
""".stripMargin.unindent,
        new File("output.scala") ->
          """/**
 * This code is generated using [[https://www.scala-sbt.org/contraband]].
 */

// DO NOT EDIT MANUALLY
package com.example
final class SimpleGreeting private (
  message: String,
  s: java.util.Optional[String]) extends com.example.Greeting(message, s) with Serializable {
  private def this(message: String) = this(message, java.util.Optional.ofNullable[String]("1"))
  override def equals(o: Any): Boolean = this.eq(o.asInstanceOf[AnyRef]) || (o match {
    case x: SimpleGreeting => this.message.equals(x.message) && this.s.equals(x.s)
    case _ => false
  })
  override def hashCode: Int = {
    37 * (37 * (37 * (17 + "com.example.SimpleGreeting".##) +  message.hashCode()) +  s.hashCode())
  }
  override def toString: String = {
    "SimpleGreeting(" + message + ", " + s + ")"
  }
  private def copy(message: String = message, s: java.util.Optional[String] = s): SimpleGreeting = {
    new SimpleGreeting(message, s)
  }
  def withMessage(message: String): SimpleGreeting = {
    copy(message = message)
  }
  def withS(s: java.util.Optional[String]): SimpleGreeting = {
    copy(s = s)
  }
  def withS(s: String): SimpleGreeting = {
    copy(s = java.util.Optional.ofNullable[String](s))
  }
}
object SimpleGreeting {
  def apply(message: String): SimpleGreeting = new SimpleGreeting(message)
  def apply(message: String, s: java.util.Optional[String]): SimpleGreeting = new SimpleGreeting(message, s)
  def apply(message: String, s: String): SimpleGreeting = new SimpleGreeting(message, java.util.Optional.ofNullable[String](s))
}
""".stripMargin.unindent
      )
    )
  }

  val javaLazy = "com.example.Lazy"
  val outputFile = new File("output.scala")
  val scalaArray = "Vector"
  val genFileName = (_: Any) => outputFile
}
