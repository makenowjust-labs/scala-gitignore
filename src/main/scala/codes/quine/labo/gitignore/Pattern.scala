package codes.quine.labo.gitignore

import java.nio.file.Path

import scala.annotation.tailrec
import scala.jdk.CollectionConverters._

sealed abstract class Pattern {
  def negation: Boolean
  def matches(path: Path): Boolean
}

object Pattern {
  final case class RelativePattern(negation: Boolean, components: Vector[Component], isDir: Boolean, base: Path)
      extends Pattern {
    def matches(path: Path): Boolean = {
      val relPath = base.relativize(path)
      if (relPath.toString == "" || relPath.startsWith(".")) false
      else {
        val matched = matchesComponents(components, relPath.iterator().asScala.map(_.toString).toVector)
        if (matched && isDir) path.toFile().isDirectory()
        else matched
      }
    }
  }

  final case class GlobalPattern(negation: Boolean, glob: Glob, isDir: Boolean) extends Pattern {
    def matches(path: Path): Boolean = {
      // When `path` is root, `path.getFileName` returns `null`, so it is wrapped.
      val matched = Option(path.getFileName).exists(name => matchesGlob(glob.chars, name.toString))
      if (matched && isDir) path.toFile().isDirectory()
      else matched
    }
  }

  sealed abstract class Component extends Serializable with Product

  final case class Glob(chars: Vector[GlobChar]) extends Component

  val Empty: Glob = Glob(Vector())

  case object StarStar extends Component

  sealed abstract class GlobChar extends Serializable with Product {
    def accepts(c: Char): Boolean
  }

  case object Star extends GlobChar {
    def accepts(c: Char): Boolean = sys.error("Pattern.Start#accepts: invalid call")
  }

  case object Question extends GlobChar {
    def accepts(c: Char): Boolean = true
  }

  final case class Literal(c: Char) extends GlobChar {
    def accepts(d: Char): Boolean = c == d
  }

  final case class Range(negation: Boolean, range: Seq[(Char, Char)]) extends GlobChar {
    def accepts(c: Char): Boolean =
      !negation == range.exists { case (b, e) => b <= c && c <= e }
  }

  def matchesComponents(components: Vector[Component], path: Vector[String]): Boolean = {
    @tailrec def loop(pos: Int = 0, state: Int = 0, nextPos: Int = 0, nextState: Int = 0): Boolean =
      if (pos >= path.size && state >= components.size) true
      else
        components.lift(state) match {
          case Some(Glob(glob)) if pos < path.size && matchesGlob(glob, path(pos)) =>
            loop(pos + 1, state + 1, nextPos, nextState)
          case Some(StarStar)                           => loop(pos, state + 1, pos + 1, state)
          case _ if 0 < nextPos && nextPos <= path.size => loop(nextPos, nextState, nextPos, nextState)
          case _                                        => false
        }
    loop()
  }

  def matchesGlob(glob: Vector[GlobChar], s: String): Boolean = {
    @tailrec def loop(pos: Int = 0, state: Int = 0, nextPos: Int = 0, nextState: Int = 0): Boolean =
      if (pos >= s.size && state >= glob.size) true
      else
        glob.lift(state) match {
          case Some(Star)                                   => loop(pos, state + 1, pos + 1, state)
          case Some(g) if pos < s.size && g.accepts(s(pos)) => loop(pos + 1, state + 1, nextPos, nextState)
          case _ if 0 < nextPos && nextPos <= s.size        => loop(nextPos, nextState, nextPos, nextState)
          case _                                            => false
        }
    loop()
  }
}
