package org.jetbrains.bazel.languages.starlark.references

import com.intellij.psi.PsiElement
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.instanceOf
import org.jetbrains.bazel.languages.starlark.StarlarkFileType
import org.jetbrains.bazel.languages.starlark.fixtures.StarlarkReferencesTestCase
import org.jetbrains.kotlin.idea.base.psi.getLineNumber
import org.jetbrains.kotlin.idea.base.psi.getLineStartOffset
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
  fun `resolve to nested assignments in module block`() {
    verifyTargetOfReferenceAtCaret(
      """
      if 1 != 2:
          <target>foo = 2
      else:
          foo = 3
      foo = 4
      <caret>foo
      """.trimIndent(),
    )
  }

  @Test
  fun `resolve to nested assignments in function block`() {
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
  fun `resolve to deeply nested assignments in module block`() {
    verifyTargetOfReferenceAtCaret(
      """
      if 1 != 2:
          baz()
          if 3 != 4:
              for i in range(10):
                  <target>foo = 2
          else:
              foo = 3
          foo = 4
      foo = 5
      <caret>foo
      """.trimIndent(),
    )
  }

  @Test
  fun `resolve to deeply nested assignments in function block`() {
    verifyTargetOfReferenceAtCaret(
      """
      foo = 1
      def bar():
          if 1 != 2:
              baz()
              if 3 != 4:
                  for i in range(10):
                      <target>foo = 2
              else:
                  foo = 3
              foo = 4
          foo = 5
          return <caret>foo
      """.trimIndent(),
    )
  }

  @Test
  fun `resolve to later binding in same block`() {
    verifyTargetOfReferenceAtCaret(
      // Taken from:
      // https://github.com/bazelbuild/starlark/blob/6dd78ee3a66820a8b7571239946466cc702b209e/spec.md#name-binding-and-variables
      """
      y = "goodbye"

      def hello():
          for x in (1, 2):
              if x == 2:
                  print(<caret>y) # prints "hello"
              if x == 1:
                  <target>y = "hello"
      """.trimIndent(),
    )
  }

  @Test
  fun `falls back to higher scope`() {
    verifyTargetOfReferenceAtCaret(
      """
      def <target>foo():
          def bar():
              def baz():
                  print(<caret>foo)
      """.trimIndent(),
    )
  }

  private fun verifyTargetOfReferenceAtCaret(text: String) {
    // given
    val expectedLine = text.lineSequence().indexOfFirst { it.contains("<target>") }
    val expectedColumn = text.lineSequence().map { it.indexOf("<target>") }.filter { it != -1 }.first()
    myFixture.configureByText(StarlarkFileType, text.replace("<target>", ""))

    // when
    val reference = myFixture.file.findReferenceAt(myFixture.caretOffset)
    val resolved = reference!!.resolve()

    // then
    resolved.shouldNotBeNull()
    resolved shouldBe instanceOf<PsiElement>()
    val actualLine = myFixture.file.getLineNumber(resolved.textOffset)
    actualLine shouldBe expectedLine
    val actualColumn = resolved.textOffset - myFixture.file.getLineStartOffset(expectedLine, skipWhitespace = false)!!
    actualColumn shouldBe expectedColumn
  }
}
