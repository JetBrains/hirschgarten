package org.jetbrains.bazel.languages.starlark.lexer

import org.jetbrains.bazel.languages.fixtures.LexerTestCase
import org.junit.jupiter.api.Test

private const val TRIPLE_QUOTE = "\"\"\""

class StarlarkLexerTest : LexerTestCase() {
  @Test
  fun `should lex simple expression`() {
    // given
    val code = "x=0"

    // when & then
    code shouldLexTo
      listOf(
        "identifier",
        "=",
        "integer",
        "end of statement",
      )
  }

  @Test
  fun `should lex and merge spaces`() {
    // given
    val code = "x  =   0"

    // when & then
    code shouldLexTo
      listOf(
        "identifier",
        "SPACE",
        "=",
        "SPACE",
        "integer",
        "end of statement",
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
    code shouldLexTo
      listOf(
        "(",
        "identifier",
        ",",
        "LINE_BREAK",
        "identifier",
        ")",
        "end of statement",
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
    code shouldLexTo
      listOf(
        "[",
        "identifier",
        ",",
        "LINE_BREAK",
        "identifier",
        "]",
        "end of statement",
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
    code shouldLexTo
      listOf(
        "{",
        "identifier",
        ",",
        "LINE_BREAK",
        "identifier",
        "}",
        "end of statement",
      )
  }

  @Test
  fun `should lex line break after extra closing bracket`() {
    // given
    val code =
      """
        |a)
        |b
      """.trimMargin()

    // when & then
    code shouldLexTo
      listOf(
        "identifier",
        ")",
        "end of statement",
        "LINE_BREAK",
        "identifier",
        "end of statement",
      )
  }

  @Test
  fun `should lex continuation lines at the indent of the opener line`() {
    // given
    val code =
      """
        |x = [a
        |.b
        |(c)
        |[0]
        |if d else e
        |for e in f]
      """.trimMargin()

    // when & then
    code shouldLexTo
      listOf(
        "identifier",
        "SPACE",
        "=",
        "SPACE",
        "[",
        "identifier",
        "LINE_BREAK",
        ".",
        "identifier",
        "LINE_BREAK",
        "(",
        "identifier",
        ")",
        "LINE_BREAK",
        "[",
        "integer",
        "]",
        "LINE_BREAK",
        "if",
        "SPACE",
        "identifier",
        "SPACE",
        "else",
        "SPACE",
        "identifier",
        "LINE_BREAK",
        "for",
        "SPACE",
        "identifier",
        "SPACE",
        "in",
        "SPACE",
        "identifier",
        "]",
        "end of statement",
      )
  }

  @Test
  fun `should lex line break between strings in brackets at opener line indent`() {
    // given
    val code =
      """
        |x = (
        |"a"
        |"b"
        |)
      """.trimMargin()

    // when & then
    code shouldLexTo
      listOf(
        "identifier",
        "SPACE",
        "=",
        "SPACE",
        "(",
        "LINE_BREAK",
        "string",
        "LINE_BREAK",
        "string",
        "LINE_BREAK",
        ")",
        "end of statement",
      )
  }

  @Test
  fun `should lex statement break before statement keyword in brackets`() {
    // given
    val code =
      """
        |f(a,
        |    return
      """.trimMargin()

    // when & then
    code shouldLexTo
      listOf(
        "identifier",
        "(",
        "identifier",
        ",",
        "end of statement",
        "LINE_BREAK",
        "indent",
        "return",
        "end of statement",
        "dedent",
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
    code shouldLexTo
      listOf(
        "identifier",
        "=",
        "{",
        "identifier",
        ",",
        "LINE_BREAK",
        "identifier",
        "}",
        "end of statement",
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
    code shouldLexTo
      listOf(
        "identifier",
        "=",
        "{",
        "identifier",
        ",",
        "SPACE",
        "comment",
        "LINE_BREAK",
        "identifier",
        "}",
        "end of statement",
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
    code shouldLexTo
      listOf(
        "identifier",
        "=",
        "end of statement",
        "LINE_BREAK",
        "indent",
        "{",
        "identifier",
        ",",
        "SPACE",
        "comment",
        "LINE_BREAK",
        "identifier",
        "}",
        "end of statement",
        "dedent",
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
    code shouldLexTo
      listOf(
        "if",
        "SPACE",
        "identifier",
        ":",
        "end of statement",
        "LINE_BREAK",
        "indent",
        "identifier",
        "end of statement",
        "dedent",
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
    code shouldLexTo
      listOf(
        "if",
        "SPACE",
        "identifier",
        ":",
        "end of statement",
        "LINE_BREAK",
        "indent",
        "identifier",
        "end of statement",
        "LINE_BREAK",
        "identifier",
        "end of statement",
        "dedent",
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
    code shouldLexTo
      listOf(
        "if",
        "SPACE",
        "identifier",
        ":",
        "end of statement",
        "LINE_BREAK",
        "indent",
        "identifier",
        "end of statement",
        "dedent",
        "LINE_BREAK",
        "identifier",
        "end of statement",
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
    code shouldLexTo
      listOf(
        "if",
        "SPACE",
        "identifier",
        ":",
        "end of statement",
        "LINE_BREAK",
        "indent",
        "identifier",
        "end of statement",
        "LINE_BREAK",
        "indent",
        "identifier",
        "end of statement",
        "dedent",
        "dedent",
        "LINE_BREAK",
        "identifier",
        "end of statement",
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
    code shouldLexTo
      listOf(
        "if",
        "SPACE",
        "identifier",
        ":",
        "end of statement",
        "LINE_BREAK",
        "indent",
        "identifier",
        "end of statement",
        "LINE_BREAK",
        "indent",
        "identifier",
        "end of statement",
        "dedent",
        "LINE_BREAK",
        "identifier",
        "end of statement",
        "dedent",
        "LINE_BREAK",
        "identifier",
        "end of statement",
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
    code shouldLexTo
      listOf(
        "if",
        "SPACE",
        "identifier",
        ":",
        "end of statement",
        "LINE_BREAK",
        "indent",
        "identifier",
        "end of statement",
        "LINE_BREAK",
        "identifier",
        "end of statement",
        "dedent",
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
    code shouldLexTo
      listOf(
        "if",
        "SPACE",
        "identifier",
        ":",
        "end of statement",
        "LINE_BREAK",
        "indent",
        "identifier",
        "end of statement",
        "LINE_BREAK",
        "identifier",
        "end of statement",
        "dedent",
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
    code shouldLexTo
      listOf(
        "if",
        "SPACE",
        "identifier",
        ":",
        "end of statement",
        "LINE_BREAK",
        "indent",
        "identifier",
        "end of statement",
        "LINE_BREAK",
        "comment",
        "dedent",
        "LINE_BREAK",
        "identifier",
        "end of statement",
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
    code shouldLexTo
      listOf(
        "comment",
        "LINE_BREAK",
        "comment",
        "LINE_BREAK",
        "comment",
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
    code shouldLexTo
      listOf(
        "if",
        "SPACE",
        "identifier",
        ":",
        "end of statement",
        "LINE_BREAK",
        "indent",
        "identifier",
        "end of statement",
        "LINE_BREAK",
        "comment",
        "LINE_BREAK",
        "identifier",
        "end of statement",
        "dedent",
      )
  }

  @Test
  fun `should lex bytes literal`() {
    // given
    val code = "x=b'X'"

    // when & then
    code shouldLexTo
      listOf(
        "identifier",
        "=",
        "bytes",
        "end of statement",
      )
  }

  @Test
  fun `should lex zero literal`() {
    // given
    val code = "x=0"

    // when & then
    code shouldLexTo
      listOf(
        "identifier",
        "=",
        "integer",
        "end of statement",
      )
  }

  @Test
  fun `should lex integer literal`() {
    // given
    val code = "x=42"

    // when & then
    code shouldLexTo
      listOf(
        "identifier",
        "=",
        "integer",
        "end of statement",
      )
  }

  @Test
  fun `should lex hexadecimal literal`() {
    // given
    val code = "x=0x859"

    // when & then
    code shouldLexTo
      listOf(
        "identifier",
        "=",
        "integer",
        "end of statement",
      )
  }

  @Test
  fun `should lex octal literal`() {
    // given
    val code = "x=0o4131"

    // when & then
    code shouldLexTo
      listOf(
        "identifier",
        "=",
        "integer",
        "end of statement",
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
    code shouldLexTo
      listOf(
        "string",
        "end of statement",
        "LINE_BREAK",
        "identifier",
        "=",
        "string",
        "end of statement",
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
    code shouldLexTo
      listOf(
        "string",
        ";",
        "end of statement",
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
    code shouldLexTo
      listOf(
        "string",
        ";",
        "end of statement",
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
    code shouldLexTo
      listOf(
        "identifier",
        "=",
        "string",
        "end of statement",
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
    code shouldLexTo
      listOf(
        "identifier",
        "=",
        "string",
        "end of statement",
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
    code shouldLexTo
      listOf(
        "string",
        "string",
        "end of statement",
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
    code shouldLexTo
      listOf(
        "def",
        "SPACE",
        "identifier",
        "(",
        ")",
        ":",
        "end of statement",
        "LINE_BREAK",
        "indent",
        "pass",
        "end of statement",
        "dedent",
        "LINE_BREAK",
        "comment",
        "LINE_BREAK",
        "def",
        "SPACE",
        "identifier",
        "(",
        ")",
        ":",
        "end of statement",
        "LINE_BREAK",
        "indent",
        "pass",
        "end of statement",
        "dedent",
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
    code shouldLexTo
      listOf(
        "def",
        "SPACE",
        "identifier",
        "(",
        ")",
        ":",
        "end of statement",
        "LINE_BREAK",
        "indent",
        "pass",
        "end of statement",
        "LINE_BREAK",
        "comment",
        "dedent",
        "LINE_BREAK",
      )
  }

  @Test
  fun `should lex dedent before comment after trailing space`() {
    // given
    val code = "def bar():\n   pass  \n#comment\ndef foo():\n   pass"

    // when & then
    code shouldLexTo
      listOf(
        "def",
        "SPACE",
        "identifier",
        "(",
        ")",
        ":",
        "end of statement",
        "LINE_BREAK",
        "indent",
        "pass",
        "end of statement",
        "dedent",
        "LINE_BREAK",
        "comment",
        "LINE_BREAK",
        "def",
        "SPACE",
        "identifier",
        "(",
        ")",
        ":",
        "end of statement",
        "LINE_BREAK",
        "indent",
        "pass",
        "end of statement",
        "dedent",
      )
  }

  @Test
  fun `should lex indent before deeper comments of the new block`() {
    // given
    val code = "if a:\n      # x\n    # y\n    b\n"

    // when & then
    code shouldLexTo
      listOf(
        "if",
        "SPACE",
        "identifier",
        ":",
        "end of statement",
        "LINE_BREAK",
        "indent",
        "comment",
        "LINE_BREAK",
        "comment",
        "LINE_BREAK",
        "identifier",
        "end of statement",
        "dedent",
        "LINE_BREAK",
      )
  }

  @Test
  fun `should lex indent after comment outside the new block`() {
    // given
    val code = "if a:\n# x\n    # y\n    b\n"

    // when & then
    code shouldLexTo
      listOf(
        "if",
        "SPACE",
        "identifier",
        ":",
        "end of statement",
        "LINE_BREAK",
        "comment",
        "LINE_BREAK",
        "indent",
        "comment",
        "LINE_BREAK",
        "identifier",
        "end of statement",
        "dedent",
        "LINE_BREAK",
      )
  }

  @Test
  fun `should lex inconsistent dedent before comment outside the closed block`() {
    // given
    val code = "if a:\n    b\n  # c\n  d\n"

    // when & then
    code shouldLexTo
      listOf(
        "if",
        "SPACE",
        "identifier",
        ":",
        "end of statement",
        "LINE_BREAK",
        "indent",
        "identifier",
        "end of statement",
        "inconsistent dedent",
        "dedent",
        "LINE_BREAK",
        "comment",
        "LINE_BREAK",
        "identifier",
        "end of statement",
        "LINE_BREAK",
      )
  }

  @Test
  fun `should lex no indent after inconsistent dedent for line with same indent`() {
    // given
    val code = "if a:\n    b\n  c\n  d\ne"

    // when & then
    code shouldLexTo
      listOf(
        "if",
        "SPACE",
        "identifier",
        ":",
        "end of statement",
        "LINE_BREAK",
        "indent",
        "identifier",
        "end of statement",
        "inconsistent dedent",
        "dedent",
        "LINE_BREAK",
        "identifier",
        "end of statement",
        "LINE_BREAK",
        "identifier",
        "end of statement",
        "LINE_BREAK",
        "identifier",
        "end of statement",
      )
  }

  @Test
  fun `should lex second inconsistent dedent without dedent`() {
    // given
    val code = "if a:\n    b\n  c\n d\ne"

    // when & then
    code shouldLexTo
      listOf(
        "if",
        "SPACE",
        "identifier",
        ":",
        "end of statement",
        "LINE_BREAK",
        "indent",
        "identifier",
        "end of statement",
        "inconsistent dedent",
        "dedent",
        "LINE_BREAK",
        "identifier",
        "end of statement",
        "inconsistent dedent",
        "LINE_BREAK",
        "identifier",
        "end of statement",
        "LINE_BREAK",
        "identifier",
        "end of statement",
      )
  }

  @Test
  fun `should lex block inside inconsistent level`() {
    // given
    val code = "if a:\n    b\n  c\n      x\n  d\ne"

    // when & then
    code shouldLexTo
      listOf(
        "if",
        "SPACE",
        "identifier",
        ":",
        "end of statement",
        "LINE_BREAK",
        "indent",
        "identifier",
        "end of statement",
        "inconsistent dedent",
        "dedent",
        "LINE_BREAK",
        "identifier",
        "end of statement",
        "LINE_BREAK",
        "indent",
        "identifier",
        "end of statement",
        "dedent",
        "LINE_BREAK",
        "identifier",
        "end of statement",
        "LINE_BREAK",
        "identifier",
        "end of statement",
      )
  }

  @Test
  fun `should end unclosed bracket at end of text`() {
    // given
    val code = "f(\n"

    // when & then
    code shouldLexTo
      listOf(
        "identifier",
        "(",
        "end of statement",
        "LINE_BREAK",
      )
  }

  @Test
  fun `should end unclosed bracket at end of text after comment`() {
    // given
    val code = "f(a,\n  # c\n"

    // when & then
    code shouldLexTo
      listOf(
        "identifier",
        "(",
        "identifier",
        ",",
        "end of statement",
        "LINE_BREAK",
        "comment",
        "LINE_BREAK",
      )
  }

  @Test
  fun `should lex indent at start of file`() {
    // given
    val code = "   x"

    // when & then
    code shouldLexTo
      listOf(
        "SPACE",
        "indent",
        "identifier",
        "end of statement",
        "dedent",
      )
  }

  @Test
  fun `should lex trailing tab as line end`() {
    // given
    val code = "x\t\ny"

    // when & then
    code shouldLexTo
      listOf(
        "identifier",
        "end of statement",
        "LINE_BREAK",
        "identifier",
        "end of statement",
      )
  }

  @Test
  fun `should lex trailing space and tab as line end`() {
    // given
    val code = "x \t\ny"

    // when & then
    code shouldLexTo
      listOf(
        "identifier",
        "end of statement",
        "LINE_BREAK",
        "identifier",
        "end of statement",
      )
  }

  @Test
  fun `should lex statement break before trailing space at end of text`() {
    // given
    val code = "x  "

    // when & then
    code shouldLexTo
      listOf(
        "identifier",
        "end of statement",
        "SPACE",
      )
  }

  @Test
  fun `should lex indent after tab at start of file`() {
    // given
    val code = "\tx"

    // when & then
    code shouldLexTo
      listOf(
        "SPACE",
        "indent",
        "identifier",
        "end of statement",
        "dedent",
      )
  }

  @Test
  fun `should lex indent after line break at start of file`() {
    // given
    val code = "\n  x"

    // when & then
    code shouldLexTo
      listOf(
        "LINE_BREAK",
        "indent",
        "identifier",
        "end of statement",
        "dedent",
      )
  }

  @Test
  fun `should lex indent of line before line continuation`() {
    // given
    val code = "if a:\n  \\\n  b\n  c"

    // when & then
    code shouldLexTo
      listOf(
        "if",
        "SPACE",
        "identifier",
        ":",
        "end of statement",
        "LINE_BREAK",
        "indent",
        "identifier",
        "end of statement",
        "LINE_BREAK",
        "identifier",
        "end of statement",
        "dedent",
      )
  }

  @Test
  fun `should lex incomplete single quoted string`() {
    // given
    val code =
      """
        |"hello
        |world
      """.trimMargin()

    // when & then
    code shouldLexTo
      listOf(
        "string",
        "end of statement",
        "LINE_BREAK",
        "identifier",
        "end of statement",
      )
  }

  @Test
  fun `should lex incomplete triple quoted string`() {
    // given
    val code =
      """
        |${TRIPLE_QUOTE}hello
        |world
      """.trimMargin()

    // when & then
    code shouldLexTo
      listOf(
        "string",
        "end of statement",
      )
  }

  @Test
  fun `should lex empty text`() {
    // given
    val code = ""

    // when & then
    code shouldLexTo emptyList()
  }

  @Test
  fun `should lex text with only whitespace`() {
    // given
    val code = "  \n\t\n"

    // when & then
    code shouldLexTo
      listOf(
        "LINE_BREAK",
      )
  }

  @Test
  fun `should lex text with only a comment`() {
    // given
    val code = "# c"

    // when & then
    code shouldLexTo
      listOf(
        "comment",
      )
  }

  @Test
  fun `should not pair closing bracket across def line`() {
    // given
    val code = "f(a\ndef g():\n  h())"

    // when & then
    code shouldLexTo
      listOf(
        "identifier",
        "(",
        "identifier",
        "end of statement",
        "LINE_BREAK",
        "def",
        "SPACE",
        "identifier",
        "(",
        ")",
        ":",
        "end of statement",
        "LINE_BREAK",
        "indent",
        "identifier",
        "(",
        ")",
        ")",
        "end of statement",
        "dedent",
      )
  }

  @Test
  fun `should end bracket left open inside pair of other kind`() {
    // given
    val code = "f([a)\nx = 1"

    // when & then
    code shouldLexTo
      listOf(
        "identifier",
        "(",
        "[",
        "identifier",
        ")",
        "end of statement",
        "LINE_BREAK",
        "identifier",
        "SPACE",
        "=",
        "SPACE",
        "integer",
        "end of statement",
      )
  }

  @Test
  fun `should lex tab after spaces as indent to next tab stop`() {
    // given
    val code = "if a:\n  \tb\n        c"

    // when & then
    code shouldLexTo
      listOf(
        "if",
        "SPACE",
        "identifier",
        ":",
        "end of statement",
        "LINE_BREAK",
        "indent",
        "identifier",
        "end of statement",
        "LINE_BREAK",
        "identifier",
        "end of statement",
        "dedent",
      )
  }

  @Test
  fun `should lex line continuation at start of text as line break`() {
    // given
    val code = "\\\nx"

    // when & then
    code shouldLexTo
      listOf(
        "LINE_BREAK",
        "identifier",
        "end of statement",
      )
  }

  private infix fun String.shouldLexTo(expectedTokens: List<String>) {
    doLexerTest(this, StarlarkIndentingLexer(), expectedTokens)
  }
}
