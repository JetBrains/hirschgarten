package org.jetbrains.bazel.languages.starlark.indentation

import com.intellij.testFramework.runInEdtAndGet
import com.intellij.testFramework.runInEdtAndWait
import io.kotest.matchers.comparables.shouldBeGreaterThan
import org.jetbrains.bazel.languages.starlark.fixtures.StarlarkIndentationTestCase
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.junit.jupiter.api.Test

@BazelTestApplication
class StarlarkLineIndentProviderTest : StarlarkIndentationTestCase() {
  @Test
  fun `should add indent in the empty line after enter between braces`() {
    // given
    myFixture.configureByFile("IndentationTestData.bzl")
    myFixture.type("()")
    moveCaretBack()

    // when
    myFixture.type("\n")

    // then
    caretColumn() shouldBeGreaterThan 0
  }

  @Test
  fun `should add indent in the empty line after enter after opening brace`() {
    // given
    myFixture.configureByFile("IndentationTestData.bzl")
    myFixture.type("(x")
    moveCaretBack()

    // when
    myFixture.type("\n")

    // then
    caretColumn() shouldBeGreaterThan 0
  }

  @Test
  fun `should add indent in the empty line after enter after colon`() {
    // given
    myFixture.configureByFile("IndentationTestData.bzl")
    myFixture.type(":")

    // when
    myFixture.type("\n")

    // then
    caretColumn() shouldBeGreaterThan 0
  }

  private fun moveCaretBack() {
    runInEdtAndWait { myFixture.editor.caretModel.moveToOffset(myFixture.caretOffset - 1) }
  }

  private fun caretColumn(): Int = runInEdtAndGet { myFixture.editor.caretModel.logicalPosition.column }
}
