package org.jetbrains.bazel.languages.starlark.completion

import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixture4TestCase
import io.kotest.matchers.shouldBe
import org.junit.Test

class StarlarkParamsCompletionContributorTest : LightPlatformCodeInsightFixture4TestCase() {
  @Test
  fun `should complete args`() {
    // given
    myFixture.configureByText("args.bzl", "")
    myFixture.type("def foo(*a")

    // when
    val lookups = myFixture.completeBasic()

    // then
    lookups shouldBe null
  }

  @Test
  fun `should complete kwargs`() {
    // given
    myFixture.configureByText("kwargs.bzl", "")
    myFixture.type("def foo(**k")

    // when
    val lookups = myFixture.completeBasic()

    // then
    lookups shouldBe null
  }
}
