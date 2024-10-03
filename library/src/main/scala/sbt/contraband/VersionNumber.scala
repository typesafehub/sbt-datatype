package sbt.contraband

import scala.annotation.tailrec

final class VersionNumber private[sbt] (val numbers: Seq[Long], val tags: Seq[String], val extras: Seq[String]) {
  def _1: Option[Long] = get(0)
  def _2: Option[Long] = get(1)
  def _3: Option[Long] = get(2)
  def _4: Option[Long] = get(3)
  def get(idx: Int): Option[Long] =
    if (size <= idx) None
    else Some(numbers(idx))
  def size: Int = numbers.size

  def <(o: VersionNumber)(implicit ord: Ordering[VersionNumber]): Boolean =
    ord.compare(this, o) < 0
  def <=(o: VersionNumber)(implicit ord: Ordering[VersionNumber]): Boolean =
    ord.compare(this, o) <= 0
  def >(o: VersionNumber)(implicit ord: Ordering[VersionNumber]): Boolean =
    ord.compare(this, o) > 0
  def >=(o: VersionNumber)(implicit ord: Ordering[VersionNumber]): Boolean =
    ord.compare(this, o) >= 0

  private val versionStr: String =
    numbers.mkString(".") +
      (tags match {
        case Seq() => ""
        case ts    => "-" + ts.mkString("-")
      }) +
      extras.mkString("")
  override def toString: String = versionStr
  override def hashCode: Int =
    numbers.hashCode * 41 * 41 +
      tags.hashCode * 41 +
      extras.hashCode
  override def equals(o: Any): Boolean =
    o match {
      case v: VersionNumber => (this.numbers == v.numbers) && (this.tags == v.tags) && (this.extras == v.extras)
      case _                => false
    }
}

trait VersionNumberInstances {
  implicit val versionNuberOrdering: Ordering[VersionNumber] =
    new Ordering[VersionNumber] {
      def compare(x: VersionNumber, y: VersionNumber): Int = {
        val cn = compareSeq(x.numbers, y.numbers)
        if (cn == 0) cn
        else cn
      }
    }
  val stringOrdering = implicitly[Ordering[String]]
  val longOrdering = implicitly[Ordering[Long]]

  @tailrec
  final def compareSeq[A: Ordering: Zero](a1: Seq[A], a2: Seq[A]): Int =
    if (a1.isEmpty && a2.isEmpty) 0
    else if (a1.nonEmpty && a2.isEmpty) implicitly[Ordering[A]].compare(a1.head, implicitly[Zero[A]].zero)
    else if (a1.isEmpty && a2.nonEmpty) implicitly[Ordering[A]].compare(implicitly[Zero[A]].zero, a2.head)
    else {
      val a1head = a1.head
      val a2head = a2.head
      if (a1head == a2head) compareSeq(a1.tail, a2.tail)
      else implicitly[Ordering[A]].compare(a1head, a2head)
    }
}

object VersionNumber extends VersionNumberInstances {
  def empty: VersionNumber = VersionNumber("0.0.0")

  /**
   * @param numbers numbers delimited by a dot.
   * @param tags string prefixed by a dash.
   * @param any other strings at the end.
   */
  def apply(numbers: Seq[Long], tags: Seq[String], extras: Seq[String]): VersionNumber =
    new VersionNumber(numbers, tags, extras)
  def apply(v: String): VersionNumber =
    unapply(v) match {
      case Some((ns, ts, es)) => VersionNumber(ns, ts, es)
      case _                  => sys.error(s"Invalid version number: $v")
    }

  def unapply(v: VersionNumber): Option[(Seq[Long], Seq[String], Seq[String])] =
    Some((v.numbers, v.tags, v.extras))

  def unapply(v: String): Option[(Seq[Long], Seq[String], Seq[String])] = {
    def splitDot(s: String): Vector[Long] =
      Option(s) match {
        case Some(x) => x.split('.').toVector.filterNot(_ == "").map(_.toLong)
        case _       => Vector()
      }
    def splitDash(s: String): Vector[String] =
      Option(s) match {
        case Some(x) => x.split('-').toVector.filterNot(_ == "")
        case _       => Vector()
      }
    def splitPlus(s: String): Vector[String] =
      Option(s) match {
        case Some(x) => x.split('+').toVector.filterNot(_ == "").map("+" + _)
        case _       => Vector()
      }
    val TaggedVersion = """(\d{1,14})([\.\d{1,14}]*)((?:-\w+)*)((?:\+.+)*)""".r
    val NonSpaceString = """(\S+)""".r
    v match {
      case TaggedVersion(m, ns, ts, es) => Some((Vector(m.toLong) ++ splitDot(ns), splitDash(ts), splitPlus(es)))
      case ""                           => None
      case NonSpaceString(s)            => Some((Vector(), Vector(), Vector(s)))
      case _                            => None
    }
  }
}
