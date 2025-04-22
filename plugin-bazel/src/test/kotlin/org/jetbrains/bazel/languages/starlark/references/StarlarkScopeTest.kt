package org.jetbrains.bazel.languages.starlark.references

import com.intellij.psi.PsiElement
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.instanceOf
import org.jetbrains.bazel.languages.starlark.StarlarkFileType
import org.jetbrains.bazel.languages.starlark.fixtures.StarlarkReferencesTestCase
import org.jetbrains.kotlin.idea.base.psi.getLineNumber
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StarlarkScopeTest : StarlarkReferencesTestCase() {
  @Test
  fun `function scope is preferred to top-level scope`() {
    verifyTargetOfReferenceAtCaret(
      """
      foo = 1
      def bar():
          <target>foo = 2
          return <caret>foo
      """.trimIndent(),
    )
  }

  @Test
  fun `parameters are preferred to top-level scope`() {
    verifyTargetOfReferenceAtCaret(
      """
      foo = 1
      def bar(<target>foo = 2):
          return <caret>foo
      """.trimIndent(),
    )
  }

  @Test
  fun `default values resolve to outer scope`() {
    verifyTargetOfReferenceAtCaret(
      """
      <target>foo = 1
      def bar(baz = <caret>foo):
          foo = 2
      """.trimIndent(),
    )
  }

  @Test
  fun `resolve to nested assignments`() {
    verifyTargetOfReferenceAtCaret(
      """
      foo = 1
      def bar():
          if 1 != 2:
              <target>foo = 2
          else:
              foo = 3
          foo = 4
          return <caret>foo
      """.trimIndent(),
    )
  }

  @Test
  fun `resolve to deeply nested assignments`() {
    verifyTargetOfReferenceAtCaret(
      """
      foo = 1
      def bar():
          if 1 != 2:
              baz()
              if 3 != 4
                  <target>foo = 2
              else:
                  foo = 3
              foo = 4
          foo = 5
          return <caret>foo
      """.trimIndent(),
    )
  }

  private fun verifyTargetOfReferenceAtCaret(text: String) {
    // given
    val targetLine = text.lineSequence().indexOfFirst { it.contains("<target>") }
    val targetColumn = text.lineSequence().map { it.indexOf("<target>") }.filter { it != -1 }.first()
    myFixture.configureByText(StarlarkFileType, text.replace("<target>", ""))

    // when
    val reference = myFixture.file.findReferenceAt(myFixture.caretOffset)
    val resolved = reference!!.resolve()

    // then
    resolved.shouldNotBeNull()
    resolved shouldBe instanceOf<PsiElement>()
    myFixture.file.getLineNumber(resolved.textOffset) shouldBe targetLine
  }
}
