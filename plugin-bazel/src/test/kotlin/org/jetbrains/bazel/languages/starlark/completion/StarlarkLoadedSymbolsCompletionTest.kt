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
    myFixture.configureByFile("LoadCompletionInTopLevel.bzl")
    myFixture.type("sym")

    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    lookups shouldContainOnly listOf("some_symbol", "another_symbol")
  }

  @Test
  fun `should complete loaded symbols in function`() {
    myFixture.configureByFile("LoadCompletionInFunction.bzl")
    myFixture.type("sym")

    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    lookups shouldContainOnly listOf("some_symbol", "another_symbol")
  }
}
