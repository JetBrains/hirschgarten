package org.jetbrains.bazel.workspace.fileEvents

import com.intellij.openapi.application.runReadActionBlocking
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.languages.starlark.repomapping.injectCanonicalRepoNameToPath
import org.jetbrains.bazel.languages.starlark.utils.StarlarkSrcsListEval
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.nio.file.Path

@RunWith(JUnit4::class)
class FindTargetsForSourceFileTest : BasePlatformTestCase() {
  override fun setUp() {
    super.setUp()
    val moduleFile = myFixture.addFileToProject("MODULE.bazel", "")
    val rootPath = Path.of(moduleFile.virtualFile.parent.path)
    project.injectCanonicalRepoNameToPath(mapOf("" to rootPath))
  }

  @Test
  fun `returns target when srcs is a string literal matching the file`() {
    addBuildFile("pkg", """java_library(name = "lib", srcs = "Foo.kt")""")
    val source = addSource("pkg/Foo.kt")

    findTargets(source) shouldBe listOf("@//pkg:lib")
  }

  @Test
  fun `returns target when srcs is a glob matching the file`() {
    addBuildFile("pkg", """java_library(name = "lib", srcs = glob(["**/*.kt"]))""")
    val source = addSource("pkg/sub/Foo.kt")

    findTargets(source) shouldBe listOf("@//pkg:lib")
  }

  @Test
  fun `returns target when srcs list contains the file`() {
    addBuildFile("pkg", """java_library(name = "lib", srcs = ["Foo.kt", "Bar.kt"])""")
    val source = addSource("pkg/Bar.kt")

    findTargets(source) shouldBe listOf("@//pkg:lib")
  }

  @Test
  fun `returns every target whose srcs match`() {
    addBuildFile(
      "pkg",
      """
      |java_library(name = "alpha", srcs = glob(["*.kt"]))
      |java_library(name = "beta", srcs = ["Foo.kt"])
      |java_library(name = "gamma", srcs = ["Other.kt"])
      """.trimMargin(),
    )
    val source = addSource("pkg/Foo.kt")

    findTargets(source) shouldContainExactlyInAnyOrder listOf("@//pkg:alpha", "@//pkg:beta")
  }

  @Test
  fun `returns empty list when no target srcs match`() {
    addBuildFile("pkg", """java_library(name = "lib", srcs = "Other.kt")""")
    val source = addSource("pkg/Foo.kt")

    findTargets(source).shouldBeEmpty()
  }

  @Test
  fun `returns empty list when no BUILD file between file and repo root`() {
    val source = addSource("pkg/Foo.kt")

    findTargets(source).shouldBeEmpty()
  }

  @Test
  fun `picks the nearer BUILD file when BUILD files exist at multiple levels`() {
    addBuildFile("outer", """java_library(name = "outer", srcs = glob(["**/*.kt"]))""")
    addBuildFile("outer/inner", """java_library(name = "inner", srcs = "Foo.kt")""")
    val source = addSource("outer/inner/Foo.kt")

    findTargets(source) shouldBe listOf("@//outer/inner:inner")
  }

  @Test
  fun `does not descend into nested BUILD files when the source is above them`() {
    addBuildFile("outer", """java_library(name = "outer", srcs = "Foo.kt")""")
    addBuildFile("outer/inner", """java_library(name = "inner", srcs = "Bar.kt")""")
    val source = addSource("outer/Foo.kt")

    findTargets(source) shouldBe listOf("@//outer:outer")
  }

  @Test
  fun `relative path is computed against BUILD's directory not repo root`() {
    addBuildFile("outer", """java_library(name = "lib", srcs = "sub/Foo.kt")""")
    val source = addSource("outer/sub/Foo.kt")

    findTargets(source) shouldBe listOf("@//outer:lib")
  }

  private fun addBuildFile(relativeDir: String, content: String) {
    myFixture.addFileToProject("$relativeDir/BUILD.bazel", content.trimIndent())
  }

  private fun addSource(relativePath: String): VirtualFile =
    myFixture.addFileToProject(relativePath, "").virtualFile

  private fun findTargets(file: VirtualFile): List<String> =
    runReadActionBlocking {
      StarlarkSrcsListEval(project).findTargetsForSourceFile(file).map {it.toString()}
    }
}
