package org.jetbrains.bazel.languages.starlark.completion

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.jetbrains.bazel.test.framework.completeBasicLookupStrings
import org.jetbrains.bazel.test.framework.testBazel
import org.jetbrains.bazel.test.framework.testproject.javaLibrary
import org.jetbrains.bazel.test.framework.testproject.sync
import org.junit.jupiter.api.Test

@BazelTestApplication
class StarlarkVisibilityCompletionTest {
  private val myFixture by bazelCodeInsightFixture()

  @Test
  fun `should complete in visibility`() {
    myFixture.testBazel.sync { targets(javaLibrary("//target1"), javaLibrary("//target2")) }

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
    val lookups = myFixture.completeBasicLookupStrings()

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
    val lookups = myFixture.completeBasicLookupStrings()

    lookups shouldContainExactlyInAnyOrder
      listOf(
        "\"//visibility:private\"",
        "\"//visibility:public\"",
      )
  }

  @Test
  fun `should not complete in deps`() {
    myFixture.testBazel.sync { targets(javaLibrary("//target1"), javaLibrary("//target2")) }

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
    val lookups = myFixture.completeBasicLookupStrings()

    lookups shouldNotContain "\"//visibility:private\""
    lookups shouldNotContain "\"//visibility:public\""
    lookups shouldNotContain "\"//target1:__pkg__\""
    lookups shouldNotContain "\"//target1:__subpackages__\""
    lookups shouldNotContain "\"//target2:__pkg__\""
    lookups shouldNotContain "\"//target2:__subpackages__\""
  }

  @Test
  fun `should not complete outside`() {
    myFixture.testBazel.sync { targets(javaLibrary("//target1"), javaLibrary("//target2")) }

    myFixture.configureByText("BUILD", "")
    myFixture.type("\"")
    val lookups = myFixture.completeBasicLookupStrings()

    lookups.shouldBeEmpty()
  }

  @Test
  fun `should search for visibilities`() {
    myFixture.testBazel.sync { targets(javaLibrary("//abcd1299"), javaLibrary("//abcd1239"), javaLibrary("//abcd1234")) }

    myFixture.configureByText(
      "BUILD",
      """
      load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
      kt_jvm_library(visibility = [<caret>],)
      """.trimMargin(),
    )
    myFixture.type("\"")
    val lookups = myFixture.completeBasicLookupStrings()

    myFixture.type("12")
    val lookups2 = myFixture.completeBasicLookupStrings()

    myFixture.type("3")
    val lookups3 = myFixture.completeBasicLookupStrings()

    myFixture.type("4")
    val lookups4 = myFixture.completeBasicLookupStrings()

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
    myFixture.testBazel.sync { targets(javaLibrary("//target1"), javaLibrary("//target2")) }

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
    val lookups = myFixture.completeBasicLookupStrings()

    lookups[0] shouldBeIn listOf("\"//visibility:private\"", "\"//visibility:public\"")
    lookups[1] shouldBeIn listOf("\"//visibility:private\"", "\"//visibility:public\"")
  }
}
