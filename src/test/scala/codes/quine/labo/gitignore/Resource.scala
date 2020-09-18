package codes.quine.labo.gitignore

import java.nio.file.Path

object Resource {
  val resourceRootPath: Path = Path.of("src/test/resources")

  def resourcePath(path: Path): Path =
    resourceRootPath.resolve(path)

  def gitignorePath: Path =
    resourcePath(Path.of("gitignore"))

  def gitignorePath(path: Path): Path =
    gitignorePath.resolve(path)

  def traversePath: Path =
    resourcePath(Path.of("traverse"))

  def traversePath(path: Path): Path =
    traversePath.resolve(path)
}
