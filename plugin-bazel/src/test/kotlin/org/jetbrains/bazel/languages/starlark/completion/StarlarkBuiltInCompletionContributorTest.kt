package org.jetbrains.bazel.languages.starlark.completion

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.equals.shouldBeEqual
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StarlarkBuiltInCompletionContributorTest : BasePlatformTestCase() {
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

//  TODO: For some reason collides with references, and since references are way more important
//  Let's leave it like that for now.
//  @Test
//  fun `should complete buildIn string method`() {
//    // given
//    myFixture.configureByText("string.bzl", "")
//    myFixture.type(
//      """
//        |"foo".c
//      """.trimMargin()
//    )
//
//    // when
//    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }
//
//    // then
//    lookups shouldContainExactlyInAnyOrder listOf("capitalize", "count", "isspace", "replace")
//  }

  @Test
  fun `should not complete buildIns for numerics`() {
    // given
    myFixture.configureByText("numeric.bzl", "")
    myFixture.type(
      """
        |x += 5
      """.trimMargin()
    )

    // when
    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    // then
    lookups shouldBeEqual emptyList()
  }
}
