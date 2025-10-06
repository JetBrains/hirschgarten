package org.jetbrains.bazel.workspace.indexAdditionalFiles

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ProjectViewGlobSetTest {
  @Test
  fun `should match by extension`() {
    val glob = glob("*.xml")
    glob.matches("a/b/asd.xml") shouldBe true
    glob.matches("a/b/asd.xmls") shouldBe false
    glob.matches("a/b/xml") shouldBe false
  }

  @Test
  fun `should match by prefix`() {
    val glob = glob("a/b/c")
    glob.matches("a/b") shouldBe false
    glob.matches("a/b/c/asd.xml") shouldBe true
    glob.matches("x/a/b/c/asd.xml") shouldBe false
  }

  @Test
  fun `should match by prefix with trailing slash`() {
    val glob = glob("a/b/c/")
    glob.matches("a/b") shouldBe false
    glob.matches("a/b/c/asd.xml") shouldBe true
    glob.matches("x/a/b/c/asd.xml") shouldBe false
  }

  @Test
  fun `should match by suffix`() {
    val glob = glob("*/test/")
    glob.matches("a/b/test/A.java") shouldBe true
    glob.matches("a/b/src/A.java") shouldBe false
  }

  @Test
  fun `should match by suffix 2`() {
    val glob = glob("*Test.java")
    glob.matches("a/b/SomeTest.java") shouldBe true
    glob.matches("a/b/ActualClass.java") shouldBe false
  }

  @Test
  fun `matching by filename`() {
    val glob = glob("example")
    glob.matches("a/b/example") shouldBe true
    glob.matches("example") shouldBe true
    glob.matches("example/asd.xml") shouldBe false
    glob.matches("examples") shouldBe false
  }

  @Test
  fun `matching by directory with one slash`() {
    val glob = glob("example/")
    glob.matches("a/b/example") shouldBe false
    glob.matches("example") shouldBe true
    glob.matches("example/asd.xml") shouldBe true
  }

  private fun glob(pattern: String): ProjectViewGlobSet = ProjectViewGlobSet(listOf(pattern))
}
