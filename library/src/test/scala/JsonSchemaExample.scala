package sbt.contraband

import java.io.File

import scala.collection.immutable.ListMap

object JsonSchemaExample {
  val basicSchema = """{
  "namespace": "com.example",
  "protocol": "HelloWorld",
  "doc": "Protocol Greetings",

  "types": [
    {"name": "Greeting", "type": "record", "fields": [
      {"name": "message", "type": "string"}]}
  ]
}"""

  val emptySchemaExample = """{}"""

  val emptyInterfaceExample = """{
  "type": "interface",
  "target": "Scala",
  "name": "emptyInterfaceExample"
}"""

  val emptyRecordExample = """{
  "type": "record",
  "target": "Scala",
  "name": "emptyRecordExample"
}"""

  val emptyEnumerationExample = """{
  "type": "enumeration",
  "target": "Scala",
  "name": "emptyEnumerationExample"
}"""

  val invalidDefinitionKindExample = """{
  "type": "invalid",
  "name": "hello"
}"""

  val simpleInterfaceExample = """{
  "type": "interface",
  "doc": "example of simple interface",
  "name": "simpleInterfaceExample",
  "target": "Scala",
  "fields": [
    {
      "name": "field",
      "type": "type"
    }
  ],
  "toString": "return \"custom\";",
  "extra": "// Some extra code...",
  "extraCompanion": "// Some extra companion code...",
  "parents": [ "Interface1", "Interface2" ],
  "parentsCompanion": [ "CompanionInterface1", "CompanionInterface2" ]
}"""

  val oneChildInterfaceExample = """{
  "name": "oneChildInterfaceExample",
  "type": "interface",
  "target": "Scala",
  "doc": "example of interface",
  "fields": [
    {
      "name": "field",
      "type": "int"
    }
  ],
  "types": [
    {
      "name": "childRecord",
      "type": "record",
      "target": "Scala",
      "fields": [
        {
          "name": "x",
          "type": "int"
        }
      ]
    }
  ]
}"""

  val nestedInterfaceExample = """{
  "name": "nestedProtocolExample",
  "target": "Scala",
  "type": "interface",
  "doc": "example of nested protocols",
  "types": [
    {
      "name": "nestedProtocol",
      "target": "Scala",
      "type": "interface",
      "generateCodec": false,
      "types": [
        {
          "name": "ChildRecord",
          "type": "record",
          "target": "Scala"
        }
      ]
    }
  ]
}"""

  val simpleRecordExample = """{
  "name": "simpleRecordExample",
  "type": "record",
  "target": "Scala",
  "doc": "Example of simple record",
  "fields": [
    {
      "name": "field",
      "type": "java.net.URL"
    }
  ],
  "extra": "// Some extra code..."
}"""

  val simpleEnumerationExample = """{
  "name": "simpleEnumerationExample",
  "target": "Scala",
  "type": "enum",
  "doc": "Example of simple enumeration",
  "symbols": [
    {
      "name": "first",
      "doc": "First symbol"
    },
    "second"
  ],
  "extra": "// Some extra code..."
}"""

  val fieldExample = """{
  "name": "fieldExample",
  "doc": "Example of field",
  "type": "type",
  "since": "1.0.0",
  "default": "2 + 2"
}"""

  val messageExample = """{
  "name": "messageExample",
  "doc": "Example of a message",
  "response": "int",
  "request": [
    {
      "name": "arg0",
      "type": "type2"
    }
  ]
}"""

  val simpleTpeRefExample = "simpleTpeRefExample"
  val lazyTpeRefExample = "lazy lazyTpeRefExample"
  val arrayTpeRefExample = "arrayTpeRefExample*"
  val optionTpeRefExample = "optionTpeRefExample?"
  val lazyArrayTpeRefExample = "lazy lazyArrayTpeRefExample*"
  val lazyOptionTpeRefExample = "lazy lazyOptionTpeRefExample?"

  val primitiveTypesExample = """{
  "types": [
    {
      "name": "primitiveTypesExample",
      "target": "Scala",
      "type": "record",
      "fields": [
        {
          "name": "simpleInteger",
          "type": "int"
        },
        {
          "name": "lazyInteger",
          "type": "lazy int"
        },
        {
          "name": "arrayInteger",
          "type": "int*"
        },
        {
          "name": "optionInteger",
          "type": "int?"
        },
        {
          "name": "lazyArrayInteger",
          "type": "lazy int*"
        },
        {
          "name": "lazyOptionInteger",
          "type": "lazy int?"
        }
      ]
    }
  ]
}"""

  val primitiveTypesNoLazyExample = """{
  "types": [
    {
      "name": "primitiveTypesNoLazyExample",
      "target": "Scala",
      "type": "record",
      "fields": [
        {
          "name": "simpleInteger",
          "type": "int"
        },
        {
          "name": "arrayInteger",
          "type": "int*"
        }
      ]
    }
  ]
}"""

  val primitiveTypesExample2 = """{
  "name": "primitiveTypesExample2",
  "target": "Scala",
  "type": "record",
  "fields": [
    {
      "name": "smallBoolean",
      "type": "boolean"
    },
    {
      "name": "bigBoolean",
      "type": "Boolean"
    }
  ]
}"""

  val modifierExample = """{
  "name": "modifierExample",
  "target": "Scala",
  "type": "record",
  "modifier": "sealed",
  "fields": [
    {
      "name": "field",
      "type": "int"
    }
  ]
}"""

  val generateArgDocExample = """{
  "codecNamespace": "generated",
  "types": [
    {
      "name": "generateArgDocExample",
      "target": "Scala",
      "type": "interface",
      "fields": [
        {
          "name": "field",
          "type": "int",
          "doc": "I'm a field."
        }
      ],
      "messages": [
        {
          "name": "messageExample",
          "doc": [
            "A very simple example of a message.",
            "Messages can only appear in interface definitions."
          ],
          "response": "int*",
          "request": [
            {
              "name": "arg0",
              "type": "lazy int*",
              "doc": [
                "The first argument of the message.",
                "Make sure it is awesome."
              ]
            },
            {
              "name": "arg1",
              "type": "boolean",
              "doc": "This argument is not important, so it gets single line doc."
            }
          ]
        }
      ]
    }
  ]
}"""

  val completeExample = """{
  "codecNamespace": "generated",
  "fullCodec": "CustomProtocol",
  "types": [
    {
      "name": "Greetings",
      "namespace": "com.example",
      "target": "Scala",
      "doc": "A greeting interface",
      "type": "interface",

      "fields": [
        {
          "name": "message",
          "doc": [
            "The message of the Greeting",
            "This is a multiline doc comment"
          ],
          "type": "lazy String"
        },
        {
          "name": "header",
          "doc": "The header of the Greeting",
          "type": "com.example.GreetingHeader",
          "default": "new com.example.GreetingHeader(new java.util.Date(), \"Unknown\")",
          "since": "0.2.0"
        }
      ],

      "types": [
        {
          "name": "SimpleGreeting",
          "namespace": "com.example",
          "target": "Scala",
          "type": "record",
          "doc": "A Greeting in its simplest form"
        },
        {
          "name": "GreetingExtra",
          "namespace": "com.example",
          "target": "Java",
          "type": "interface",
          "fields": [
            {
              "name": "extra",
              "type": "String*"
            }
          ],
          "toString": "return \"Welcome, extra!\";",
          "types": [
            {
              "name": "GreetingExtraImpl",
              "namespace": "com.example",
              "target": "Java",
              "type": "record",
              "fields": [
                {
                  "name": "x",
                  "type": "String"
                }
              ],
              "toString": "return \"Welcome, extra implosion!\";"
            }
          ]
        },
        {
          "name": "GreetingWithAttachments",
          "namespace": "com.example",
          "target": "Scala",
          "type": "record",
          "doc": "A Greeting with attachments",

          "fields": [
            {
              "name": "attachments",
              "type": "java.io.File*",
              "doc": "The files attached to the greeting"
            }
          ]
        }
      ],
      "extraCompanion": "val empty: com.example.Greeting = new com.example.SimpleGreeting(\"Hello, World!\")",
      "parents": "com.example.GreetingsLike",
      "parentsCompanion": "com.example.GreetingsCompanionLike"
    },
    {
      "name": "GreetingHeader",
      "namespace": "com.example",
      "target": "Scala",
      "type": "record",
      "doc": "Meta information of a Greeting",

      "fields": [
        {
          "name": "created",
          "doc": "Creation date",
          "type": "lazy java.util.Date"
        },
        {
          "name": "priority",
          "doc": "The priority of this Greeting",
          "type": "com.example.PriorityLevel",
          "since": "0.3.0",
          "default": "com.example.PriorityLevel.Medium"
        },
        {
          "name": "author",
          "doc": "The author of the Greeting",
          "type": "String"
        }
      ],
      "extraCompanion": [
        "val default: GreetingHeader = ",
        "  new GreetingHeader(new java.util.Date(), com.example.PriorityLevel.Medium, scala.sys.props(\"user.name\")"
      ]
    },
    {
      "name": "PriorityLevel",
      "namespace": "com.example",
      "target": "Scala",
      "type": "enumeration",
      "doc": "Priority levels",

      "symbols": [
        "Low",
        {
          "name": "Medium",
          "doc": "Default priority level"
        },
        "High"
      ],
      "extraCompanion": "val default: com.example.PriorityLevel = com.example.PriorityLevel.Medium"
    }
  ]
}"""

  val completeExampleCodeScala =
    """package com.example
/** A greeting interface */
sealed abstract class Greetings(
  _message: => String,
  val header: com.example.GreetingHeader) extends com.example.GreetingsLike with Serializable {
  def this(message: => String) = this(message, new com.example.GreetingHeader(new java.util.Date(), "Unknown"))
  /**
   * The message of the Greeting
   * This is a multiline doc comment
   */
  lazy val message: String = _message

  override def equals(o: Any): Boolean = this.eq(o.asInstanceOf[AnyRef]) || (o match {
    case _: Greetings => super.equals(o) // We have lazy members, so use object identity to avoid circularity.
    case _ => false
  })
  override def hashCode: Int = {
    super.hashCode // Avoid evaluating lazy members in hashCode to avoid circularity.
  }
  override def toString: String = {
    super.toString // Avoid evaluating lazy members in toString to avoid circularity.
  }
}
object Greetings extends com.example.GreetingsCompanionLike {
  val empty: com.example.Greeting = new com.example.SimpleGreeting("Hello, World!")
}

/**
 * A Greeting in its simplest form
 * @param message The message of the Greeting
                  This is a multiline doc comment
 * @param header The header of the Greeting
 */
final class SimpleGreeting private (
  message: => String,
  header: com.example.GreetingHeader) extends com.example.Greetings(message, header) with Serializable {
  private def this(message: => String) = this(message, new com.example.GreetingHeader(new java.util.Date(), "Unknown"))

  override def equals(o: Any): Boolean = this.eq(o.asInstanceOf[AnyRef]) || (o match {
    case _: SimpleGreeting => super.equals(o) // We have lazy members, so use object identity to avoid circularity.
    case _ => false
  })
  override def hashCode: Int = {
    super.hashCode // Avoid evaluating lazy members in hashCode to avoid circularity.
  }
  override def toString: String = {
    super.toString // Avoid evaluating lazy members in toString to avoid circularity.
  }
  private def copy(message: => String = message, header: com.example.GreetingHeader = header): SimpleGreeting = {
    new SimpleGreeting(message, header)
  }
  def withMessage(message: => String): SimpleGreeting = {
    copy(message = message)
  }
  def withHeader(header: com.example.GreetingHeader): SimpleGreeting = {
    copy(header = header)
  }
}
object SimpleGreeting {
  def apply(message: => String): SimpleGreeting = new SimpleGreeting(message)
  def apply(message: => String, header: com.example.GreetingHeader): SimpleGreeting = new SimpleGreeting(message, header)
}
sealed abstract class GreetingExtra(
  message: => String,
  header: com.example.GreetingHeader,
  val extra: Vector[String]) extends com.example.Greetings(message, header) with Serializable {
  def this(message: => String, extra: Vector[String]) = this(message, new com.example.GreetingHeader(new java.util.Date(), "Unknown"), extra)


  override def equals(o: Any): Boolean = this.eq(o.asInstanceOf[AnyRef]) || (o match {
    case _: GreetingExtra => super.equals(o) // We have lazy members, so use object identity to avoid circularity.
    case _ => false
  })
  override def hashCode: Int = {
    super.hashCode // Avoid evaluating lazy members in hashCode to avoid circularity.
  }
  override def toString: String = {
    return "Welcome, extra!";
  }
}
object GreetingExtra {
}
/**
 * @param message The message of the Greeting
                  This is a multiline doc comment
 * @param header The header of the Greeting
 */
final class GreetingExtraImpl private (
  message: com.example.Lazy[String],
  header: com.example.GreetingHeader,
  extra: Array[String],
  val x: String) extends com.example.GreetingExtra(message, header, extra) with Serializable {
  private def this(message: com.example.Lazy[String], extra: Array[String], x: String) = this(message, new com.example.GreetingHeader(new java.util.Date(), "Unknown"), extra, x)

  override def equals(o: Any): Boolean = this.eq(o.asInstanceOf[AnyRef]) || (o match {
    case _: GreetingExtraImpl => super.equals(o) // We have lazy members, so use object identity to avoid circularity.
    case _ => false
  })
  override def hashCode: Int = {
    super.hashCode // Avoid evaluating lazy members in hashCode to avoid circularity.
  }
  override def toString: String = {
    return "Welcome, extra implosion!";
  }
  private def copy(message: com.example.Lazy[String] = message, header: com.example.GreetingHeader = header, extra: Array[String] = extra, x: String = x): GreetingExtraImpl = {
    new GreetingExtraImpl(message, header, extra, x)
  }
  def withMessage(message: com.example.Lazy[String]): GreetingExtraImpl = {
    copy(message = message)
  }
  def withHeader(header: com.example.GreetingHeader): GreetingExtraImpl = {
    copy(header = header)
  }
  def withExtra(extra: Array[String]): GreetingExtraImpl = {
    copy(extra = extra)
  }
  def withX(x: String): GreetingExtraImpl = {
    copy(x = x)
  }
}
object GreetingExtraImpl {
  def apply(message: com.example.Lazy[String], extra: Array[String], x: String): GreetingExtraImpl = new GreetingExtraImpl(message, extra, x)
  def apply(message: com.example.Lazy[String], header: com.example.GreetingHeader, extra: Array[String], x: String): GreetingExtraImpl = new GreetingExtraImpl(message, header, extra, x)
}

/**
 * A Greeting with attachments
 * @param message The message of the Greeting
                  This is a multiline doc comment
 * @param header The header of the Greeting
 * @param attachments The files attached to the greeting
 */
final class GreetingWithAttachments private (
  message: => String,
  header: com.example.GreetingHeader,
  val attachments: Vector[java.io.File]) extends com.example.Greetings(message, header) with Serializable {
  private def this(message: => String, attachments: Vector[java.io.File]) = this(message, new com.example.GreetingHeader(new java.util.Date(), "Unknown"), attachments)

  override def equals(o: Any): Boolean = this.eq(o.asInstanceOf[AnyRef]) || (o match {
    case _: GreetingWithAttachments => super.equals(o) // We have lazy members, so use object identity to avoid circularity.
    case _ => false
  })
  override def hashCode: Int = {
    super.hashCode // Avoid evaluating lazy members in hashCode to avoid circularity.
  }
  override def toString: String = {
    super.toString // Avoid evaluating lazy members in toString to avoid circularity.
  }
  private def copy(message: => String = message, header: com.example.GreetingHeader = header, attachments: Vector[java.io.File] = attachments): GreetingWithAttachments = {
    new GreetingWithAttachments(message, header, attachments)
  }
  def withMessage(message: => String): GreetingWithAttachments = {
    copy(message = message)
  }
  def withHeader(header: com.example.GreetingHeader): GreetingWithAttachments = {
    copy(header = header)
  }
  def withAttachments(attachments: Vector[java.io.File]): GreetingWithAttachments = {
    copy(attachments = attachments)
  }
}
object GreetingWithAttachments {
  def apply(message: => String, attachments: Vector[java.io.File]): GreetingWithAttachments = new GreetingWithAttachments(message, attachments)
  def apply(message: => String, header: com.example.GreetingHeader, attachments: Vector[java.io.File]): GreetingWithAttachments = new GreetingWithAttachments(message, header, attachments)
}

/**
 * Meta information of a Greeting
 * @param created Creation date
 * @param priority The priority of this Greeting
 * @param author The author of the Greeting
 */
final class GreetingHeader private (
  _created: => java.util.Date,
  val priority: com.example.PriorityLevel,
  val author: String) extends Serializable {
  private def this(created: => java.util.Date, author: String) = this(created, com.example.PriorityLevel.Medium, author)
  /** Creation date */
  lazy val created: java.util.Date = _created
  override def equals(o: Any): Boolean = this.eq(o.asInstanceOf[AnyRef]) || (o match {
    case _: GreetingHeader => super.equals(o) // We have lazy members, so use object identity to avoid circularity.
    case _ => false
  })
  override def hashCode: Int = {
    super.hashCode // Avoid evaluating lazy members in hashCode to avoid circularity.
  }
  override def toString: String = {
    super.toString // Avoid evaluating lazy members in toString to avoid circularity.
  }
  private def copy(created: => java.util.Date = created, priority: com.example.PriorityLevel = priority, author: String = author): GreetingHeader = {
    new GreetingHeader(created, priority, author)
  }
  def withCreated(created: => java.util.Date): GreetingHeader = {
    copy(created = created)
  }
  def withPriority(priority: com.example.PriorityLevel): GreetingHeader = {
    copy(priority = priority)
  }
  def withAuthor(author: String): GreetingHeader = {
    copy(author = author)
  }
}
object GreetingHeader {
  val default: GreetingHeader =
  new GreetingHeader(new java.util.Date(), com.example.PriorityLevel.Medium, scala.sys.props("user.name")
  def apply(created: => java.util.Date, author: String): GreetingHeader = new GreetingHeader(created, author)
  def apply(created: => java.util.Date, priority: com.example.PriorityLevel, author: String): GreetingHeader = new GreetingHeader(created, priority, author)
}

/** Priority levels */
sealed abstract class PriorityLevel extends Serializable
object PriorityLevel {
  case object Low extends PriorityLevel
  /** Default priority level */
  case object Medium extends PriorityLevel
  case object High extends PriorityLevel
}""".stripMargin

  val completeExampleCodeJava =
    ListMap(
      new File("com/example/GreetingHeader.java") ->
        """package com.example;
          |/** Meta information of a Greeting */
          |public final class GreetingHeader implements java.io.Serializable {
          |
          |    public static GreetingHeader create(com.example.MyLazy<java.util.Date> _created, String _author) {
          |        return new GreetingHeader(_created, _author);
          |    }
          |    public static GreetingHeader of(com.example.MyLazy<java.util.Date> _created, String _author) {
          |        return new GreetingHeader(_created, _author);
          |    }
          |    public static GreetingHeader create(com.example.MyLazy<java.util.Date> _created, com.example.PriorityLevel _priority, String _author) {
          |        return new GreetingHeader(_created, _priority, _author);
          |    }
          |    public static GreetingHeader of(com.example.MyLazy<java.util.Date> _created, com.example.PriorityLevel _priority, String _author) {
          |        return new GreetingHeader(_created, _priority, _author);
          |    }
          |    private com.example.MyLazy<java.util.Date> created;
          |    private com.example.PriorityLevel priority;
          |    private String author;
          |
          |    protected GreetingHeader(com.example.MyLazy<java.util.Date> _created, String _author) {
          |        super();
          |        created = _created;
          |        priority = com.example.PriorityLevel.Medium;
          |        author = _author;
          |    }
          |
          |    protected GreetingHeader(com.example.MyLazy<java.util.Date> _created, com.example.PriorityLevel _priority, String _author) {
          |        super();
          |        created = _created;
          |        priority = _priority;
          |        author = _author;
          |    }
          |
          |    /** Creation date */
          |    public java.util.Date created() {
          |        return this.created.get();
          |    }
          |
          |    /** The priority of this Greeting */
          |    public com.example.PriorityLevel priority() {
          |        return this.priority;
          |    }
          |
          |    /** The author of the Greeting */
          |    public String author() {
          |        return this.author;
          |    }
          |
          |    public GreetingHeader withCreated(com.example.MyLazy<java.util.Date> created) {
          |        return new GreetingHeader(created, priority, author);
          |    }
          |
          |    public GreetingHeader withPriority(com.example.PriorityLevel priority) {
          |        return new GreetingHeader(created, priority, author);
          |    }
          |
          |    public GreetingHeader withAuthor(String author) {
          |        return new GreetingHeader(created, priority, author);
          |    }
          |
          |    public boolean equals(Object obj) {
          |        return this == obj; // We have lazy members, so use object identity to avoid circularity.
          |    }
          |
          |    public int hashCode() {
          |        return super.hashCode(); // Avoid evaluating lazy members in hashCode to avoid circularity.
          |    }
          |
          |    public String toString() {
          |        return super.toString(); // Avoid evaluating lazy members in toString to avoid circularity.
          |    }
          |}""".stripMargin,
      new File("com/example/PriorityLevel.java") ->
        """package com.example;
          |/** Priority levels */
          |public enum PriorityLevel {
          |    Low,
          |    /** Default priority level */
          |    Medium,
          |    High;
          |}""".stripMargin,
      new File("com/example/GreetingWithAttachments.java") ->
        """package com.example;
          |/** A Greeting with attachments */
          |public final class GreetingWithAttachments extends com.example.Greetings implements java.io.Serializable {
          |    public static GreetingWithAttachments create(com.example.MyLazy<String> _message, java.io.File[] _attachments) {
          |        return new GreetingWithAttachments(_message, _attachments);
          |    }
          |    public static GreetingWithAttachments of(com.example.MyLazy<String> _message, java.io.File[] _attachments) {
          |        return new GreetingWithAttachments(_message, _attachments);
          |    }
          |    public static GreetingWithAttachments create(com.example.MyLazy<String> _message, com.example.GreetingHeader _header, java.io.File[] _attachments) {
          |        return new GreetingWithAttachments(_message, _header, _attachments);
          |    }
          |    public static GreetingWithAttachments of(com.example.MyLazy<String> _message, com.example.GreetingHeader _header, java.io.File[] _attachments) {
          |        return new GreetingWithAttachments(_message, _header, _attachments);
          |    }
          |    private java.io.File[] attachments;
          |    protected GreetingWithAttachments(com.example.MyLazy<String> _message, java.io.File[] _attachments) {
          |        super(_message, new com.example.GreetingHeader(new java.util.Date(), "Unknown"));
          |        attachments = _attachments;
          |    }
          |    protected GreetingWithAttachments(com.example.MyLazy<String> _message, com.example.GreetingHeader _header, java.io.File[] _attachments) {
          |        super(_message, _header);
          |        attachments = _attachments;
          |    }
          |    /** The files attached to the greeting */
          |    public java.io.File[] attachments() {
          |        return this.attachments;
          |    }
          |    public GreetingWithAttachments withMessage(com.example.MyLazy<String> message) {
          |        return new GreetingWithAttachments(message, header(), attachments);
          |    }
          |    public GreetingWithAttachments withHeader(com.example.GreetingHeader header) {
          |        return new GreetingWithAttachments(new com.example.MyLazy<String>() { public String get() { return message(); } }, header, attachments);
          |    }
          |    public GreetingWithAttachments withAttachments(java.io.File[] attachments) {
          |        return new GreetingWithAttachments(new com.example.MyLazy<String>() { public String get() { return message(); } }, header(), attachments);
          |    }
          |    public boolean equals(Object obj) {
          |        return this == obj; // We have lazy members, so use object identity to avoid circularity.
          |    }
          |    public int hashCode() {
          |        return super.hashCode(); // Avoid evaluating lazy members in hashCode to avoid circularity.
          |    }
          |    public String toString() {
          |        return super.toString(); // Avoid evaluating lazy members in toString to avoid circularity.
          |    }
          |}""".stripMargin,
      new File("com/example/GreetingExtra.java") ->
        """package com.example;
public abstract class GreetingExtra extends com.example.Greetings implements java.io.Serializable {

    private String[] extra;
    protected GreetingExtra(com.example.MyLazy<String> _message, String[] _extra) {
        super(_message, new com.example.GreetingHeader(new java.util.Date(), "Unknown"));
        extra = _extra;
    }
    protected GreetingExtra(com.example.MyLazy<String> _message, com.example.GreetingHeader _header, String[] _extra) {
        super(_message, _header);
        extra = _extra;
    }
    public String[] extra() {
        return this.extra;
    }

    public boolean equals(Object obj) {
        return this == obj; // We have lazy members, so use object identity to avoid circularity.
    }
    public int hashCode() {
        return super.hashCode(); // Avoid evaluating lazy members in hashCode to avoid circularity.
    }
    public String toString() {
        return "Welcome, extra!";
    }
}""",
      new File("com/example/GreetingExtraImpl.java") ->
        """package com.example;
public final class GreetingExtraImpl extends com.example.GreetingExtra implements java.io.Serializable {

    public static GreetingExtraImpl create(com.example.MyLazy<String> _message, String[] _extra, String _x) {
        return new GreetingExtraImpl(_message, _extra, _x);
    }
    public static GreetingExtraImpl of(com.example.MyLazy<String> _message, String[] _extra, String _x) {
        return new GreetingExtraImpl(_message, _extra, _x);
    }
    public static GreetingExtraImpl create(com.example.MyLazy<String> _message, com.example.GreetingHeader _header, String[] _extra, String _x) {
        return new GreetingExtraImpl(_message, _header, _extra, _x);
    }
    public static GreetingExtraImpl of(com.example.MyLazy<String> _message, com.example.GreetingHeader _header, String[] _extra, String _x) {
        return new GreetingExtraImpl(_message, _header, _extra, _x);
    }
    private String x;
    protected GreetingExtraImpl(com.example.MyLazy<String> _message, String[] _extra, String _x) {
        super(_message, new com.example.GreetingHeader(new java.util.Date(), "Unknown"), _extra);
        x = _x;
    }
    protected GreetingExtraImpl(com.example.MyLazy<String> _message, com.example.GreetingHeader _header, String[] _extra, String _x) {
        super(_message, _header, _extra);
        x = _x;
    }
    public String x() {
        return this.x;
    }
    public GreetingExtraImpl withMessage(com.example.MyLazy<String> message) {
        return new GreetingExtraImpl(message, header(), extra(), x);
    }
    public GreetingExtraImpl withHeader(com.example.GreetingHeader header) {
        return new GreetingExtraImpl(new com.example.MyLazy<String>() { public String get() { return message(); } }, header, extra(), x);
    }
    public GreetingExtraImpl withExtra(String[] extra) {
        return new GreetingExtraImpl(new com.example.MyLazy<String>() { public String get() { return message(); } }, header(), extra, x);
    }
    public GreetingExtraImpl withX(String x) {
        return new GreetingExtraImpl(new com.example.MyLazy<String>() { public String get() { return message(); } }, header(), extra(), x);
    }
    public boolean equals(Object obj) {
        return this == obj; // We have lazy members, so use object identity to avoid circularity.
    }
    public int hashCode() {
        return super.hashCode(); // Avoid evaluating lazy members in hashCode to avoid circularity.
    }
    public String toString() {
        return "Welcome, extra implosion!";
    }
}""",
      new File("com/example/Greetings.java") ->
        """package com.example;
          |/** A greeting interface */
          |public abstract class Greetings implements com.example.GreetingsLike, java.io.Serializable {
          |
          |    private com.example.MyLazy<String> message;
          |    private com.example.GreetingHeader header;
          |
          |    protected Greetings(com.example.MyLazy<String> _message) {
          |        super();
          |        message = _message;
          |        header = new com.example.GreetingHeader(new java.util.Date(), "Unknown");
          |    }
          |
          |    protected Greetings(com.example.MyLazy<String> _message, com.example.GreetingHeader _header) {
          |        super();
          |        message = _message;
          |        header = _header;
          |    }
          |
          |    /**
          |     * The message of the Greeting
          |     * This is a multiline doc comment
          |     */
          |    public String message() {
          |        return this.message.get();
          |    }
          |
          |    /** The header of the Greeting */
          |    public com.example.GreetingHeader header() {
          |        return this.header;
          |    }
          |
          |    public boolean equals(Object obj) {
          |        return this == obj; // We have lazy members, so use object identity to avoid circularity.
          |    }
          |
          |    public int hashCode() {
          |        return super.hashCode(); // Avoid evaluating lazy members in hashCode to avoid circularity.
          |    }
          |
          |    public String toString() {
          |        return super.toString(); // Avoid evaluating lazy members in toString to avoid circularity.
          |    }
          |}""".stripMargin,
      new File("com/example/SimpleGreeting.java") ->
        """package com.example;
          |/** A Greeting in its simplest form */
          |public final class SimpleGreeting extends com.example.Greetings implements java.io.Serializable {
          |    public static SimpleGreeting create(com.example.MyLazy<String> _message) {
          |        return new SimpleGreeting(_message);
          |    }
          |    public static SimpleGreeting of(com.example.MyLazy<String> _message) {
          |        return new SimpleGreeting(_message);
          |    }
          |    public static SimpleGreeting create(com.example.MyLazy<String> _message, com.example.GreetingHeader _header) {
          |        return new SimpleGreeting(_message, _header);
          |    }
          |    public static SimpleGreeting of(com.example.MyLazy<String> _message, com.example.GreetingHeader _header) {
          |        return new SimpleGreeting(_message, _header);
          |    }
          |    protected SimpleGreeting(com.example.MyLazy<String> _message) {
          |        super(_message, new com.example.GreetingHeader(new java.util.Date(), "Unknown"));
          |    }
          |
          |    protected SimpleGreeting(com.example.MyLazy<String> _message, com.example.GreetingHeader _header) {
          |        super(_message, _header);
          |    }
          |
          |    public SimpleGreeting withMessage(com.example.MyLazy<String> message) {
          |        return new SimpleGreeting(message, header());
          |    }
          |
          |    public SimpleGreeting withHeader(com.example.GreetingHeader header) {
          |        return new SimpleGreeting(new com.example.MyLazy<String>() { public String get() { return message(); } }, header);
          |    }
          |
          |    public boolean equals(Object obj) {
          |        return this == obj; // We have lazy members, so use object identity to avoid circularity.
          |    }
          |
          |    public int hashCode() {
          |        return super.hashCode(); // Avoid evaluating lazy members in hashCode to avoid circularity.
          |    }
          |
          |    public String toString() {
          |        return super.toString(); // Avoid evaluating lazy members in toString to avoid circularity.
          |    }
          |}""".stripMargin
    )

  val completeExampleCodeCodec =
    """/**
 * This code is generated using [[https://www.scala-sbt.org/contraband]].
 */

// DO NOT EDIT MANUALLY
package generated

import _root_.sjsonnew.JsonFormat

trait GreetingsFormats { self: generated.GreetingHeaderFormats with generated.PriorityLevelFormats with sjsonnew.BasicJsonProtocol with generated.SimpleGreetingFormats with generated.GreetingExtraImplFormats with generated.GreetingWithAttachmentsFormats =>
implicit lazy val GreetingsFormat: JsonFormat[com.example.Greetings] = flatUnionFormat3[com.example.Greetings, com.example.SimpleGreeting, com.example.GreetingExtraImpl, com.example.GreetingWithAttachments]("type")
}
/**
 * This code is generated using [[https://www.scala-sbt.org/contraband]].
 */

// DO NOT EDIT MANUALLY
package generated

import _root_.sjsonnew.{ Unbuilder, Builder, JsonFormat, deserializationError }

trait SimpleGreetingFormats { self: generated.GreetingHeaderFormats with generated.PriorityLevelFormats with sjsonnew.BasicJsonProtocol =>
implicit lazy val SimpleGreetingFormat: JsonFormat[com.example.SimpleGreeting] = new JsonFormat[com.example.SimpleGreeting] {
  override def read[J](__jsOpt: Option[J], unbuilder: Unbuilder[J]): com.example.SimpleGreeting = {
    __jsOpt match {
      case Some(__js) =>
      unbuilder.beginObject(__js)
      val message = unbuilder.readField[String]("message")
      val header = unbuilder.readField[com.example.GreetingHeader]("header")
      unbuilder.endObject()
      com.example.SimpleGreeting(message, header)
      case None =>
      deserializationError("Expected JsObject but found None")
    }
  }
  override def write[J](obj: com.example.SimpleGreeting, builder: Builder[J]): Unit = {
    builder.beginObject()
    builder.addField("message", obj.message)
    builder.addField("header", obj.header)
    builder.endObject()
  }
}
}
/**
 * This code is generated using [[https://www.scala-sbt.org/contraband]].
 */

// DO NOT EDIT MANUALLY
package generated

import _root_.sjsonnew.JsonFormat

trait GreetingExtraFormats { self: generated.GreetingHeaderFormats with generated.PriorityLevelFormats with sjsonnew.BasicJsonProtocol with generated.GreetingExtraImplFormats =>
implicit lazy val GreetingExtraFormat: JsonFormat[com.example.GreetingExtra] = flatUnionFormat1[com.example.GreetingExtra, com.example.GreetingExtraImpl]("type")
}
/**
 * This code is generated using [[https://www.scala-sbt.org/contraband]].
 */

// DO NOT EDIT MANUALLY
package generated

import _root_.sjsonnew.{ Unbuilder, Builder, JsonFormat, deserializationError }

trait GreetingExtraImplFormats { self: generated.GreetingHeaderFormats with generated.PriorityLevelFormats with sjsonnew.BasicJsonProtocol =>
implicit lazy val GreetingExtraImplFormat: JsonFormat[com.example.GreetingExtraImpl] = new JsonFormat[com.example.GreetingExtraImpl] {
  override def read[J](__jsOpt: Option[J], unbuilder: Unbuilder[J]): com.example.GreetingExtraImpl = {
    __jsOpt match {
      case Some(__js) =>
      unbuilder.beginObject(__js)
      val message = unbuilder.readField[String]("message")
      val header = unbuilder.readField[com.example.GreetingHeader]("header")
      val extra = unbuilder.readField[Array[String]]("extra")
      val x = unbuilder.readField[String]("x")
      unbuilder.endObject()
      com.example.GreetingExtraImpl.of(mkLazy(message), header, extra, x)
      case None =>
      deserializationError("Expected JsObject but found None")
    }
  }
  override def write[J](obj: com.example.GreetingExtraImpl, builder: Builder[J]): Unit = {
    builder.beginObject()
    builder.addField("message", obj.message)
    builder.addField("header", obj.header)
    builder.addField("extra", obj.extra)
    builder.addField("x", obj.x)
    builder.endObject()
  }
}
}
/**
 * This code is generated using [[https://www.scala-sbt.org/contraband]].
 */

// DO NOT EDIT MANUALLY
package generated

import _root_.sjsonnew.{ Unbuilder, Builder, JsonFormat, deserializationError }

trait GreetingWithAttachmentsFormats { self: generated.GreetingHeaderFormats with generated.PriorityLevelFormats with sjsonnew.BasicJsonProtocol =>
implicit lazy val GreetingWithAttachmentsFormat: JsonFormat[com.example.GreetingWithAttachments] = new JsonFormat[com.example.GreetingWithAttachments] {
  override def read[J](__jsOpt: Option[J], unbuilder: Unbuilder[J]): com.example.GreetingWithAttachments = {
    __jsOpt match {
      case Some(__js) =>
      unbuilder.beginObject(__js)
      val message = unbuilder.readField[String]("message")
      val header = unbuilder.readField[com.example.GreetingHeader]("header")
      val attachments = unbuilder.readField[Vector[java.io.File]]("attachments")
      unbuilder.endObject()
      com.example.GreetingWithAttachments(message, header, attachments)
      case None =>
      deserializationError("Expected JsObject but found None")
    }
  }
  override def write[J](obj: com.example.GreetingWithAttachments, builder: Builder[J]): Unit = {
    builder.beginObject()
    builder.addField("message", obj.message)
    builder.addField("header", obj.header)
    builder.addField("attachments", obj.attachments)
    builder.endObject()
  }
}
}
/**
 * This code is generated using [[https://www.scala-sbt.org/contraband]].
 */

// DO NOT EDIT MANUALLY
package generated

import _root_.sjsonnew.{ Unbuilder, Builder, JsonFormat, deserializationError }

trait GreetingHeaderFormats { self: generated.PriorityLevelFormats with sjsonnew.BasicJsonProtocol =>
implicit lazy val GreetingHeaderFormat: JsonFormat[com.example.GreetingHeader] = new JsonFormat[com.example.GreetingHeader] {
  override def read[J](__jsOpt: Option[J], unbuilder: Unbuilder[J]): com.example.GreetingHeader = {
    __jsOpt match {
      case Some(__js) =>
      unbuilder.beginObject(__js)
      val created = unbuilder.readField[java.util.Date]("created")
      val priority = unbuilder.readField[com.example.PriorityLevel]("priority")
      val author = unbuilder.readField[String]("author")
      unbuilder.endObject()
      com.example.GreetingHeader(created, priority, author)
      case None =>
      deserializationError("Expected JsObject but found None")
    }
  }
  override def write[J](obj: com.example.GreetingHeader, builder: Builder[J]): Unit = {
    builder.beginObject()
    builder.addField("created", obj.created)
    builder.addField("priority", obj.priority)
    builder.addField("author", obj.author)
    builder.endObject()
  }
}
}
/**
 * This code is generated using [[https://www.scala-sbt.org/contraband]].
 */

// DO NOT EDIT MANUALLY
package generated

import _root_.sjsonnew.{ Unbuilder, Builder, JsonFormat, deserializationError }

trait PriorityLevelFormats { self: sjsonnew.BasicJsonProtocol =>
implicit lazy val PriorityLevelFormat: JsonFormat[com.example.PriorityLevel] = new JsonFormat[com.example.PriorityLevel] {
  override def read[J](__jsOpt: Option[J], unbuilder: Unbuilder[J]): com.example.PriorityLevel = {
    __jsOpt match {
      case Some(__js) =>
      unbuilder.readString(__js) match {
        case "Low" => com.example.PriorityLevel.Low
        case "Medium" => com.example.PriorityLevel.Medium
        case "High" => com.example.PriorityLevel.High
      }
      case None =>
      deserializationError("Expected JsString but found None")
    }
  }
  override def write[J](obj: com.example.PriorityLevel, builder: Builder[J]): Unit = {
    val str = obj match {
      case com.example.PriorityLevel.Low => "Low"
      case com.example.PriorityLevel.Medium => "Medium"
      case com.example.PriorityLevel.High => "High"
    }
    builder.writeString(str)
  }
}
}
/**
 * This code is generated using [[https://www.scala-sbt.org/contraband]].
 */

// DO NOT EDIT MANUALLY
package generated
trait CustomProtocol extends sjsonnew.BasicJsonProtocol
  with generated.PriorityLevelFormats
  with generated.GreetingHeaderFormats
  with generated.SimpleGreetingFormats
  with generated.GreetingExtraImplFormats
  with generated.GreetingWithAttachmentsFormats
  with generated.GreetingsFormats
  with generated.GreetingExtraFormats
object CustomProtocol extends CustomProtocol""".stripMargin

  val growableAddOneFieldExample = """{
  "name": "growableAddOneField",
  "target": "Scala",
  "type": "record",
  "fields": [
    {
      "name": "field",
      "type": "int",
      "since": "0.1.0",
      "default": "0"
    }
  ]
}""".stripMargin

  val growableZeroToOneToTwoFieldsExample = """{
  "name": "Foo",
  "target": "Scala",
  "type": "record",
  "fields": [
    {
      "name": "x",
      "type": "int?",
      "since": "0.1.0",
      "default": "Option(0)"
    },
    {
      "name": "y",
      "type": "int*",
      "since": "0.2.0",
      "default": "Vector(0)"
    }
  ]
}""".stripMargin

  val growableZeroToOneToTwoFieldsJavaExample = """{
  "name": "Foo",
  "target": "Java",
  "type": "record",
  "fields": [
    {
      "name": "x",
      "type": "int?",
      "since": "0.1.0",
      "default": "java.util.Optional.<String>ofNullable(0)"
    },
    {
      "name": "y",
      "type": "int*",
      "since": "0.2.0",
      "default": "new Array { 0 }"
    }
  ]
}""".stripMargin

  val multiLineDocExample =
    """{
      |  "name": "multiLineDocField",
      |  "type": "int",
      |  "doc": [
      |    "A field whose documentation",
      |    "spans over multiple lines"
      |  ]
      |}""".stripMargin
}
