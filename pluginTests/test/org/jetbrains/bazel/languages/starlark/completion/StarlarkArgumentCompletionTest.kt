package org.jetbrains.bazel.languages.starlark.completion

import io.kotest.matchers.collections.shouldContainOnly
import org.jetbrains.bazel.commons.BazelRelease
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.jetbrains.bazel.test.framework.completeBasicLookupStrings
import org.junit.jupiter.api.Test

@BazelTestApplication
class StarlarkArgumentCompletionTest {
  private val myFixture by bazelCodeInsightFixture(release = BazelRelease(9, 1), testDataDir = "starlark/completion")

  @Test
  fun `should complete arguments in function call (excluding variadic ones)`() {
    myFixture.configureByFile("ArgumentCompletion.bzl")
    myFixture.type("ar")

    val lookups = myFixture.completeBasicLookupStrings()

    lookups shouldContainOnly listOf("bar = ", "arg = ")
  }

  @Test
  fun `should not complete arguments that are already passed`() {
    myFixture.configureByFile("PassedArgumentCompletion.bzl")
    myFixture.type("ar")

    val lookups = myFixture.completeBasicLookupStrings()

    lookups shouldContainOnly listOf("arg = ", "dar = ")
  }
}
