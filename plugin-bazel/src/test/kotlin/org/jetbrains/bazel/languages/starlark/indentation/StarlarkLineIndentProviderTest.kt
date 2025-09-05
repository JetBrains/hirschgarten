package org.jetbrains.bazel.languages.starlark.indentation

import io.kotest.matchers.comparables.shouldBeGreaterThan
import org.jetbrains.bazel.languages.starlark.fixtures.StarlarkIndentationTestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StarlarkLineIndentProviderTest : StarlarkIndentationTestCase() {
  @Test
  fun `should add indent in the empty line after enter between braces`() {
    // given
    myFixture.configureByFile("IndentationTestData.bzl")
    myFixture.type("()")
    myFixture.editor.caretModel.moveToOffset(myFixture.caretOffset - 1)

    // when
    myFixture.type("\n")

    // then
    myFixture.editor.caretModel.logicalPosition.column shouldBeGreaterThan 0
  }

  @Test
  fun `should add indent in the empty line after enter after opening brace`() {
    // given
    myFixture.configureByFile("IndentationTestData.bzl")
    myFixture.type("(x")
    myFixture.editor.caretModel.moveToOffset(myFixture.caretOffset - 1)

    // when
    myFixture.type("\n")

    // then
    myFixture.editor.caretModel.logicalPosition.column shouldBeGreaterThan 0
  }

  @Test
  fun `should add indent in the empty line after enter after colon`() {
    // given
    myFixture.configureByFile("IndentationTestData.bzl")
    myFixture.type(":")

    // when
    myFixture.type("\n")

    // then
    myFixture.editor.caretModel.logicalPosition.column shouldBeGreaterThan 0
  }
}
