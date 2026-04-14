package org.jetbrains.bazel.workspace.indexAdditionalFiles

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.Path

class ProjectViewGlobSetTest {
  @Test
  fun `should match by extension`() {
    val glob = glob("*.xml")
    glob.matches(Path("a/b/asd.xml")) shouldBe true
    glob.matches(Path("a/b/asd.xmls")) shouldBe false
    glob.matches(Path("a/b/xml")) shouldBe false
    glob.matches(Path("a/b/asd.xml/someFile.txt")) shouldBe false
  }

  @Test
  fun `should match by prefix`() {
    val glob = glob("a/b/c")
    glob.matches(Path("a/b")) shouldBe false
    glob.matches(Path("a/b/c/asd.xml")) shouldBe true
    glob.matches(Path("x/a/b/c/asd.xml")) shouldBe false
  }

  @Test
  fun `should match by prefix with trailing slash`() {
    val glob = glob("a/b/c/")
    glob.matches(Path("a/b")) shouldBe false
    glob.matches(Path("a/b/c/asd.xml")) shouldBe true
    glob.matches(Path("x/a/b/c/asd.xml")) shouldBe false
  }

  @Test
  fun `should match by suffix`() {
    val glob = glob("*/test/")
    glob.matches(Path("a/b/test/A.java")) shouldBe true
    glob.matches(Path("a/b/src/A.java")) shouldBe false
  }

  @Test
  fun `should match by suffix 2`() {
    val glob = glob("*Test.java")
    glob.matches(Path("a/b/SomeTest.java")) shouldBe true
    glob.matches(Path("a/b/ActualClass.java")) shouldBe false
  }

  @Test
  fun `matching by filename`() {
    val glob = glob("example")
    glob.matches(Path("a/b/example")) shouldBe true
    glob.matches(Path("example")) shouldBe true
    glob.matches(Path("example/asd.xml")) shouldBe false
    glob.matches(Path("examples")) shouldBe false
  }

  @Test
  fun `matching by directory with one slash`() {
    val glob = glob("example/")
    glob.matches(Path("a/b/example")) shouldBe false
    glob.matches(Path("example")) shouldBe true
    glob.matches(Path("example/asd.xml")) shouldBe true
  }

  @Test
  fun `should match directory by suffix`() {
    val glob = glob("*/test/unit")
    glob.matches(Path("module/test/unit/A.java")) shouldBe true
    glob.matches(Path("module/test/unit")) shouldBe true
    glob.matches(Path("test/unit/A.java")) shouldBe false
    glob.matches(Path("module/test")) shouldBe false
  }

  @Test
  fun `should match directory by suffix 2`() {
    val glob = glob("*-test/unit")
    glob.matches(Path("module/test/unit/A.java")) shouldBe false
    glob.matches(Path("module/java-test/unit/A.java")) shouldBe true
    glob.matches(Path("module/java-test/unit")) shouldBe true
  }

  @Test
  fun `should match java nio's Path`() {
    val pattern = "test/*"
    glob(pattern, Path("/project")).matches(Path("/project/test")) shouldBe true
    glob(pattern, Path("/project")).matches(Path("project/test")) shouldBe false
    glob(pattern, Path("/")).matches(Path("/project/test")) shouldBe false
    glob(pattern, Path("/some/random/dir")).matches(Path("/project/test")) shouldBe false
    glob(pattern, Path("/some/random/dir")).matches(Path("project/test")) shouldBe false
    glob(pattern, Path("/some/random/dir")).matches(Path("test")) shouldBe true
    shouldThrow<IllegalArgumentException> { glob(pattern, Path("some/random/dir")).matches(Path("test")) }
  }

  private fun glob(pattern: String, rootDir: Path = Path("/")): ProjectViewGlobSet = ProjectViewGlobSet(rootDir, listOf(pattern))
}
