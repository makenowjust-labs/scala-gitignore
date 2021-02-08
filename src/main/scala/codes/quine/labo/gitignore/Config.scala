package codes.quine.labo.gitignore

import java.nio.file.Path
import scala.collection.mutable

trait Config {
  def findRoot(path: Path): List[Path]

  def loadIgnore(ignorePath: Path, base: Path): Option[IgnoreFile]

  def ignoreFile(path: Path): Seq[Path]

  def rootIgnoreFile(rootPath: Path): Seq[Path]
}

object Config {
  def apply(): Config = new Default

  class Default extends Config {
    val findRootCache: mutable.Map[Path, List[Path]] = mutable.Map.empty[Path, List[Path]]
    val loadIgnoreCache: mutable.Map[(Path, Path), Option[IgnoreFile]] =
      mutable.Map.empty[(Path, Path), Option[IgnoreFile]]

    def findRoot(path: Path): List[Path] =
      findRootCache.getOrElseUpdate(
        path, {
          var list = List.empty[Path]

          var p = path
          var stop = false
          while (!stop) {
            val isRoot = p.resolve(".git").toFile.exists
            list = p :: list
            stop = isRoot || p.getRoot == p
            p = p.getParent
          }

          list
        }
      )

    def loadIgnore(ignorePath: Path, base: Path): Option[IgnoreFile] =
      loadIgnoreCache.getOrElseUpdate(
        (ignorePath, base), {
          val ignoreFile = ignorePath.toFile
          if (ignoreFile.exists && ignoreFile.isFile && ignoreFile.canRead) {
            val ignore = IgnoreFile.load(ignorePath, base)
            Some(ignore)
          } else None
        }
      )

    def ignoreFile(path: Path): Seq[Path] =
      Seq(path.resolve(".gitignore"))

    def rootIgnoreFile(rootPath: Path): Seq[Path] = {
      val home = System.getProperty("user.home")
      Seq(
        // TODO: Read `git config core.excludeFile` instead.
        Path.of(home, ".config/git/ignore"),
        rootPath.resolve(".git/info/exclude")
      )
    }
  }
}
