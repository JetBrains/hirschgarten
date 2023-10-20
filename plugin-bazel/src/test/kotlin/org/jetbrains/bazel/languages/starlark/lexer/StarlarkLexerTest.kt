package org.jetbrains.bazel.languages.starlark.lexer

import org.jetbrains.bazel.languages.starlark.fixtures.StarlarkLexerTestCase
import org.junit.Test

private const val TRIPLE_QUOTE = "\"\"\""

class StarlarkLexerTest : StarlarkLexerTestCase() {
  @Test
  fun `should lex simple expression`() {
    // given
    val code = "x=0"

    // when & then
    code shouldLexTo listOf(
      "Starlark:IDENTIFIER", "Starlark:=", "Starlark:INT", "Starlark:STATEMENT_BREAK",
    )
  }

  @Test
  fun `should lex and merge spaces`() {
    // given
    val code = "x  =   0"

    // when & then
    code shouldLexTo listOf(
      "Starlark:IDENTIFIER",
      "Starlark:SPACE",
      "Starlark:=",
      "Starlark:SPACE",
      "Starlark:INT",
      "Starlark:STATEMENT_BREAK",
    )
  }

  @Test
  fun `should lex line break in parentheses`() {
    // given
    val code =
      """
        |(a,
        |b)
      """.trimMargin()

    // when & then
    code shouldLexTo listOf(
      "Starlark:(",
      "Starlark:IDENTIFIER",
      "Starlark:,",
      "Starlark:LINE_BREAK",
      "Starlark:IDENTIFIER",
      "Starlark:)",
      "Starlark:STATEMENT_BREAK",
    )
  }

  @Test
  fun `should lex line break in brackets`() {
    // given
    val code =
      """
        |[a,
        |b]
      """.trimMargin()

    // when & then
    code shouldLexTo listOf(
      "Starlark:[",
      "Starlark:IDENTIFIER",
      "Starlark:,",
      "Starlark:LINE_BREAK",
      "Starlark:IDENTIFIER",
      "Starlark:]",
      "Starlark:STATEMENT_BREAK",
    )
  }

  @Test
  fun `should lex line break in braces`() {
    val code =
      // given
      """
        |{a,
        |b}
      """.trimMargin()

    // when & then
    code shouldLexTo listOf(
      "Starlark:{",
      "Starlark:IDENTIFIER",
      "Starlark:,",
      "Starlark:LINE_BREAK",
      "Starlark:IDENTIFIER",
      "Starlark:}",
      "Starlark:STATEMENT_BREAK",
    )
  }

  @Test
  fun `should lex line break in braces after assignment`() {
    // given
    val code =
      """
        |x={a,
        |b}
      """.trimMargin()

    // when & then
    code shouldLexTo listOf(
      "Starlark:IDENTIFIER",
      "Starlark:=",
      "Starlark:{",
      "Starlark:IDENTIFIER",
      "Starlark:,",
      "Starlark:LINE_BREAK",
      "Starlark:IDENTIFIER",
      "Starlark:}",
      "Starlark:STATEMENT_BREAK",
    )
  }

  @Test
  fun `should lex line break in braces after comment`() {
    // given
    val code =
      """
        |x={a, #com
        |b}
      """.trimMargin()

    // when & then
    code shouldLexTo listOf(
      "Starlark:IDENTIFIER",
      "Starlark:=",
      "Starlark:{",
      "Starlark:IDENTIFIER",
      "Starlark:,",
      "Starlark:SPACE",
      "Starlark:COMMENT",
      "Starlark:LINE_BREAK",
      "Starlark:IDENTIFIER",
      "Starlark:}",
      "Starlark:STATEMENT_BREAK",
    )
  }

  @Test
  fun `should lex brace after indent`() {
    // given
    val code =
      """
        |x=
        |  {a, #comment
        |  b}
      """.trimMargin()

    // when & then
    code shouldLexTo listOf(
      "Starlark:IDENTIFIER",
      "Starlark:=",
      "Starlark:STATEMENT_BREAK",
      "Starlark:LINE_BREAK",
      "Starlark:INDENT",
      "Starlark:{",
      "Starlark:IDENTIFIER",
      "Starlark:,",
      "Starlark:SPACE",
      "Starlark:COMMENT",
      "Starlark:LINE_BREAK",
      "Starlark:IDENTIFIER",
      "Starlark:}",
      "Starlark:STATEMENT_BREAK",
    )
  }

  @Test
  fun `should lex indent`() {
    // given
    val code =
      """
        |if a:
        |  b
      """.trimMargin()

    // when & then
    code shouldLexTo listOf(
      "Starlark:if",
      "Starlark:SPACE",
      "Starlark:IDENTIFIER",
      "Starlark::",
      "Starlark:STATEMENT_BREAK",
      "Starlark:LINE_BREAK",
      "Starlark:INDENT",
      "Starlark:IDENTIFIER",
      "Starlark:STATEMENT_BREAK",
    )
  }

  @Test
  fun `should lex multiline indent`() {
    // given
    val code =
      """
        |if a:
        |  b
        |  c
      """.trimMargin()

    // when & then
    code shouldLexTo listOf(
      "Starlark:if",
      "Starlark:SPACE",
      "Starlark:IDENTIFIER",
      "Starlark::",
      "Starlark:STATEMENT_BREAK",
      "Starlark:LINE_BREAK",
      "Starlark:INDENT",
      "Starlark:IDENTIFIER",
      "Starlark:STATEMENT_BREAK",
      "Starlark:LINE_BREAK",
      "Starlark:IDENTIFIER",
      "Starlark:STATEMENT_BREAK",
    )
  }

  @Test
  fun `should lex dedent`() {
    // given
    val code =
      """
        |if a:
        |  b
        |c
      """.trimMargin()

    // when & then
    code shouldLexTo listOf(
      "Starlark:if",
      "Starlark:SPACE",
      "Starlark:IDENTIFIER",
      "Starlark::",
      "Starlark:STATEMENT_BREAK",
      "Starlark:LINE_BREAK",
      "Starlark:INDENT",
      "Starlark:IDENTIFIER",
      "Starlark:STATEMENT_BREAK",
      "Starlark:DEDENT",
      "Starlark:LINE_BREAK",
      "Starlark:IDENTIFIER",
      "Starlark:STATEMENT_BREAK",
    )
  }

  @Test
  fun `should lex multi dedent`() {
    // given
    val code =
      """
        |if a:
        |  b
        |    c
        |d
      """.trimMargin()

    // when & then
    code shouldLexTo listOf(
      "Starlark:if",
      "Starlark:SPACE",
      "Starlark:IDENTIFIER",
      "Starlark::",
      "Starlark:STATEMENT_BREAK",
      "Starlark:LINE_BREAK",
      "Starlark:INDENT",
      "Starlark:IDENTIFIER",
      "Starlark:STATEMENT_BREAK",
      "Starlark:LINE_BREAK",
      "Starlark:INDENT",
      "Starlark:IDENTIFIER",
      "Starlark:STATEMENT_BREAK",
      "Starlark:DEDENT",
      "Starlark:DEDENT",
      "Starlark:LINE_BREAK",
      "Starlark:IDENTIFIER",
      "Starlark:STATEMENT_BREAK",
    )
  }

  @Test
  fun `should lex multi cascade dedent`() {
    // given
    val code =
      """
        |if a:
        |  b
        |    c
        |  d
        |e
      """.trimMargin()

    // when & then
    code shouldLexTo listOf(
      "Starlark:if",
      "Starlark:SPACE",
      "Starlark:IDENTIFIER",
      "Starlark::",
      "Starlark:STATEMENT_BREAK",
      "Starlark:LINE_BREAK",
      "Starlark:INDENT",
      "Starlark:IDENTIFIER",
      "Starlark:STATEMENT_BREAK",
      "Starlark:LINE_BREAK",
      "Starlark:INDENT",
      "Starlark:IDENTIFIER",
      "Starlark:STATEMENT_BREAK",
      "Starlark:DEDENT",
      "Starlark:LINE_BREAK",
      "Starlark:IDENTIFIER",
      "Starlark:STATEMENT_BREAK",
      "Starlark:DEDENT",
      "Starlark:LINE_BREAK",
      "Starlark:IDENTIFIER",
      "Starlark:STATEMENT_BREAK",
    )
  }

  @Test
  fun `should lex empty line`() {
    // given
    val code =
      """
        |if a:
        |  b
        |
        |  c
      """.trimMargin()

    // when & then
    code shouldLexTo listOf(
      "Starlark:if",
      "Starlark:SPACE",
      "Starlark:IDENTIFIER",
      "Starlark::",
      "Starlark:STATEMENT_BREAK",
      "Starlark:LINE_BREAK",
      "Starlark:INDENT",
      "Starlark:IDENTIFIER",
      "Starlark:STATEMENT_BREAK",
      "Starlark:LINE_BREAK",
      "Starlark:IDENTIFIER",
      "Starlark:STATEMENT_BREAK",
    )
  }

  @Test
  fun `should lex end of line space`() {
    // given
    val code =
      """
        |if a:
        |  b             
        |  c
      """.trimMargin()

    // when & then
    code shouldLexTo listOf(
      "Starlark:if",
      "Starlark:SPACE",
      "Starlark:IDENTIFIER",
      "Starlark::",
      "Starlark:STATEMENT_BREAK",
      "Starlark:LINE_BREAK",
      "Starlark:INDENT",
      "Starlark:IDENTIFIER",
      "Starlark:STATEMENT_BREAK",
      "Starlark:LINE_BREAK",
      "Starlark:IDENTIFIER",
      "Starlark:STATEMENT_BREAK",
    )
  }

  @Test
  fun `should lex comment`() {
    // given
    val code =
      """
        |if a:
        |  b
        |  #comment
        |c
      """.trimMargin()

    // when & then
    code shouldLexTo listOf(
      "Starlark:if",
      "Starlark:SPACE",
      "Starlark:IDENTIFIER",
      "Starlark::",
      "Starlark:STATEMENT_BREAK",
      "Starlark:LINE_BREAK",
      "Starlark:INDENT",
      "Starlark:IDENTIFIER",
      "Starlark:STATEMENT_BREAK",
      "Starlark:LINE_BREAK",
      "Starlark:COMMENT",
      "Starlark:DEDENT",
      "Starlark:LINE_BREAK",
      "Starlark:IDENTIFIER",
      "Starlark:STATEMENT_BREAK",
    )
  }

  @Test
  fun `should lex indented comment`() {
    // given
    val code =
      """
        |#comment1
        |  #comment2
        |#comment3
      """.trimMargin()

    // when & then
    code shouldLexTo listOf(
      "Starlark:COMMENT",
      "Starlark:LINE_BREAK",
      "Starlark:COMMENT",
      "Starlark:LINE_BREAK",
      "Starlark:COMMENT",
      "Starlark:STATEMENT_BREAK",
    )
  }

  @Test
  fun `should lex indented comment and code`() {
    // given
    val code =
      """
        |if a:
        |  b
        |  #comment
        |  c
      """.trimMargin()

    // when & then
    code shouldLexTo listOf(
      "Starlark:if",
      "Starlark:SPACE",
      "Starlark:IDENTIFIER",
      "Starlark::",
      "Starlark:STATEMENT_BREAK",
      "Starlark:LINE_BREAK",
      "Starlark:INDENT",
      "Starlark:IDENTIFIER",
      "Starlark:STATEMENT_BREAK",
      "Starlark:LINE_BREAK",
      "Starlark:COMMENT",
      "Starlark:LINE_BREAK",
      "Starlark:IDENTIFIER",
      "Starlark:STATEMENT_BREAK",
    )
  }

  @Test
  fun `should lex bytes literal`() {
    // given
    val code = "x=b'X'"

    // when & then
    code shouldLexTo listOf(
      "Starlark:IDENTIFIER", "Starlark:=", "Starlark:BYTES", "Starlark:STATEMENT_BREAK",
    )
  }

  @Test
  fun `should lex zero literal`() {
    // given
    val code = "x=0"

    // when & then
    code shouldLexTo listOf(
      "Starlark:IDENTIFIER", "Starlark:=", "Starlark:INT", "Starlark:STATEMENT_BREAK",
    )
  }

  @Test
  fun `should lex integer literal`() {
    // given
    val code = "x=42"

    // when & then
    code shouldLexTo listOf(
      "Starlark:IDENTIFIER", "Starlark:=", "Starlark:INT", "Starlark:STATEMENT_BREAK",
    )
  }

  @Test
  fun `should lex hexadecimal literal`() {
    // given
    val code = "x=0x859"

    // when & then
    code shouldLexTo listOf(
      "Starlark:IDENTIFIER", "Starlark:=", "Starlark:INT", "Starlark:STATEMENT_BREAK",
    )
  }

  @Test
  fun `should lex octal literal`() {
    // given
    val code = "x=0o4131"

    // when & then
    code shouldLexTo listOf(
      "Starlark:IDENTIFIER", "Starlark:=", "Starlark:INT", "Starlark:STATEMENT_BREAK",
    )
  }

  @Test
  fun `should lex triple string`() {
    // given
    val code =
      """
        |$TRIPLE_QUOTE$TRIPLE_QUOTE
        |x=$TRIPLE_QUOTE$TRIPLE_QUOTE
      """.trimMargin()

    // when & then
    code shouldLexTo listOf(
      "Starlark:STRING",
      "Starlark:STATEMENT_BREAK",
      "Starlark:LINE_BREAK",
      "Starlark:IDENTIFIER",
      "Starlark:=",
      "Starlark:STRING",
      "Starlark:STATEMENT_BREAK",
    )
  }

  @Test
  fun `should lex triple string escaped`() {
    // given
    val code =
      """
        |$TRIPLE_QUOTE\$TRIPLE_QUOTE X \$TRIPLE_QUOTE $TRIPLE_QUOTE;
      """.trimMargin()

    // when & then
    code shouldLexTo listOf(
      "Starlark:STRING", "Starlark:;", "Starlark:STATEMENT_BREAK",
    )
  }

  @Test
  fun `should lex triple string with line break`() {
    // given
    val code =
      """
        |$TRIPLE_QUOTE
        |\nX
        |
        |$TRIPLE_QUOTE;
      """.trimMargin()

    // when & then
    code shouldLexTo listOf(
      "Starlark:STRING", "Starlark:;", "Starlark:STATEMENT_BREAK",
    )
  }

  @Test
  fun `should lex escaped closing triple apostrophes`() {
    // given
    val code =
      """
        |x=''' X '\''' X '''
      """.trimMargin()

    // when & then
    code shouldLexTo listOf(
      "Starlark:IDENTIFIER", "Starlark:=", "Starlark:STRING", "Starlark:STATEMENT_BREAK",
    )
  }

  @Test
  fun `should lex escaped closing triple quotes`() {
    // given
    val code =
      """
        |x=$TRIPLE_QUOTE X "\$TRIPLE_QUOTE X $TRIPLE_QUOTE
      """.trimMargin()

    // when & then
    code shouldLexTo listOf(
      "Starlark:IDENTIFIER", "Starlark:=", "Starlark:STRING", "Starlark:STATEMENT_BREAK",
    )
  }

  @Test
  fun `should lex adjacent strings`() {
    // given
    val code =
      """
        |$TRIPLE_QUOTE X $TRIPLE_QUOTE""
      """.trimMargin()

    // when & then
    code shouldLexTo listOf(
      "Starlark:STRING", "Starlark:STRING", "Starlark:STATEMENT_BREAK",
    )
  }

  @Test
  fun `should lex dedent before comment`() {
    // given
    val code =
      """
        |def bar():
        |   pass
        |   
        |#comment
        |def foo():
        |   pass
      """.trimMargin()

    // when & then
    code shouldLexTo listOf(
      "Starlark:def",
      "Starlark:SPACE",
      "Starlark:IDENTIFIER",
      "Starlark:(",
      "Starlark:)",
      "Starlark::",
      "Starlark:STATEMENT_BREAK",
      "Starlark:LINE_BREAK",
      "Starlark:INDENT",
      "Starlark:pass",
      "Starlark:STATEMENT_BREAK",
      "Starlark:DEDENT",
      "Starlark:LINE_BREAK",
      "Starlark:COMMENT",
      "Starlark:LINE_BREAK",
      "Starlark:def",
      "Starlark:SPACE",
      "Starlark:IDENTIFIER",
      "Starlark:(",
      "Starlark:)",
      "Starlark::",
      "Starlark:STATEMENT_BREAK",
      "Starlark:LINE_BREAK",
      "Starlark:INDENT",
      "Starlark:pass",
      "Starlark:STATEMENT_BREAK",
    )
  }

  @Test
  fun `should lex dedent after comment`() {
    // given
    val code =
      """
        |def foo():
        |   pass
        |   #comment
        |   
      """.trimMargin()

    // when & then
    code shouldLexTo listOf(
      "Starlark:def",
      "Starlark:SPACE",
      "Starlark:IDENTIFIER",
      "Starlark:(",
      "Starlark:)",
      "Starlark::",
      "Starlark:STATEMENT_BREAK",
      "Starlark:LINE_BREAK",
      "Starlark:INDENT",
      "Starlark:pass",
      "Starlark:STATEMENT_BREAK",
      "Starlark:LINE_BREAK",
      "Starlark:COMMENT",
      "Starlark:DEDENT",
      "Starlark:LINE_BREAK",
      "Starlark:STATEMENT_BREAK",
    )
  }

  @Test
  fun `should lex indent at start of file`() {
    // given
    val code = "   x"

    // when & then
    code shouldLexTo listOf(
      "Starlark:SPACE", "Starlark:INDENT", "Starlark:IDENTIFIER", "Starlark:STATEMENT_BREAK",
    )
  }

  private infix fun String.shouldLexTo(expectedTokens: List<String>) {
    doLexerTest(this, StarlarkIndentingLexer(), expectedTokens)
  }
}
