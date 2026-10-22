package org.jetbrains.bazel.languages.starlark.completion

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.jetbrains.bazel.test.framework.completeBasicLookupStrings
import org.jetbrains.bazel.test.framework.testBazel
import org.jetbrains.bazel.test.framework.testproject.javaLibrary
import org.jetbrains.bazel.test.framework.testproject.sync
import org.junit.jupiter.api.Test

@BazelTestApplication
class BazelTargetCompletionTest {
  private val myFixture by bazelCodeInsightFixture()

  @Test
  fun `should complete in deps`() {
    myFixture.testBazel.sync { targets(javaLibrary("//target1"), javaLibrary("//target2")) }

    myFixture.configureByText(
      "BUILD",
      """
      load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
      kt_jvm_library(
        name = "test",
        srcs = glob(["*.kt",]),
        visibility = [],
        deps = [
          "//another_test_target",
          <caret>
        ],
      )
      """.trimMargin(),
    )
    myFixture.type("\"")
    val lookups = myFixture.completeBasicLookupStrings()

    lookups shouldContainExactlyInAnyOrder listOf("\"//target1\"", "\"//target2\"")
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
  fun `should search for targets`() {
    myFixture.testBazel.sync { targets(javaLibrary("//abcd1299"), javaLibrary("//abcd1239"), javaLibrary("//abcd1234")) }

    myFixture.configureByText(
      "BUILD",
      """
      load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
      kt_jvm_library(deps = [<caret>],)
      """.trimMargin(),
    )
    myFixture.type("\"12")
    val lookups = myFixture.completeBasicLookupStrings()

    myFixture.type("3")
    val lookups2 = myFixture.completeBasicLookupStrings()

    myFixture.type("4")
    val lookups3 = myFixture.completeBasicLookupStrings()

    lookups shouldContainExactlyInAnyOrder listOf("\"//abcd1234\"", "\"//abcd1239\"", "\"//abcd1299\"")
    lookups2 shouldContainExactlyInAnyOrder listOf("\"//abcd1234\"", "\"//abcd1239\"")
    lookups3 shouldContainExactlyInAnyOrder listOf("\"//abcd1234\"")
  }
}
