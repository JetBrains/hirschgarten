package org.jetbrains.bazel.languages.starlark.completion

import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.junit.jupiter.api.Test

@BazelTestApplication
class StarlarkParamsCompletionContributorTest {
  private val myFixture by bazelCodeInsightFixture()

  @Test
  fun `should complete args`() {
    myFixture.configureByText("args.bzl", "")
    myFixture.type("def foo(*a")

    val lookups = myFixture.completeBasic()

    lookups shouldBe null
  }

  @Test
  fun `should complete kwargs`() {
    myFixture.configureByText("kwargs.bzl", "")
    myFixture.type("def foo(**k")

    val lookups = myFixture.completeBasic()

    lookups shouldBe null
  }
}
