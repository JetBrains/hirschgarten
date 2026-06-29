package org.jetbrains.bazel.languages.starlark.completion

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.jetbrains.bazel.languages.starlark.fixtures.StarlarkCompletionTestCase
import org.jetbrains.bazel.project.BazelProjectFixtures
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StarlarkStructFieldCompletionTest : StarlarkCompletionTestCase() {
  @Test
  fun `should complete local struct fields`() {
    myFixture.configureByText(
      "defs.bzl",
      """
        symbols = struct(
            alpha = "alpha",
            beta = "beta",
        )

        symbols.<caret>
      """.trimIndent(),
    )

    completeBasicLookupStrings() shouldContainExactlyInAnyOrder listOf("alpha", "beta")
  }

  @Test
  fun `should complete nested struct fields`() {
    myFixture.configureByText(
      "defs.bzl",
      """
        symbols = struct(
            inner = struct(
                delta = "delta",
                gamma = "gamma",
            ),
            sibling = "sibling",
        )

        symbols.inner.<caret>
      """.trimIndent(),
    )

    completeBasicLookupStrings() shouldContainExactlyInAnyOrder listOf("delta", "gamma")
  }

  @Test
  fun `should complete loaded struct fields`() {
    BazelProjectFixtures.initializeBazelProject(project, myFixture.tempDirPath)
    myFixture.addFileToProject("MODULE.bazel", "")
    myFixture.addFileToProject("BUILD", "")
    myFixture.addFileToProject(
      "defs.bzl",
      """
        symbols = struct(
            alpha = "alpha",
            beta = "beta",
        )
      """.trimIndent(),
    )
    myFixture.configureByText(
      "consumer.bzl",
      """
        load("//:defs.bzl", "symbols")

        symbols.<caret>
      """.trimIndent(),
    )

    completeBasicLookupStrings() shouldContainExactlyInAnyOrder listOf("alpha", "beta")
  }

  @Test
  fun `should complete nested struct fields defined in different files`() {
    BazelProjectFixtures.initializeBazelProject(project, myFixture.tempDirPath)
    myFixture.addFileToProject("MODULE.bazel", "")
    myFixture.addFileToProject("BUILD", "")
    myFixture.addFileToProject(
      "inner.bzl",
      """
        inner_symbols = struct(
            delta = "delta",
            gamma = "gamma",
        )
      """.trimIndent(),
    )
    myFixture.addFileToProject(
      "defs.bzl",
      """
        load("//:inner.bzl", "inner_symbols")

        symbols = struct(
            inner = inner_symbols,
            sibling = "sibling",
        )
      """.trimIndent(),
    )
    myFixture.configureByText(
      "consumer.bzl",
      """
        load("//:defs.bzl", "symbols")

        symbols.inner.<caret>
      """.trimIndent(),
    )

    completeBasicLookupStrings() shouldContainExactlyInAnyOrder listOf("delta", "gamma")
  }

  private fun completeBasicLookupStrings(): List<String> =
    myFixture.completeBasic().flatMap { it.allLookupStrings }
}
