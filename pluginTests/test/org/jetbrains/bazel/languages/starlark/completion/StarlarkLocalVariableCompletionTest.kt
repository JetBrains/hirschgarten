package org.jetbrains.bazel.languages.starlark.completion

import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.jetbrains.bazel.test.framework.completeBasicLookupStrings
import org.junit.jupiter.api.Test

@BazelTestApplication
class StarlarkLocalVariableCompletionTest {
  private val myFixture by bazelCodeInsightFixture()

  @Test
  fun `should complete each visible binding once`() {
    myFixture.configureByText(
      "defs.bzl",
      """
      top = 1

      def f(param):
          local = 1
          local = 2
          foo = struct()
          foo.attr = 3
          a, (b, c) = 1, (2, 3)
          for k in []:
              pass
          return <caret>
      """.trimIndent(),
    )

    val names = myFixture.completeBasicLookupStrings()
    names shouldContainAll listOf("top", "param", "local", "foo", "a", "b", "c", "k")
    names.count { it == "local" } shouldBe 1
    names shouldNotContain "attr"
  }

  @Test
  fun `should not complete bindings of another function`() {
    myFixture.configureByText(
      "defs.bzl",
      """
      def f():
          hidden = 1

      def g():
          return <caret>
      """.trimIndent(),
    )

    myFixture.completeBasicLookupStrings() shouldNotContain "hidden"
  }
}
