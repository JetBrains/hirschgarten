package org.jetbrains.bazel.languages.starlark.psi

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.util.PsiTreeUtil
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.languages.starlark.fixtures.StarlarkPsiTestCase
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkListLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkExpressionStatement
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StarlarkListLiteralTest : StarlarkPsiTestCase() {
  @Test
  fun `should append string at the end of list`() {
    // given
    myFixture.configureByFile("ListLiteral.bzl")
    // when & then
    testListAppending()
  }

  @Test
  fun `should append string at the end of list ended with comma`() {
    // given
    myFixture.configureByFile("ListLiteralComma.bzl")
    // when & then
    testListAppending()
  }

  @Test
  fun `should append string at the end of list ended with comma and whitespace`() {
    // given
    myFixture.configureByFile("ListLiteralCommaWhitespace.bzl")
    // when & then
    testListAppending()
  }

  @Test
  fun `should append string at the end of empty list`() {
    // given
    myFixture.configureByFile("ListLiteralEmpty.bzl")
    // when & then
    testListAppending()
  }

  @Test
  fun `should append string at the end of list ended with whitespace`() {
    // given
    myFixture.configureByFile("ListLiteralWhitespace.bzl")
    // when & then
    testListAppending()
  }

  @Test
  fun `should get list elements`() {
    // given
    myFixture.configureByFile("ListLiteral.bzl")
    val list = getLists().last()
    val expected = listOf("\"first\"", "\"second\"")
    // when
    val elements = list.getElements().map { it.text }
    // then
    elements shouldBe expected
  }

  private fun testListAppending() {
    val lists = getLists()
    val initial = lists.first()
    val expected = lists.last()

    WriteCommandAction.runWriteCommandAction(project) {
      initial.appendString("second")
    }

    initial.text shouldBe expected.text
  }

  private fun getLists() =
    myFixture.file.children
      .filterIsInstance<StarlarkExpressionStatement>()
      .mapNotNull {
        PsiTreeUtil.getChildOfType(it, StarlarkListLiteralExpression::class.java)
      }
}
