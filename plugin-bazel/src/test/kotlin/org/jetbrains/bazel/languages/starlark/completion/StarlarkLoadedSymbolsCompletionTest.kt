package org.jetbrains.bazel.languages.starlark.completion

import io.kotest.matchers.collections.shouldContainOnly
import org.jetbrains.bazel.languages.starlark.fixtures.StarlarkCompletionTestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StarlarkLoadedSymbolsCompletionTest : StarlarkCompletionTestCase() {
  @Test
  fun `should complete loaded symbols in top level`() {
    // given
    myFixture.configureByFile("LoadCompletionInTopLevel.bzl")
    myFixture.type("sym")

    // when
    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    // then
    lookups shouldContainOnly listOf("some_symbol", "another_symbol")
  }

  @Test
  fun `should complete loaded symbols in function`() {
    // given
    myFixture.configureByFile("LoadCompletionInFunction.bzl")
    myFixture.type("sym")

    // when
    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    // then
    lookups shouldContainOnly listOf("some_symbol", "another_symbol")
  }
}
