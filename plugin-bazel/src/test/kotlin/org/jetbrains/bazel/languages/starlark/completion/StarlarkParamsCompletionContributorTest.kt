package org.jetbrains.bazel.languages.starlark.completion

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StarlarkParamsCompletionContributorTest : BasePlatformTestCase() {
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
