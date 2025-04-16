package org.jetbrains.bazel.languages.starlark.completion

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bsp.protocol.BuildTarget
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.io.path.Path

@RunWith(JUnit4::class)
class StarlarkVisibilityCompletionTest : BasePlatformTestCase() {
  @Before
  fun beforeEach() {
    project.isBazelProject = true
  }

  private fun setTargets(targets: List<String>) {
    project.targetUtils.setTargets(
      targets.map { Label.parse(it) }.associateWith {
        BuildTarget(
          id = it,
          tags = emptyList(),
          languageIds = emptyList(),
          dependencies = emptyList(),
          kind =
            TargetKind(
              kindString = "java_library",
              ruleType = RuleType.LIBRARY,
            ),
          sources = emptyList(),
          resources = emptyList(),
          baseDirectory = Path("/"),
        )
      },
    )
  }

  @Test
  fun `should complete in visibility`() {
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
        visibility = [<caret>],
      )
      """.trimMargin(),
    )
    myFixture.type("\"")
    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    // then
    lookups shouldContainExactlyInAnyOrder
      listOf(
        "\"//visibility:private\"",
        "\"//visibility:public\"",
        "\"//target1\"",
        "\"//target1:__pkg__\"",
        "\"//target1:__subpackages__\"",
        "\"//target2\"",
        "\"//target2:__pkg__\"",
        "\"//target2:__subpackages__\"",
      )
  }

  @Test
  fun `should complete only predefined in visibility`() {
    // given
    setTargets(emptyList())

    // when
    myFixture.configureByText(
      "BUILD",
      """
      load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
      kt_jvm_library(
        name = "test",
        srcs = glob(["*.kt",]),
        visibility = [<caret>],
      )
      """.trimMargin(),
    )
    myFixture.type("\"")
    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    // then
    lookups shouldContainExactlyInAnyOrder
      listOf(
        "\"//visibility:private\"",
        "\"//visibility:public\"",
      )
  }

  @Test
  fun `should not complete in deps`() {
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
    lookups shouldNotContain "\"//visibility:private\""
    lookups shouldNotContain "\"//visibility:public\""
    lookups shouldNotContain "\"//target1:__pkg__\""
    lookups shouldNotContain "\"//target1:__subpackages__\""
    lookups shouldNotContain "\"//target2:__pkg__\""
    lookups shouldNotContain "\"//target2:__subpackages__\""
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
  fun `should search for visibilities`() {
    // given
    setTargets(listOf("//abcd1299", "//abcd1239", "//abcd1234"))

    // when
    myFixture.configureByText(
      "BUILD",
      """
      load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
      kt_jvm_library(visibility = [<caret>],)
      """.trimMargin(),
    )
    myFixture.type("\"")
    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    myFixture.type("12")
    val lookups2 = myFixture.completeBasic().flatMap { it.allLookupStrings }

    myFixture.type("3")
    val lookups3 = myFixture.completeBasic().flatMap { it.allLookupStrings }

    myFixture.type("4")
    val lookups4 = myFixture.completeBasic().flatMap { it.allLookupStrings }

    // then
    lookups shouldContainAll
      listOf(
        "\"//abcd1234:__pkg__\"",
        "\"//abcd1239:__subpackages__\"",
        "\"//abcd1299\"",
        "\"//visibility:private\"",
        "\"//visibility:public\"",
      )
    lookups2 shouldContainAll
      listOf(
        "\"//abcd1234:__subpackages__\"",
        "\"//abcd1239\"",
        "\"//abcd1299:__pkg__\"",
      )

    lookups3 shouldContainAll
      listOf(
        "\"//abcd1234:__subpackages__\"",
        "\"//abcd1239:__pkg__\"",
      )

    lookups4 shouldContainExactlyInAnyOrder
      listOf(
        "\"//abcd1234:__pkg__\"",
        "\"//abcd1234\"",
        "\"//abcd1234:__subpackages__\"",
      )
  }

  @Test
  fun `should prioritize public and private`() {
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
        visibility = [<caret>],
      )
      """.trimMargin(),
    )
    myFixture.type("\"")
    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    // then
    lookups[0] shouldBeIn listOf("\"//visibility:private\"", "\"//visibility:public\"")
    lookups[1] shouldBeIn listOf("\"//visibility:private\"", "\"//visibility:public\"")
  }
}
