package org.jetbrains.bazel.languages.starlark.findusages

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.runReadActionBlocking
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.common.timeoutRunBlocking
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.collections.shouldHaveSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.project.BazelProjectFixtures
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.junit.jupiter.api.Test

@BazelTestApplication
class BazelTargetFindUsagesTest {
  private val myFixture by bazelCodeInsightFixture()

  @Test
  fun `should find usage of target name in same BUILD file`() {
    myFixture.configureByText(
      "BUILD",
      """
      java_library(
          name = "lib",
          srcs = ["Lib.java"],
      )
      java_binary(
          name = "my_app",
          deps = [":lib"],
      )
      """.trimIndent(),
    )

    val usages = runReadActionBlocking { myFixture.findBazelTargetUsages("lib") }
    runReadActionBlocking { usages.shouldHaveSingleElement { it.element?.text == "\":lib\"" } }
  }

  @Test
  fun `should find usage of target name across BUILD files`() {
    myFixture.addFileToProject("MODULE.bazel", "")
    val libFile = myFixture.addFileToProject(
      "core/BUILD",
      """
      java_library(
          name = "lib",
          srcs = ["Lib.java"],
      )
      """.trimIndent(),
    )
    myFixture.addFileToProject(
      "BUILD",
      """
      java_binary(
          name = "app",
          deps = ["//core:lib"],
      )
      """.trimIndent(),
    )
    openFileInEditor(libFile.virtualFile)
    val usages = runReadActionBlocking { myFixture.findBazelTargetUsages("lib") }
    runReadActionBlocking { usages.shouldHaveSingleElement { it.element?.text == "\"//core:lib\"" } }
  }

  @Test
  fun `should find multiple usages of target name`() {
    myFixture.addFileToProject("MODULE.bazel", "")
    val libFile = myFixture.addFileToProject(
      "core/BUILD",
      """
      java_library(
          name = "lib",
          srcs = ["Lib.java"],
      )
      java_library(
          name = "lib_utils",
          srcs = ["LibUtils.java"],
          deps = [":lib"],
      )
      """.trimIndent(),
    )
    myFixture.addFileToProject(
      "BUILD",
      """
      java_binary(
          name = "app1",
          deps = ["//core:lib"],
      )
      java_binary(
          name = "app2",
          deps = ["//core:lib"],
      )
      """.trimIndent(),
    )
    openFileInEditor(libFile.virtualFile)

    val usages = runReadActionBlocking { myFixture.findBazelTargetUsages("lib") }
    usages shouldHaveSize 3
  }

  @Test
  fun `should not find usages in non-starlark files`() {
    myFixture.configureByText(
      "BUILD",
      """
      java_library(
          name = "core_lib",
          srcs = ["Lib.java"],
      )
      """.trimIndent(),
    )
    myFixture.addFileToProject(
      "README.md",
      """
      # Project
      This project uses `core_lib` for core functionality.
      """.trimIndent(),
    )

    runReadActionBlocking { myFixture.findBazelTargetUsages("core_lib") }.shouldBeEmpty()
  }

  @Test
  fun `should not find usage when dep is a substring match`() {
    myFixture.configureByText(
      "BUILD",
      """
      java_library(
          name = "lib",
          srcs = ["Lib.java"],
      )
      java_binary(
          name = "app",
          deps = ["//other:lib"],
      )
      """.trimIndent(),
    )

    runReadActionBlocking { myFixture.findBazelTargetUsages("lib") }.shouldBeEmpty()
  }

  @Test
  fun `should not find usages for name attribute in bzl file`() {
    myFixture.addFileToProject("MODULE", "")
    myFixture.addFileToProject(
      "BUILD",
      """
      java_library(
          name = "lib",
          srcs = ["Lib.java"],
      )
      java_binary(
          name = "app",
          deps = [":lib"],
      )
      """.trimIndent(),
    )
    myFixture.configureByText(
      "macros.bzl",
      """
      def my_macro():
          java_library(
              name = "lib",
          )
      """.trimIndent(),
    )
    runReadActionBlocking { myFixture.findBazelTargetUsages("lib") }.shouldBeEmpty()
  }

  @Test
  fun `should find default target shorthand usage`() {
    myFixture.addFileToProject("MODULE.bazel", "")
    val libFile = myFixture.addFileToProject(
      "core/BUILD",
      """
      java_library(
          name = "core",
          srcs = ["Core.java"],
      )
      """.trimIndent(),
    )
    myFixture.addFileToProject(
      "app/BUILD",
      """
      java_binary(
          name = "app",
          deps = ["//core"],
      )
      """.trimIndent(),
    )
    openFileInEditor(libFile.virtualFile)
    val usages = runReadActionBlocking { myFixture.findBazelTargetUsages("core") }
    runReadActionBlocking { usages.shouldHaveSingleElement { it.element?.text == "\"//core\"" } }
  }

  @Test
  fun `should find default target shorthand usage with multi-segment package`() {
    myFixture.addFileToProject("MODULE.bazel", "")
    val libFile = myFixture.addFileToProject(
      "lib/bar/BUILD",
      """
      java_library(
          name = "bar",
          srcs = ["Bar.java"],
      )
      """.trimIndent(),
    )
    myFixture.addFileToProject(
      "app/BUILD",
      """
      java_binary(
          name = "app",
          deps = ["//lib/bar"],
      )
      """.trimIndent(),
    )
    openFileInEditor(libFile.virtualFile)
    val usages = runReadActionBlocking { myFixture.findBazelTargetUsages("bar") }
    runReadActionBlocking { usages.shouldHaveSingleElement { it.element?.text == "\"//lib/bar\"" } }
  }

  @Test
  fun `should find usage from root package target`() {
    myFixture.addFileToProject("MODULE.bazel", "")
    val libFile = myFixture.addFileToProject(
      "BUILD",
      """
      java_library(
          name = "lib",
          srcs = ["Lib.java"],
      )
      """.trimIndent(),
    )
    myFixture.addFileToProject(
      "sub/BUILD",
      """
      java_binary(
          name = "app",
          deps = ["//:lib"],
      )
      """.trimIndent(),
    )
    openFileInEditor(libFile.virtualFile)
    val usages = runReadActionBlocking { myFixture.findBazelTargetUsages("lib") }
    runReadActionBlocking { usages.shouldHaveSingleElement { it.element?.text == "\"//:lib\"" } }
  }

  private fun openFileInEditor(file: VirtualFile): Unit = timeoutRunBlocking {
    withContext(Dispatchers.EDT) {
      myFixture.openFileInEditor(file)
    }
  }
}
