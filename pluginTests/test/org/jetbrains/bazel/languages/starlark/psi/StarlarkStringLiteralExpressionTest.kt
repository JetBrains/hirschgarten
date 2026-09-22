package org.jetbrains.bazel.languages.starlark.psi

import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.application.EDT
import com.intellij.openapi.util.TextRange
import com.intellij.platform.testFramework.junit5.codeInsight.fixture.codeInsightFixture
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.moduleFixture
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource

@TestApplication
class StarlarkStringLiteralExpressionTest {
  private val projectFixture = projectFixture(openAfterCreation = true)
  private val moduleFixture = projectFixture.moduleFixture()
  private val codeInsightFixture by codeInsightFixture(projectFixture, tempPathFixture())

  companion object {
    @JvmStatic
    fun stringDelimiters(): List<Arguments> =
      listOf("", "r", "b", "rb", "br").flatMap { prefix ->
        listOf("'", "\"", "'''", "\"\"\"").map { quote -> Arguments.of(prefix, quote) }
      }
  }

  @ParameterizedTest
  @MethodSource("stringDelimiters")
  fun `content ranges exclude only complete delimiters`(prefix: String, quote: String): Unit = timeoutRunBlocking {
    withContext(Dispatchers.EDT) {
      val opening = prefix + quote
      checkContentRange(opening, opening.length, "")
      checkContentRange(opening + "text", opening.length, "text")
      checkContentRange(opening + quote, opening.length, "")
      checkContentRange(opening + "text" + quote, opening.length, "text")
      checkContentRange(opening + "text\\\\" + quote, opening.length, "text\\\\")
      if (quote.length == 1) {
        checkContentRange(opening + "text\\" + quote, opening.length, "text\\" + quote)
      }
    }
  }

  @ParameterizedTest
  @ValueSource(strings = ["r\"", "r\"\"\""])
  fun `extend selection handles an unfinished raw string`(text: String): Unit = timeoutRunBlocking {
    withContext(Dispatchers.EDT) {
      codeInsightFixture.configureByText("literal.bzl", "value = $text<caret>")
      codeInsightFixture.performEditorAction(IdeActions.ACTION_EDITOR_SELECT_WORD_AT_CARET)
      assertTrue(codeInsightFixture.editor.selectionModel.hasSelection())
    }
  }

  private fun checkContentRange(text: String, startOffset: Int, content: String) {
    val file = codeInsightFixture.configureByText("literal.bzl", "value = $text")
    val literal = PsiTreeUtil.findChildOfType(file, StarlarkStringLiteralExpression::class.java)
      ?: error("No string literal in $text")
    assertEquals(text, literal.text)
    val range = literal.getStringContentsOffset()
    assertEquals(TextRange(startOffset, startOffset + content.length), range, text)
    assertEquals(content, range.substring(literal.text), text)
  }
}
