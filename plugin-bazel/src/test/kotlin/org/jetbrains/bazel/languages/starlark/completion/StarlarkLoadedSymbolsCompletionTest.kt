package org.jetbrains.bazel.languages.starlark.completion

import com.google.idea.testing.runfiles.Runfiles
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.kotest.matchers.collections.shouldContainOnly
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.io.path.pathString

@RunWith(JUnit4::class)
class StarlarkLoadedSymbolsCompletionTest : BasePlatformTestCase() {
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

  override fun getTestDataPath(): String = Runfiles.runfilesPath("/plugin-bazel/src/test/testData/starlark/completion/").pathString
}
