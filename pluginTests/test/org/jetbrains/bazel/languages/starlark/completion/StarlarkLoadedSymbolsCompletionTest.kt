package org.jetbrains.bazel.languages.starlark.completion

import io.kotest.matchers.collections.shouldContainOnly
import org.jetbrains.bazel.commons.BazelRelease
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.jetbrains.bazel.test.framework.completeBasicLookupStrings
import org.junit.jupiter.api.Test

@BazelTestApplication
class StarlarkLoadedSymbolsCompletionTest {
  private val myFixture by bazelCodeInsightFixture(release = BazelRelease(9, 1), testDataDir = "starlark/completion")

  @Test
  fun `should complete loaded symbols in top level`() {
    myFixture.configureByFile("LoadCompletionInTopLevel.bzl")
    myFixture.type("sym")

    val lookups = myFixture.completeBasicLookupStrings()

    lookups shouldContainOnly listOf("some_symbol", "another_symbol")
  }

  @Test
  fun `should complete loaded symbols in function`() {
    myFixture.configureByFile("LoadCompletionInFunction.bzl")
    myFixture.type("sym")

    val lookups = myFixture.completeBasicLookupStrings()

    lookups shouldContainOnly listOf("some_symbol", "another_symbol")
  }
}
