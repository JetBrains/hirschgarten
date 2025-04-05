package org.jetbrains.bazel.languages.starlark.completion

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.target.BuildTargetState
import org.jetbrains.bazel.target.TargetUtilsState
import org.jetbrains.bazel.target.targetUtils
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BazelTargetCompletionTest : BasePlatformTestCase() {
  @Before
  fun beforeEach() {
    project.isBazelProject = true
  }

  private fun setTargets(targets: List<String>) {
    val targetsState =
      TargetUtilsState(
        labelToTargetInfo = targets.associateWith { BuildTargetState() },
        moduleIdToTarget = emptyMap(),
        libraryIdToTarget = emptyMap(),
        fileToTarget = emptyMap(),
        fileToExecutableTargets = emptyMap(),
      )
    project.targetUtils.loadState(targetsState)
  }

  @Test
  fun `should complete in deps`() {
    // given
    setTargets(listOf("//target1", "//target2"))

    // when
    myFixture.configureByText(
      "BUILD",
      """
      load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
      kt_jvm_library(
        name = "test",
        srcs = glob(["*.kt",]),
        visible = [],
        deps = [
          "//another_test_target",
          <caret>
        ],
      )
      """.trimMargin(),
    )
    myFixture.type("\"")
    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    // then
    lookups shouldContainExactlyInAnyOrder listOf("\"//target1\"", "\"//target2\"")
  }

  @Test
  fun `should complete in visible`() {
    // given
    setTargets(listOf("//target1", "//target2"))

    // when
    myFixture.configureByText(
      "BUILD",
      """
      load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
      kt_jvm_library(
        name = "test",
        srcs = glob(["*.kt",]),
        visible = [<caret>],
        deps = [
          "//another_test_target",
        ],
      )
      """.trimMargin(),
    )
    myFixture.type("\"")
    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    // then
    lookups shouldContainExactlyInAnyOrder listOf("\"//target1\"", "\"//target2\"")
  }

  @Test
  fun `should not complete outside`() {
    // given
    setTargets(listOf("//target1", "//target2"))

    // when
    myFixture.configureByText("BUILD", "")
    myFixture.type("\"")
    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    // then
    lookups.shouldBeEmpty()
  }

  @Test
  fun `should search for targets`() {
    // given
    setTargets(listOf("//abcd1299", "//abcd1239", "//abcd1234"))

    // when
    myFixture.configureByText(
      "BUILD",
      """
      load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
      kt_jvm_library(deps = [<caret>],)
      """.trimMargin(),
    )
    myFixture.type("\"12")
    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    myFixture.type("3")
    val lookups2 = myFixture.completeBasic().flatMap { it.allLookupStrings }

    myFixture.type("4")
    val lookups3 = myFixture.completeBasic().flatMap { it.allLookupStrings }

    // then
    lookups shouldContainExactlyInAnyOrder listOf("\"//abcd1234\"", "\"//abcd1239\"", "\"//abcd1299\"")
    lookups2 shouldContainExactlyInAnyOrder listOf("\"//abcd1234\"", "\"//abcd1239\"")
    lookups3 shouldContainExactlyInAnyOrder listOf("\"//abcd1234\"")
  }
}
