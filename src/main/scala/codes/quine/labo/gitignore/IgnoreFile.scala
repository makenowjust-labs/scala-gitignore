package codes.quine.labo.gitignore

import java.nio.file.Path

import scala.io.Source

final case class IgnoreFile(patterns: Seq[Pattern]) {
  def isIgnored(path: Path): Boolean =
    patterns.foldLeft(false) { (ignored, pattern) =>
      val matched = pattern.matches(path)
      if (matched) !pattern.negation
      else ignored
    }

  def ++(other: IgnoreFile): IgnoreFile = IgnoreFile(patterns ++ other.patterns)
}

object IgnoreFile {
  def load(path: Path, base: Option[Path] = None): IgnoreFile =
    IgnoreFile(PatternParser.parse(Source.fromFile(path.toFile).mkString, base.getOrElse(path.getParent)))
}
