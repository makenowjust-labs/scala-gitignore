package codes.quine.labo.gitignore

import java.nio.file.Path

import scala.io.Source

import minitest.SimpleTestSuite

object GitIgnoreSuite extends SimpleTestSuite {
  class TestConfig extends Config.Default {
    override def findRoot(path: Path): List[Path] =
      findRootCache.getOrElseUpdate(
        path, {
          var list = List.empty[Path]

          var p = path
          var stop = false
          while (!stop) {
            val isRoot = p.resolve(".root").toFile.exists
            list = p :: list
            stop = isRoot || p.getRoot == p
            p = p.getParent
          }

          list
        }
      )

    override def ignoreFile(path: Path): Seq[Path] =
      Seq(path.resolve("test.gitignore"))

    override def rootIgnoreFile(rootPath: Path): Seq[Path] = Seq.empty
  }

  for (i <- (1 to 3)) {
    test(f"GitIgnore.traverse: test-$i%02d") {
      val rootPath = Resource.traversePath(Path.of(f"test-$i%02d")).toAbsolutePath
      val txtPath = rootPath.resolve("test.txt")
      val expected = Source.fromFile(txtPath.toFile).getLines().toList

      val list = List.newBuilder[String]
      GitIgnore.traverse(rootPath, new TestConfig) { path =>
        val relPath = rootPath.relativize(path)
        list.addOne(relPath.toString)
      }

      assertEquals(list.result(), expected)
    }
  }
}
