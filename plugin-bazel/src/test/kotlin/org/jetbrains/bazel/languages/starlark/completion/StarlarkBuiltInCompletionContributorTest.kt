package org.jetbrains.bazel.languages.starlark.completion

import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixture4TestCase
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.junit.Test

class StarlarkBuiltInCompletionContributorTest : LightPlatformCodeInsightFixture4TestCase() {
  @Test
  fun `should complete buildIn const`() {
    // given
    myFixture.configureByText("const.bzl", "")
    myFixture.type("E")

    // when
    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    // then
    lookups shouldContainExactlyInAnyOrder listOf("False", "None", "True")
  }

  @Test
  fun `should complete buildIn function`() {
    // given
    myFixture.configureByText("function.bzl", "")
    myFixture.type("o")

    // when
    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    // then
    lookups shouldContainExactlyInAnyOrder listOf("bool", "float", "sorted")
  }

  @Test
  fun `should complete buildIn string method`() {
    //given
    myFixture.configureByText("string.bzl", "")
    myFixture.type(
      """
        |"foo".c
      """.trimMargin()
    )

    // when
    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    // then
    lookups shouldContainExactlyInAnyOrder listOf("capitalize", "count", "isspace", "replace")
  }
}
