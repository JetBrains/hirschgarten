package org.jetbrains.bazel.languages.starlark.injection

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.util.Computable
import com.intellij.psi.ElementManipulators
import com.intellij.psi.util.PsiTreeUtil
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.languages.starlark.fixtures.StarlarkPsiTestCase
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression
import org.jetbrains.bazel.languages.starlark.utils.StarlarkQuote
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Starlark literals are written as Kotlin raw strings, which have no escape sequences of their own, so that they read
 * exactly as they would in a BUILD file. Since `"""` cannot appear in a Kotlin raw string, Starlark triple-quoted
 * strings use `'''`.
 */
@RunWith(JUnit4::class)
class StarlarkStringLiteralEscaperTest : StarlarkPsiTestCase() {
  @Test
  fun `should compute contents of differently quoted strings`() {
    stringLiteral(""""abc"""").getStringContents() shouldBe "abc"
    stringLiteral("""'abc'""").getStringContents() shouldBe "abc"
    stringLiteral("\"\"\"abc\"\"\"").getStringContents() shouldBe "abc"
    stringLiteral("""'''abc'''""").getStringContents() shouldBe "abc"
    stringLiteral("'''\nabc\n'''").getStringContents() shouldBe "\nabc\n"
    stringLiteral("""''""").getStringContents() shouldBe ""
    stringLiteral("""''''''""").getStringContents() shouldBe ""
    stringLiteral("""r"a\b"""").getStringContents() shouldBe """a\b"""
    stringLiteral("""b'abc'""").getStringContents() shouldBe "abc"
    stringLiteral("""rb'abc'""").getStringContents() shouldBe "abc"
    stringLiteral(""""abc""").getStringContents() shouldBe "abc"
    stringLiteral("""'''abc""").getStringContents() shouldBe "abc"
  }

  @Test
  fun `should detect quotes and raw strings`() {
    stringLiteral(""""abc"""").getQuote() shouldBe StarlarkQuote.DOUBLE
    stringLiteral("""'abc'""").getQuote() shouldBe StarlarkQuote.SINGLE
    stringLiteral("\"\"\"abc\"\"\"").getQuote() shouldBe StarlarkQuote.TRIPLE_DOUBLE
    stringLiteral("""'''abc'''""").getQuote() shouldBe StarlarkQuote.TRIPLE_SINGLE
    stringLiteral("""r"abc"""").getQuote() shouldBe StarlarkQuote.DOUBLE
    stringLiteral("""r'''abc'''""").getQuote() shouldBe StarlarkQuote.TRIPLE_SINGLE
    stringLiteral(""""abc"""").isRaw() shouldBe false
    stringLiteral("""r"abc"""").isRaw() shouldBe true
    stringLiteral("""rb"abc"""").isRaw() shouldBe true
    stringLiteral("""b"abc"""").isRaw() shouldBe false
  }

  @Test
  fun `should decode escape sequences`() {
    decode(""""a\nb"""") shouldBe "a\nb"
    decode(""""a\tb\\c\"d\'e"""") shouldBe "a\tb\\c\"d'e"
    decode(""""\x41\101\u0042\U00000043"""") shouldBe "AABC"
    decode(""""\U0001F600"""") shouldBe "\uD83D\uDE00"
    decode(""""\0\7a"""") shouldBe "\u0000\u0007a"
    decode(""""\d\xZZ"""") shouldBe """\d\xZZ"""
  }

  @Test
  fun `should not decode raw strings`() {
    decode("""r"a\nb\\c"""") shouldBe """a\nb\\c"""
  }

  @Test
  fun `should drop line continuations in triple quoted strings`() {
    decode(
      """
      '''echo a && \
        echo b'''
      """.trimIndent(),
    ) shouldBe "echo a &&   echo b"
  }

  @Test
  fun `should map decoded offsets back to the host`() {
    val literal = stringLiteral(""""a\nb\x41c"""")
    val escaper = literal.createLiteralTextEscaper()
    val range = literal.getStringContentsOffset()
    val decoded = StringBuilder()
    escaper.decode(range, decoded) shouldBe true
    decoded.toString() shouldBe "a\nbAc"
    // decoded: a | \n | b | A | c
    // source:  a | \n | b | \x41 | c   (offset by the opening quote)
    listOf(0, 1, 2, 3, 4, 5).map { escaper.getOffsetInHost(it, range) } shouldBe listOf(1, 2, 4, 5, 9, 10)
    escaper.getOffsetInHost(6, range) shouldBe -1
    escaper.isOneLine() shouldBe true
    stringLiteral("""'''a'''""").createLiteralTextEscaper().isOneLine() shouldBe false
  }

  @Test
  fun `should escape new content`() {
    replaceContents(""""abc"""", "a\"b\\c\nd") shouldBe """"a\"b\\c\nd""""
    replaceContents("""'abc'""", "a'b\"c") shouldBe """'a\'b"c'"""
    replaceContents("""'''abc'''""", "a'b\nc") shouldBe "'''a'b\nc'''"
    replaceContents("""'''abc'''""", "a''b") shouldBe """'''a''b'''"""
    replaceContents("""'''abc'''""", "a'''b") shouldBe """'''a\'''b'''"""
    replaceContents("""'''abc'''""", "ab'") shouldBe """'''ab\''''"""
  }

  @Test
  fun `should keep raw strings raw if possible and convert them to regular strings otherwise`() {
    replaceContents("""r"abc"""", """a\nb""") shouldBe """r"a\nb""""
    replaceContents("""r"a\b"""", """a\b\c""") shouldBe """r"a\b\c""""
    replaceContents("""r"a\b"""", """a\"b""") shouldBe """r"a\"b""""
    replaceContents("""r'''a\b'''""", """a'b''\c""") shouldBe """r'''a'b''\c'''"""
    replaceContents("""r"a\b"""", """a"b""") shouldBe """"a\"b""""
    replaceContents("""r"a\b"""", """a\b"c""") shouldBe """"a\\b\"c""""
    replaceContents("""r'''a\b'''""", "a'''b") shouldBe """'''a\'''b'''"""
    replaceContents("""r'''a\b'''""", "ab'") shouldBe """'''ab\''''"""
    replaceContents("""r"ab"""", """ab\""") shouldBe """"ab\\""""
    replaceContents("""r"ab"""", "a\nb") shouldBe """"a\nb""""
    replaceContents("""rb"ab"""", """a"b""") shouldBe """b"a\"b""""
  }

  private fun decode(literal: String): String {
    val stringLiteral = stringLiteral(literal)
    val decoded = StringBuilder()
    stringLiteral.createLiteralTextEscaper().decode(stringLiteral.getStringContentsOffset(), decoded) shouldBe true
    return decoded.toString()
  }

  private fun replaceContents(literal: String, newContent: String): String {
    val stringLiteral = stringLiteral(literal)
    return WriteCommandAction.runWriteCommandAction(
      project,
      Computable { ElementManipulators.handleContentChange(stringLiteral, newContent).text },
    )
  }

  private fun stringLiteral(literal: String): StarlarkStringLiteralExpression {
    myFixture.configureByText("BUILD", "x = $literal\n")
    val stringLiteral = PsiTreeUtil.findChildOfType(myFixture.file, StarlarkStringLiteralExpression::class.java)
    checkNotNull(stringLiteral) { "No string literal found in $literal" }
    stringLiteral.text shouldBe literal
    return stringLiteral
  }
}
