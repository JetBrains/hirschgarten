package org.jetbrains.bazel.languages.starlark.rename

import com.intellij.refactoring.BaseRefactoringProcessor
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.kotest.assertions.throwables.shouldThrowWithMessage
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bazel.project.BazelProjectFixtures
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BazelTargetRenameTest : BasePlatformTestCase() {

  @Before
  fun setupBuildEnvironment() {
    BazelProjectFixtures.initializeBazelProject(project, myFixture.tempDirPath)
  }

  @Test
  fun `rename target should update short label reference in same file`() {
    myFixture.configureByText(
      "BUILD",
      """
      java_library(
          name = "my_lib",
          srcs = ["Lib.java"],
      )
      java_binary(
          name = "my_app",
          deps = [":my_lib"],
      )
      """.trimIndent(),
    )

    myFixture.renameBazelTarget("my_lib", "renamed_lib")

    myFixture.checkResult(
      """
      java_library(
          name = "renamed_lib",
          srcs = ["Lib.java"],
      )
      java_binary(
          name = "my_app",
          deps = [":renamed_lib"],
      )
      """.trimIndent(),
    )
  }

  @Test
  fun `rename target should update absolute label reference in other BUILD file`() {
    myFixture.addFileToProject("MODULE.bazel", "")
    val libFile = myFixture.addFileToProject(
      "core/BUILD",
      """
      java_library(
          name = "lib",
          srcs = ["Lib.java"],
      )
      """.trimIndent()
    )
    val otherBuildFile = myFixture.addFileToProject(
      "BUILD",
      """
      java_binary(
          name = "app",
          deps = ["//core:lib"],
      )
      """.trimIndent(),
    )
    myFixture.openFileInEditor(libFile.virtualFile)
    myFixture.renameBazelTarget("lib", "renamed_lib")
    myFixture.checkResult(
      """
      java_library(
          name = "renamed_lib",
          srcs = ["Lib.java"],
      )
      """.trimIndent(),
    )
    assertEquals(
      """
      java_binary(
          name = "app",
          deps = ["//core:renamed_lib"],
      )
      """.trimIndent(),
      otherBuildFile.text,
    )
  }

  @Test
  fun `rename target should not modify non-starlark files`() {
    myFixture.addFileToProject("MODULE.bazel", "")
    myFixture.configureByText(
      "BUILD",
      """
      java_library(
          name = "core_lib",
          srcs = ["Lib.java"],
      )
      """.trimIndent(),
    )
    val readme = myFixture.addFileToProject(
      "README.md",
      """
      # Project
      This project uses `core_lib` for core functionality.
      """.trimIndent(),
    )
    val readmeContentBefore = readme.text

    myFixture.renameBazelTarget("core_lib", "other_lib")

    assertEquals(readmeContentBefore, readme.text)
  }

  @Test
  fun `rename target should detect conflict with existing target name`() {
    myFixture.configureByText(
      "BUILD",
      """
      java_library(
          name = "my_lib",
          srcs = ["Lib.java"],
      )
      java_library(
          name = "other_lib",
          srcs = ["Other.java"],
      )
      """.trimIndent(),
    )
    shouldThrowConflict(StarlarkBundle.message("rename.target.conflict.already.exists", "other_lib", "BUILD")) {
      myFixture.renameBazelTarget("my_lib", "other_lib")
    }
  }

  @Test
  fun `rename should not rename in bzl file`() {
    myFixture.configureByText(
      "macros.bzl",
      """
      def my_macro():
          java_library(
              name = "my_lib",
              srcs = ["Lib.java"],
          )
      """.trimIndent(),
    )
    myFixture.renameBazelTarget("my_lib", "new_lib")
    myFixture.checkResult(
      """
      def my_macro():
          java_library(
              name = "my_lib",
              srcs = ["Lib.java"],
          )
      """.trimIndent()
    )
  }

  private inline fun shouldThrowConflict(message: String, block: () -> Unit) {
    shouldThrowWithMessage<BaseRefactoringProcessor.ConflictsInTestsException>(message) {
      block()
    }
  }
}

