package org.jetbrains.bazel.languages.starlark.completion

import io.kotest.matchers.collections.shouldContainOnly
import org.jetbrains.bazel.languages.starlark.fixtures.StarlarkCompletionTestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StarlarkArgumentCompletionTest : StarlarkCompletionTestCase() {
  @Test
  fun `should complete arguments in function call (excluding variadic ones)`() {
    myFixture.configureByFile("ArgumentCompletion.bzl")
    myFixture.type("ar")

    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    lookups shouldContainOnly listOf("bar = ", "arg = ")
  }

  @Test
  fun `should not complete arguments that are already passed`() {
    myFixture.configureByFile("PassedArgumentCompletion.bzl")
    myFixture.type("ar")

    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    lookups shouldContainOnly listOf("arg = ", "dar = ")
  }
}
