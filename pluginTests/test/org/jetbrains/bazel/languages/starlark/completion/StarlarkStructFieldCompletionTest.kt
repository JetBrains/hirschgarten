package org.jetbrains.bazel.languages.starlark.completion

import com.intellij.openapi.application.EDT
import com.intellij.testFramework.common.timeoutRunBlocking
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.junit.jupiter.api.Test

@BazelTestApplication
class StarlarkStructFieldCompletionTest {
  private val myFixture by bazelCodeInsightFixture()

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

  private fun completeBasicLookupStrings(): List<String> = timeoutRunBlocking {
    withContext(Dispatchers.EDT) {
      myFixture.completeBasic().flatMap { it.allLookupStrings }
    }
  }
}
