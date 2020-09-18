package codes.quine.labo.gitignore

import java.nio.file.Path

import scala.collection.mutable

object GitIgnore {
  def traverse(path: Path, config: Config = Config())(handle: Path => Unit): Unit =
    recurse(path)(!isIgnored(_, config))(path => if (!isIgnored(path, config)) handle(path))

  def isIgnored(path: Path, config: Config = Config()): Boolean = {
    val absPath = path.toAbsolutePath.normalize
    if (absPath.getFileName().toString == ".git") return true

    val isDir = absPath.toFile.isDirectory
    val paths = config.findRoot(if (isDir) absPath else absPath.getParent)
    val rootPath = paths.head

    var ignore = IgnoreFile(Seq.empty)
    def load(path: Path, base: Path): Unit =
      config.loadIgnore(path, base).foreach(ignore ++= _)

    for (ignorePath <- config.rootIgnoreFile(rootPath)) load(ignorePath, rootPath)

    for (path <- paths) {
      for (ignorePath <- config.ignoreFile(path)) load(ignorePath, rootPath)
      if (ignore.isIgnored(path)) return true
    }

    if (isDir) false else ignore.isIgnored(absPath)
  }

  def recurse(path: Path)(handleDir: Path => Boolean)(handleFile: Path => Unit): Unit = {
    val stack = mutable.Stack(path.toAbsolutePath.normalize)

    while (stack.nonEmpty) {
      val path = stack.pop()
      val file = path.toFile
      if (file.isDirectory) {
        if (handleDir(path)) {
          stack.pushAll(file.list.sorted(Ordering[String].reverse).map(path.resolve(_)))
        }
      } else {
        handleFile(path)
      }
    }
  }
}
