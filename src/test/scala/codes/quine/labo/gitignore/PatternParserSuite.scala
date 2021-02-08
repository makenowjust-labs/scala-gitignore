package codes.quine.labo.gitignore

import minitest.SimpleTestSuite

import java.nio.file.Path
import scala.io.Source

object PatternParserSuite extends SimpleTestSuite {
  for (i <- (1 to 2)) {
    test(f"PatternParser.parse: test-$i%02d") {
      val gitignorePath = Resource.gitignorePath(Path.of(f"test-$i%02d.gitignore"))
      val txtPath = Resource.gitignorePath(Path.of(f"test-$i%02d.txt"))
      val gitignoreSource = Source.fromFile(gitignorePath.toFile).mkString

      val expected = Source.fromFile(txtPath.toFile).mkString
      val result = PatternParser.parse(gitignoreSource, Resource.gitignorePath).map(p => p.toString + "\n").mkString
      assertEquals(result, expected)
    }
  }
}
