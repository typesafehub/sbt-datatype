package sbt.contraband
package parser

import org.parboiled2._
import CharPredicate.{ HexDigit, Digit19, AlphaNum }
import scala.util.{ Failure, Success, Try }

trait Tokens extends StringBuilding with PositionTracking { this: Parser & Ignored =>

  def Token = rule { Punctuator | Name | NumberValue | StringValue }

  val PunctuatorChar = CharPredicate("!$():=@[]{|}")

  def Punctuator = rule { PunctuatorChar | Ellipsis }

  def Ellipsis = rule { quiet(str("...") ~ IgnoredNoComment.*) }

  val NameFirstChar = CharPredicate.Alpha ++ '_'

  val NameChar = NameFirstChar ++ CharPredicate.Digit

  def NameStrict = rule { capture(NameFirstChar ~ NameChar.*) ~ IgnoredNoComment.* }

  def Name = rule { Ignored.* ~ NameStrict }

  def DotNames = rule { Name ~ ('.' ~ Name).* ~> ((n, ns) => n :: ns.toList) }

  def RawNames = rule { atomic("raw\"" ~ capture(Characters) ~ "\"") ~> ((n: String) => List(n)) }

  def NumberValue = rule {
    atomic(Comments ~ trackPos ~ IntegerValuePart ~ FloatValuePart.? ~ IgnoredNoComment.*) ~>
      ((comment, pos, intPart, floatPart) =>
        floatPart map (f => ast.BigDecimalValue(BigDecimal(intPart + f), comment, Some(pos))) getOrElse
          ast.BigIntValue(BigInt(intPart), comment, Some(pos))
      )
  }

  def FloatValuePart = rule { atomic(capture(FractionalPart ~ ExponentPart.? | ExponentPart)) }

  def FractionalPart = rule { '.' ~ Digit.+ }

  def IntegerValuePart = rule { capture(NegativeSign.? ~ IntegerPart) }

  def IntegerPart = rule { ch('0') | NonZeroDigit ~ Digit.* }

  def ExponentPart = rule { ExponentIndicator ~ Sign.? ~ Digit.+ }

  def ExponentIndicator = rule { ch('e') | ch('E') }

  def Sign = rule { ch('-') | '+' }

  val NegativeSign = '-'

  val NonZeroDigit = Digit19

  def Digit = rule { ch('0') | NonZeroDigit }

  def StringValue = rule {
    atomic(
      Comments ~ trackPos ~ '"' ~ clearSB() ~ Characters ~ '"' ~ push(sb.toString) ~ IgnoredNoComment.* ~> ((comment, pos, s) =>
        ast.StringValue(s, comment, Some(pos))
      )
    )
  }

  def Characters = rule { (NormalChar | '\\' ~ EscapedChar).* }

  val QuoteBackslash = CharPredicate("\"\\")

  def NormalChar = rule { !(QuoteBackslash | LineTerminator) ~ ANY ~ appendSB() }

  def EscapedChar = rule {
    QuoteBackslash ~ appendSB() |
      'b' ~ appendSB('\b') |
      'f' ~ appendSB('\f') |
      'n' ~ appendSB('\n') |
      'r' ~ appendSB('\r') |
      't' ~ appendSB('\t') |
      Unicode ~> { code => sb.append(code.asInstanceOf[Char]); () }
  }

  def Unicode = rule { 'u' ~ capture(4 times HexDigit) ~> (Integer.parseInt(_, 16)) }

  def Keyword(s: String) = rule { atomic(Ignored.* ~ s ~ !NameChar ~ IgnoredNoComment.*) }
}

trait Ignored extends PositionTracking { this: Parser =>

  val WhiteSpace = CharPredicate("\u0009\u0020")

  def CRLF = rule { '\u000D' ~ '\u000A' }

  val LineTerminator = CharPredicate("\u000A")

  val UnicodeBOM = CharPredicate('\uFEFF')

  def Ignored = rule { quiet(UnicodeBOM | WhiteSpace | (CRLF | LineTerminator) ~ trackNewLine | Comment | ',') }

  def IgnoredNoComment = rule { quiet(UnicodeBOM | WhiteSpace | (CRLF | LineTerminator) ~ trackNewLine | ',') }

  def Comments = rule { (DocComment | CommentCap).* ~ Ignored.* ~> (_.toList) }

  def ExtraComments = rule {
    (ExtraIntfComment | ToStringImplComment | CompanionExtraIntfComment | CompanionExtraComment | ExtraComment | DocComment | CommentCap).* ~ Ignored.* ~> (_.toList)
  }

  def CommentCap = rule {
    trackPos ~ "#" ~ capture(CommentChar.*) ~ IgnoredNoComment.* ~> ((pos, comment) => ast.CommentLine(comment, Some(pos)))
  }

  def DocComment = rule {
    trackPos ~ "##" ~ capture(CommentChar.*) ~ IgnoredNoComment.* ~> ((pos, comment) => ast.DocComment(comment, Some(pos)))
  }

  def ExtraComment = rule {
    trackPos ~ "#x" ~ capture(CommentChar.*) ~ IgnoredNoComment.* ~> ((pos, comment) => ast.ExtraComment(comment, Some(pos)))
  }

  def ExtraIntfComment = rule {
    trackPos ~ "#xinterface" ~ capture(CommentChar.*) ~ IgnoredNoComment.* ~> ((pos, comment) =>
      ast.ExtraIntfComment(comment.trim, Some(pos))
    )
  }

  def ToStringImplComment = rule {
    trackPos ~ "#xtostring" ~ capture(CommentChar.*) ~ IgnoredNoComment.* ~> ((pos, comment) =>
      ast.ToStringImplComment(comment.trim, Some(pos))
    )
  }

  def CompanionExtraIntfComment = rule {
    trackPos ~ "#xcompanioninterface" ~ capture(CommentChar.*) ~ IgnoredNoComment.* ~> ((pos, comment) =>
      ast.CompanionExtraIntfComment(comment.trim, Some(pos))
    )
  }

  def CompanionExtraComment = rule {
    trackPos ~ "#xcompanion" ~ capture(CommentChar.*) ~ IgnoredNoComment.* ~> ((pos, comment) =>
      ast.CompanionExtraComment(comment.trim, Some(pos))
    )
  }

  def Comment = rule { "#" ~ CommentChar.* }

  def CommentChar = rule { !(CRLF | LineTerminator) ~ ANY }

  def ws(char: Char): Rule0 = rule { quiet(Ignored.* ~ ch(char) ~ Ignored.*) }

  def wsNoComment(char: Char): Rule0 = rule { quiet(Ignored.* ~ ch(char) ~ IgnoredNoComment.*) }

  def ws(s: String): Rule0 = rule { quiet(Ignored.* ~ str(s) ~ Ignored.*) }

}

trait Document {
  this: Parser & Values & Operations & Types & Tokens & Ignored & Directives & TypeSystemDefinitions =>

  def `package` = rule { Keyword("package") }

  def Document = rule {
    PackageDecl.? ~ IgnoredNoComment.* ~ trackPos ~ Definition.+ ~ IgnoredNoComment.* ~ Comments ~ EOI ~>
      ((pkg, pos, d, comments) => ast.Document(pkg, d.toList, Nil, comments, Some(pos)))
  }

  def PackageDecl = rule {
    Comments ~ trackPos ~ `package` ~ DotNames ~ (Directives.? ~> (_ getOrElse Nil)) ~>
      ((comment, pos, names, dirs) => ast.PackageDecl(names, dirs, comment, Some(pos)))
  }

  // def InputDocument = rule { IgnoredNoComment.* ~ ValueConst ~ Ignored.* ~ EOI }

  def Definition = rule { /*OperationDefinition | FragmentDefinition |*/
    TypeSystemDefinition
  }

}

trait TypeSystemDefinitions {
  this: Parser & Tokens & Ignored & Directives & Types & Operations & Values =>

  def scalar = rule { Keyword("scalar") }
  def `type` = rule { Keyword("type") }
  def interface = rule { Keyword("interface") }
  def union = rule { Keyword("union") }
  def `enum` = rule { Keyword("enum") }
  def inputType = rule { Keyword("input") }
  def implements = rule { Keyword("implements") }
  def extend = rule { Keyword("extend") }
  def directive = rule { Keyword("directive") }
  def schema = rule { Keyword("schema") }

  def TypeSystemDefinition = rule {
    // SchemaDefinition |
    TypeDefinition
    // | TypeExtensionDefinition | DirectiveDefinition
  }

  def TypeDefinition = rule {
    // ScalarTypeDefinition |
    ObjectTypeDefinition |
      InterfaceTypeDefinition |
      EnumTypeDefinition // |
    // UnionTypeDefinition |
    // InputObjectTypeDefinition
  }

  // def ScalarTypeDefinition = rule {
  //   Comments ~ trackPos ~ scalar ~ Name ~ (Directives.? ~> (_ getOrElse Nil)) ~> (
  //     (comment, pos, name, dirs) => ast.ScalarTypeDefinition(name, dirs, comment, Some(pos)))
  // }

  // TODO: clarify https://github.com/facebook/graphql/pull/90/files#r64149353
  def ObjectTypeDefinition = rule {
    Comments ~ trackPos ~ `type` ~ Name ~ (ImplementsInterfaces.? ~> (_ getOrElse Nil)) ~
      (Directives.? ~> (_ getOrElse Nil)) ~ wsNoComment('{') ~ FieldDefinition.* ~ ExtraComments ~ wsNoComment('}') ~> (
        (comment, pos, name, interfaces, dirs, fields, tc) =>
          ast.ObjectTypeDefinition(name, None, interfaces, fields.toList, dirs, comment, tc, Some(pos))
      )
  }

  def ImplementsInterfaces = rule { implements ~ NamedType.+ ~> (_.toList) }

  def FieldDefinition = rule {
    Comments ~ trackPos ~ Name ~ (ArgumentsDefinition.? ~> (_ getOrElse Nil)) ~ ws(
      ':'
    ) ~ Type ~ DefaultValue.? ~ (Directives.? ~> (_ getOrElse Nil)) ~> ((comment, pos, name, args, fieldType, default, dirs) =>
      ast.FieldDefinition(name, fieldType, args, default, dirs, comment, Some(pos))
    )
  }

  def ArgumentsDefinition = rule { wsNoComment('(') ~ InputValueDefinition.+ ~ wsNoComment(')') ~> (_.toList) }

  def InputValueDefinition = rule {
    Comments ~ trackPos ~ Name ~ ws(':') ~ Type ~ DefaultValue.? ~ (Directives.? ~> (_ getOrElse Nil)) ~> (
      (comment, pos, name, valueType, default, dirs) => ast.InputValueDefinition(name, valueType, default, dirs, comment, Some(pos))
    )
  }

  def InterfaceTypeDefinition = rule {
    // Changed FieldDefinition.+ to FieldDefinition.*
    Comments ~ trackPos ~ interface ~ Name ~ (ImplementsInterfaces.? ~> (_ getOrElse Nil)) ~
      (Directives.? ~> (_ getOrElse Nil)) ~ wsNoComment('{') ~ FieldDefinition.* ~ ExtraComments ~ wsNoComment('}') ~> (
        (comment, pos, name, interfaces, dirs, fields, tc) =>
          ast.InterfaceTypeDefinition(name, None, interfaces, fields.toList, dirs, comment, tc, Some(pos))
      )
  }

  // def UnionTypeDefinition = rule {
  //   Comments ~ trackPos ~ union ~ Name ~ (Directives.? ~> (_ getOrElse Nil)) ~ wsNoComment('=') ~ UnionMembers ~> (
  //       (comment, pos, name, dirs, members) => ast.UnionTypeDefinition(name, members, dirs, comment, Some(pos)))
  // }

  // def UnionMembers = rule { NamedType.+(ws('|')) ~> (_.toList) }

  def EnumTypeDefinition = rule {
    Comments ~ trackPos ~ `enum` ~ Name ~ (Directives.? ~> (_ getOrElse Nil)) ~ wsNoComment(
      '{'
    ) ~ EnumValueDefinition.+ ~ ExtraComments ~ wsNoComment('}') ~> ((comment, pos, name, dirs, values, tc) =>
      ast.EnumTypeDefinition(name, None, values.toList, dirs, comment, tc, Some(pos))
    )
  }

  def EnumValueDefinition = rule {
    EnumValue ~ (Directives.? ~> (_ getOrElse Nil)) ~> ((v, dirs) => ast.EnumValueDefinition(v.value, dirs, v.comments, v.position))
  }

  // // TODO: clarify https://github.com/facebook/graphql/pull/90/files#r64149353
  // def InputObjectTypeDefinition = rule {
  //   Comments ~ trackPos ~ inputType ~ Name ~ (Directives.? ~> (_ getOrElse Nil)) ~ wsNoComment('{') ~ InputValueDefinition.* ~ Comments ~ wsNoComment('}') ~> (
  //     (comment, pos, name, dirs, fields, tc) => ast.InputObjectTypeDefinition(name, fields.toList, dirs, comment, tc, Some(pos)))
  // }

  // def TypeExtensionDefinition = rule {
  //   Comments ~ trackPos ~ extend ~ ObjectTypeDefinition ~> (
  //     (comment, pos, definition) => ast.TypeExtensionDefinition(definition, comment, Some(pos)))
  // }

  // def DirectiveDefinition = rule {
  //   Comments ~ trackPos ~ directive ~ '@' ~ NameStrict ~ (ArgumentsDefinition.? ~> (_ getOrElse Nil)) ~ on ~ DirectiveLocations ~> (
  //     (comment, pos, name, args, locations) => ast.DirectiveDefinition(name, args, locations, comment, Some(pos)))
  // }

  // def DirectiveLocations = rule { DirectiveLocation.+(wsNoComment('|')) ~> (_.toList) }

  // def DirectiveLocation = rule { Comments ~ trackPos ~ Name ~> ((comment, pos, name) => ast.DirectiveLocation(name, comment, Some(pos))) }

  // def SchemaDefinition = rule {
  //   Comments ~ trackPos ~ schema ~ (Directives.? ~> (_ getOrElse Nil)) ~ wsNoComment('{') ~ OperationTypeDefinition.+ ~ Comments ~ wsNoComment('}') ~> (
  //     (comment, pos, dirs, ops, tc) => ast.SchemaDefinition(ops.toList, dirs, comment, tc, Some(pos)))
  // }

  // def OperationTypeDefinition = rule {
  //   Comments ~ trackPos ~ OperationType ~ ws(':') ~ NamedType ~> (
  //     (comment, pos, opType, tpe) => ast.OperationTypeDefinition(opType, tpe, comment, Some(pos)))
  // }

}

trait Operations extends PositionTracking {
  this: Parser & Tokens & Ignored & Values & Types & Directives =>

  // def OperationDefinition = rule {
  //   Comments ~ trackPos ~ SelectionSet ~> ((comment, pos, s) => ast.OperationDefinition(selections = s._1, comments = comment, trailingComments = s._2, position = Some(pos))) |
  //   Comments ~ trackPos ~ OperationType ~ OperationName.? ~ (VariableDefinitions.? ~> (_ getOrElse Nil)) ~ (Directives.? ~> (_ getOrElse Nil)) ~ SelectionSet ~>
  //       ((comment, pos, opType, name, vars, dirs, sels) => ast.OperationDefinition(opType, name, vars, dirs, sels._1, comment, sels._2, Some(pos)))
  // }

  // def OperationName = rule { Name }

  // def OperationType = rule {
  //   Query ~ push(ast.OperationType.Query) |
  //   Mutation ~ push(ast.OperationType.Mutation) |
  //   Subscription ~ push(ast.OperationType.Subscription)
  // }

  // def Query = rule { Keyword("query") }

  // def Mutation = rule { Keyword("mutation") }

  // def Subscription = rule { Keyword("subscription") }

  // def VariableDefinitions = rule { wsNoComment('(') ~ VariableDefinition.+ ~ wsNoComment(')') ~> (_.toList)}

  // def VariableDefinition = rule { Comments ~ trackPos ~ Variable ~ ws(':') ~ Type ~ DefaultValue.? ~>
  //     ((comment, pos, name, tpe, defaultValue) => ast.VariableDefinition(name, tpe, defaultValue, comment, Some(pos))) }

  def Variable = rule { Ignored.* ~ '$' ~ NameStrict }

  def DefaultValue = rule { wsNoComment('=') ~ ValueConst }

  // def SelectionSet: Rule1[(List[ast.Selection], List[ast.Comment])] = rule {
  //   wsNoComment('{') ~ Selection.+ ~ Comments ~ wsNoComment('}') ~>
  //       ((x: Seq[ast.Selection], comments: List[ast.Comment]) => x.toList -> comments)
  // }

  // def Selection = rule { Field | FragmentSpread | InlineFragment }

  // def Field = rule {
  //   Comments ~ trackPos ~ Alias.? ~ Name ~
  //     (Arguments.? ~> (_ getOrElse Nil)) ~
  //     (Directives.? ~> (_ getOrElse Nil)) ~
  //     (SelectionSet.? ~> (_ getOrElse (Nil -> Nil))) ~>
  //       ((comment, pos, alias, name, args, dirs, sels) => ast.Field(alias, name, args, dirs, sels._1, comment, sels._2, Some(pos)))
  // }

  // def Alias = rule { Name ~ ws(':') }

  def Arguments = rule { Ignored.* ~ wsNoComment('(') ~ Argument.+ ~ wsNoComment(')') ~> (_.toList) }

  def Argument = rule {
    Comments ~ trackPos ~ (Name ~ wsNoComment(':')).? ~ Value ~>
      ((comment, pos, name, value) => ast.Argument(name, value, comment, Some(pos)))
  }
}

trait Values { this: Parser & Tokens & Ignored & Operations & Types & Directives =>

  def ValueConst: Rule1[ast.Value] = rule {
    NumberValue | RawValue | StringValue | BooleanValue | NullValue | EnumValue | ListValueConst | ObjectValueConst
  }

  def Value: Rule1[ast.Value] = rule {
    Comments ~ trackPos ~ Variable ~> ((comment, pos, name) => ast.VariableValue(name, comment, Some(pos))) |
      NumberValue |
      RawValue |
      StringValue |
      BooleanValue |
      NullValue |
      EnumValue |
      ListValue |
      ObjectValue
  }

  def BooleanValue = rule {
    Comments ~ trackPos ~ True ~> ((comment, pos) => ast.BooleanValue(true, comment, Some(pos))) |
      Comments ~ trackPos ~ False ~> ((comment, pos) => ast.BooleanValue(false, comment, Some(pos)))
  }

  def True = rule { Keyword("true") }

  def False = rule { Keyword("false") }

  def Null = rule { Keyword("null") }

  def NullValue = rule { Comments ~ trackPos ~ Null ~> ((comment, pos) => ast.NullValue(comment, Some(pos))) }

  def EnumValue = rule { Comments ~ !True ~ !False ~ trackPos ~ Name ~> ((comment, pos, name) => ast.EnumValue(name, comment, Some(pos))) }

  def ListValueConst = rule {
    Comments ~ trackPos ~ wsNoComment('[') ~ ValueConst.* ~ wsNoComment(']') ~> ((comment, pos, v) =>
      ast.ListValue(v.toList, comment, Some(pos))
    )
  }

  def ListValue = rule {
    Comments ~ trackPos ~ wsNoComment('[') ~ Value.* ~ wsNoComment(']') ~> ((comment, pos, v) =>
      ast.ListValue(v.toList, comment, Some(pos))
    )
  }

  def ObjectValueConst = rule {
    Comments ~ trackPos ~ wsNoComment('{') ~ ObjectFieldConst.* ~ wsNoComment('}') ~> ((comment, pos, f) =>
      ast.ObjectValue(f.toList, comment, Some(pos))
    )
  }

  def ObjectValue = rule {
    Comments ~ trackPos ~ wsNoComment('{') ~ ObjectField.* ~ wsNoComment('}') ~> ((comment, pos, f) =>
      ast.ObjectValue(f.toList, comment, Some(pos))
    )
  }

  def ObjectFieldConst = rule {
    Comments ~ trackPos ~ Name ~ wsNoComment(':') ~ ValueConst ~> ((comment, pos, name, value) =>
      ast.ObjectField(name, value, comment, Some(pos))
    )
  }

  def ObjectField = rule {
    Comments ~ trackPos ~ Name ~ wsNoComment(':') ~ Value ~> ((comment, pos, name, value) =>
      ast.ObjectField(name, value, comment, Some(pos))
    )
  }

  def RawValue = rule {
    atomic(
      Comments ~ trackPos ~ 'r' ~ 'a' ~ 'w' ~ '"' ~ clearSB() ~ Characters ~ '"' ~ push(sb.toString) ~ IgnoredNoComment.* ~> (
        (comment, pos, s) => ast.RawValue(s, comment, Some(pos))
      )
    )
  }
}

trait Directives {
  this: Parser & Tokens & Operations & Ignored & Values & Operations & Types =>

  def Directives = rule { Directive.+ ~> (_.toList) }

  def Directive = rule {
    Comments ~ trackPos ~ '@' ~ NameStrict ~ (Arguments.? ~> (_ getOrElse Nil)) ~>
      ((comment, pos, name, args) => ast.Directive(name, args, comment, Some(pos)))
  }

}

trait Types { this: Parser & Tokens & Ignored =>
  def `lazy` = rule { Keyword("lazy") }

  def Type: Rule1[ast.Type] = rule { LazyType | NonNullType | ListType | NamedType }

  def LazyType = rule { trackPos ~ `lazy` ~ Type ~> ((pos, tpe) => ast.LazyType(tpe, Some(pos))) }

  def TypeName: Rule1[List[String]] = rule { RawNames | DotNames }

  def NamedType = rule { Ignored.* ~ trackPos ~ TypeName ~> ((pos, name) => ast.NamedType(name, Some(pos))) }

  def ListType = rule { trackPos ~ ws('[') ~ Type ~ wsNoComment(']') ~> ((pos, tpe) => ast.ListType(tpe, Some(pos))) }

  def NonNullType = rule {
    trackPos ~ TypeName ~ wsNoComment('!') ~> ((pos, name) => ast.NotNullType(ast.NamedType(name, Some(pos)), Some(pos))) |
      trackPos ~ ListType ~ wsNoComment('!') ~> ((pos, tpe) => ast.NotNullType(tpe, Some(pos)))
  }
}

class SchemaParser(val input: ParserInput)
    extends Parser
    with Tokens
    with Ignored
    with Document
    with Operations // with Fragments
    with Values
    with Directives
    with Types
    with TypeSystemDefinitions

object SchemaParser {
  def parse(input: String): Try[ast.Document] =
    parse(ParserInput(input))

  def parse(input: ParserInput): Try[ast.Document] = {
    val parser = new SchemaParser(input)
    parser.Document.run() map { x =>
      Transform.run(x)
    }
  }
}

object Transform {
  import ast.AstUtil._

  def run(doc: ast.Document): ast.Document =
    propateNamespace(doc)

  def propateNamespace(doc: ast.Document): ast.Document = {
    val pkg =
      doc.packageDecl map { case ast.PackageDecl(nameSegments, _, _, _) =>
        nameSegments.mkString(".")
      }
    val target =
      doc.packageDecl flatMap { case ast.PackageDecl(_, dirs, _, _) =>
        toTarget(dirs)
      }
    val defns =
      doc.definitions map {
        toDefinitions(_, pkg, target)
      }
    doc.copy(definitions = defns)
  }

  def toDefinitions(d: ast.Definition, ns0: Option[String], packageTarget: Option[String]): ast.TypeDefinition =
    d match {
      case e: ast.EnumTypeDefinition =>
        val dirs = e.directives
        e.copy(namespace = e.namespace orElse ns0, directives = addTarget(dirs, packageTarget))
      case o: ast.ObjectTypeDefinition =>
        val dirs = o.directives
        o.copy(namespace = o.namespace orElse ns0, directives = addTarget(dirs, packageTarget))
      case i: ast.InterfaceTypeDefinition =>
        val dirs = i.directives
        i.copy(namespace = i.namespace orElse ns0, directives = addTarget(dirs, packageTarget))
    }
  def addTarget(dirs0: List[ast.Directive], packageTarget: Option[String]): List[ast.Directive] =
    (toTarget(dirs0), packageTarget) match {
      case (Some(x), _)       => dirs0
      case (None, None)       => dirs0
      case (_, Some("Java"))  => dirs0 ++ List(ast.Directive.targetJava)
      case (_, Some("Scala")) => dirs0 ++ List(ast.Directive.targetScala)
      case (_, Some(x))       => sys.error(s"Invalid package target $x")
    }
}
