package org.jetbrains.bazel.workspace

import com.intellij.openapi.application.runWriteAction
import com.intellij.testFramework.LightVirtualFile
import com.intellij.testFramework.utils.vfs.createDirectory
import com.intellij.testFramework.utils.vfs.createFile
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.starlark.repomapping.calculateLabel
import org.jetbrains.bazel.languages.starlark.repomapping.calculateWildcardLabel
import org.jetbrains.bazel.workspace.model.test.framework.WorkspaceModelBaseTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class BazelRepoMappingUtilsTest : WorkspaceModelBaseTest() {
  @BeforeEach
  fun setUp() {
    val rootDir = project.rootDir.toNioPath()
    project.setRepoMapping {
      mainRepo(rootDir)
      repo("community~", rootDir.resolve("community"), apparentName = "community")
    }
    runWriteAction {
      project.rootDir.createFile("MODULE.bazel")
      project.rootDir.createChildDirectory(this, "community").createFile("MODULE.bazel")
    }
  }

  @Test
  fun checkRoot() {
    val buildFile = runWriteAction { project.rootDir.createFile("BUILD.bazel") }

    calculateLabel(project, buildFile, "target") shouldBe Label.parse("//:target")
    calculateLabel(project, buildFile) shouldBe Label.parse("//")
    calculateWildcardLabel(project, project.rootDir) shouldBe Label.parse("//...")
  }

  @Test
  fun checkRootSubdir() {
    val packageDir = runWriteAction {
      project.rootDir.createChildDirectory(this, "a").createChildDirectory("this", "b")
    }
    val buildFile = runWriteAction { packageDir.createFile("BUILD") }

    calculateLabel(project, buildFile, "mytarget") shouldBe Label.parse("//a/b:mytarget")
    calculateWildcardLabel(project, packageDir) shouldBe Label.parse("//a/b/...")
  }

  @Test
  fun testRepository() {
    val packageDir = runWriteAction {
      project.rootDir.findChild("community")!!.createDirectory("package")
    }
    val buildFile = runWriteAction { packageDir.createFile("BUILD") }

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
