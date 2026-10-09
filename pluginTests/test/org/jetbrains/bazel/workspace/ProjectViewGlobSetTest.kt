package org.jetbrains.bazel.workspace

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createDirectories

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
  fun `should match a filename wildcard that ends with an asterisk`() {
    val glob = glob("*Test*")
    glob.matches(Path("a/b/SomeTest.java")) shouldBe true
    glob.matches(Path("a/b/SomeTestUtil.java")) shouldBe true
    glob.matches(Path("a/b/ActualClass.java")) shouldBe false
  }

  @Test
  fun `should match a path wildcard that ends with an asterisk`() {
    val glob = glob("src/*/test/*")
    glob.matches(Path("src/module/test/A.java")) shouldBe true
    glob.matches(Path("src/module/main/A.java")) shouldBe false
    glob.impliesRecursiveMatch(Path("src/module/test")) shouldBe false
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
    glob(pattern, Path("/project").toAbsolutePath()).matches(Path("/project/test").toAbsolutePath()) shouldBe true
    glob(pattern, Path("/project").toAbsolutePath()).matches(Path("project/test")) shouldBe false
    glob(pattern, Path("/").toAbsolutePath()).matches(Path("/project/test").toAbsolutePath()) shouldBe false
    glob(pattern, Path("/some/random/dir").toAbsolutePath()).matches(Path("/project/test").toAbsolutePath()) shouldBe false
    glob(pattern, Path("/some/random/dir").toAbsolutePath()).matches(Path("project/test")) shouldBe false
    glob(pattern, Path("/some/random/dir").toAbsolutePath()).matches(Path("test")) shouldBe true
    shouldThrow<IllegalArgumentException> { glob(pattern, Path("some/random/dir")).matches(Path("test")) }
  }

  @Test
  fun `match-all pattern should match workspace paths only`() {
    val rootDir = Path("/project").toAbsolutePath()
    val glob = glob("*", rootDir)
    glob.matches(Path("a/b/asd.xml")) shouldBe true
    glob.matches(rootDir.resolve("a/b/asd.xml")) shouldBe true
    glob.matches(Path("/other/asd.xml").toAbsolutePath()) shouldBe false
  }

  @Test
  fun `directory pattern should imply a recursive match for an absolute directory`(@TempDir rootDir: Path) {
    val docs = rootDir.resolve("docs/nested").createDirectories()
    val src = rootDir.resolve("src").createDirectories()
    val glob = glob("docs/*", rootDir)
    glob.impliesRecursiveMatch(rootDir.resolve("docs")) shouldBe true
    glob.impliesRecursiveMatch(docs) shouldBe true
    glob.impliesRecursiveMatch(src) shouldBe false
    glob.impliesRecursiveMatch(rootDir) shouldBe false
  }

  @Test
  fun `directory pattern should not match a sibling with the same prefix`() {
    val glob = glob("docs/*")
    glob.matches(Path("docs/guide.md")) shouldBe true
    glob.matches(Path("docsfoo/guide.md")) shouldBe false
    glob.matches(Path("docs_old/guide.md")) shouldBe false
    glob.impliesRecursiveMatch(Path("docsfoo")) shouldBe false
  }

  @Test
  fun `match-all pattern should imply a recursive match only inside the workspace`(@TempDir rootDir: Path) {
    val src = rootDir.resolve("src").createDirectories()
    val glob = glob("*", src)
    glob.impliesRecursiveMatch(src) shouldBe true
    glob.impliesRecursiveMatch(rootDir) shouldBe false
  }

  private fun glob(pattern: String, rootDir: Path = Path("/").toAbsolutePath()): ProjectViewGlobSet =
    ProjectViewGlobSet.of(rootDir, pattern)
}
