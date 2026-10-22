package org.jetbrains.bazel.workspace

import com.intellij.testFramework.LightVirtualFile
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.starlark.repomapping.calculateLabel
import org.jetbrains.bazel.languages.starlark.repomapping.calculateWildcardLabel
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.testproject.sync
import org.jetbrains.bazel.test.framework.testproject.testBazelProjectFixture
import org.jetbrains.bazel.test.framework.testproject.writeFile
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@BazelTestApplication
internal class BazelRepoMappingUtilsTest {
  private val testBazel by testBazelProjectFixture()

  private val project get() = testBazel.project

  @BeforeEach
  fun setUp() {
    testBazel.sync { repositories { local("community~", "community", apparentName = "community") } }
    testBazel.writeFile("community/MODULE.bazel")
  }

  @Test
  fun checkRoot() {
    val buildFile = testBazel.writeFile("BUILD.bazel")

    calculateLabel(project, buildFile, "target") shouldBe Label.parse("//:target")
    calculateLabel(project, buildFile) shouldBe Label.parse("//")
    calculateWildcardLabel(project, project.rootDir) shouldBe Label.parse("//...")
  }

  @Test
  fun checkRootSubdir() {
    val buildFile = testBazel.writeFile("a/b/BUILD")
    val packageDir = buildFile.parent

    calculateLabel(project, buildFile, "mytarget") shouldBe Label.parse("//a/b:mytarget")
    calculateWildcardLabel(project, packageDir) shouldBe Label.parse("//a/b/...")
  }

  @Test
  fun testRepository() {
    val buildFile = testBazel.writeFile("community/package/BUILD")
    val packageDir = buildFile.parent

    calculateLabel(project, buildFile, "mytarget") shouldBe Label.parse("@@community~//package:mytarget")
    calculateWildcardLabel(project, packageDir) shouldBe Label.parse("@@community~//package/...")
  }

  @Test
  fun testRandomDirectory() {
    val packageDir = project.rootDir.parent
    calculateLabel(project, packageDir) shouldBe null
    calculateWildcardLabel(project, packageDir) shouldBe null
  }

  @Test
  fun testFileWithoutParent() {
    val buildFile = LightVirtualFile("BUILD.bazel")

    calculateLabel(project, buildFile, "mytarget") shouldBe null
    calculateLabel(project, buildFile) shouldBe null
  }
}
