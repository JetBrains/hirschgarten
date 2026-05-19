package org.jetbrains.bazel.languages.starlark.findusages

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldStartWith
import org.jetbrains.bazel.project.BazelProjectFixtures
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BazelTargetFindUsagesTest : BasePlatformTestCase() {
  @Before
  fun setupBuildEnvironment() {
    BazelProjectFixtures.initializeBazelProject(project, myFixture.tempDirPath)
  }

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

    val usages = myFixture.findBazelTargetUsages("lib")
    usages.shouldHaveSingleElement { it.element?.text == "\":lib\"" }
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
    myFixture.openFileInEditor(libFile.virtualFile)
    val usages = myFixture.findBazelTargetUsages("lib")
    usages.shouldHaveSingleElement { it.element?.text == "\"//core:lib\"" }
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
    myFixture.openFileInEditor(libFile.virtualFile)

    val usages = myFixture.findBazelTargetUsages("lib")
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
      This project uses core_lib for core functionality.
      """.trimIndent(),
    )

    val usages = myFixture.findBazelTargetUsages("core_lib")
    usages shouldHaveSize 0
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

    myFixture.findBazelTargetUsages("lib").shouldBeEmpty()
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
    val error = shouldThrow<AssertionError> { myFixture.findBazelTargetUsages("lib") }
    error.message shouldStartWith "Cannot find handler"
  }
}
