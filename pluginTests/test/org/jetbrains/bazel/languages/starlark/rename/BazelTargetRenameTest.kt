package org.jetbrains.bazel.languages.starlark.rename

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.runReadActionBlocking
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.refactoring.BaseRefactoringProcessor
import com.intellij.testFramework.common.timeoutRunBlocking
import io.kotest.assertions.throwables.shouldThrowWithMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bazel.project.BazelProjectFixtures
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@BazelTestApplication
class BazelTargetRenameTest {
  private val myFixture by bazelCodeInsightFixture()

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

    renameBazelTarget("my_lib", "renamed_lib")

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
    openFileInEditor(libFile.virtualFile)
    renameBazelTarget("lib", "renamed_lib")
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
      runReadActionBlocking { otherBuildFile.text },
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
    val readmeContentBefore = runReadActionBlocking { readme.text }

    renameBazelTarget("core_lib", "other_lib")

    assertEquals(readmeContentBefore, runReadActionBlocking { readme.text })
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
      renameBazelTarget("my_lib", "other_lib")
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
    renameBazelTarget("my_lib", "new_lib")
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

  private fun openFileInEditor(file: VirtualFile): Unit = timeoutRunBlocking {
    withContext(Dispatchers.EDT) {
      myFixture.openFileInEditor(file)
    }
  }

  private fun renameBazelTarget(old: String, new: String): Unit = timeoutRunBlocking {
    withContext(Dispatchers.EDT) {
      myFixture.renameBazelTarget(old, new)
    }
  }

  private inline fun shouldThrowConflict(message: String, block: () -> Unit) {
    shouldThrowWithMessage<BaseRefactoringProcessor.ConflictsInTestsException>(message) {
      block()
    }
  }
}

