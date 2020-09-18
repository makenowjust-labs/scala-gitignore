package codes.quine.labo.gitignore

import java.nio.file.Path

import fastparse.NoWhitespace._
import fastparse._

import Pattern._

final class PatternParser(val base: Path) {
  def pattern[_: P]: P[Pattern] = P {
    "#" ~/ Fail("comment") |
      (negation ~ components ~ spaces.? ~ End).flatMap {
        case (_, Vector(Empty))                       => Fail("empty line")
        case (negation, Vector(glob: Glob, Empty))    => Pass(GlobalPattern(negation, glob, true))
        case (negation, Vector(glob: Glob))           => Pass(GlobalPattern(negation, glob, false))
        case (negation, Empty +: components :+ Empty) => Pass(RelativePattern(negation, components, true, base))
        case (negation, Empty +: components)          => Pass(RelativePattern(negation, components, false, base))
        case (negation, components :+ Empty)          => Pass(RelativePattern(negation, components, true, base))
        case (negation, components)                   => Pass(RelativePattern(negation, components, false, base))
      }
  }

  def negation[_: P]: P[Boolean] =
    P("!" ~ Pass(true) | Pass(false))

  def components[_: P]: P[Vector[Component]] =
    P(component.rep(1, sep = "/").map(_.toVector))

  def component[_: P]: P[Component] =
    P("**" ~ Pass(StarStar) | glob)

  def glob[_: P]: P[Glob] =
    P(globChar.rep.map(cs => Glob(cs.toVector)))

  def globChar[_: P]: P[GlobChar] =
    P("?" ~ Pass(Question) | "*" ~ Pass(Star) | range | !(spaces ~ End) ~ char.map(Literal(_)))

  def range[_: P]: P[Range] =
    P(("[" ~/ ("^" ~ Pass(true) | Pass(false)) ~ (!"]" ~ rangePart).rep ~ "]").map { case (ne, r) => Range(ne, r) })

  def rangePart[_: P]: P[(Char, Char)] =
    P(char ~ "-" ~ char | char.map(c => (c, c)))

  def char[_: P]: P[Char] =
    P("\\" ~/ AnyChar.!.map(_.charAt(0)) | CharPred(_ != '/').!.map(_.charAt(0)))

  def spaces[_: P]: P[Unit] = P(CharsWhileIn(" \t"))
}

object PatternParser {
  def parse(source: String, base: Path): Seq[Pattern] = {
    val parser = new PatternParser(base)
    source.linesIterator.flatMap { line =>
      // When parsing is failed, this line is ignored.
      // Note that comments and empty lines are reported as parsing failure.
      fastparse.parse(line, parser.pattern(_)).fold((_, _, _) => Seq.empty, (p, _) => Seq(p))
    }.toSeq
  }
}
