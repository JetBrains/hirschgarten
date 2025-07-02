package org.jetbrains.bazel.languages.bazelquery.lexer

import org.jetbrains.bazel.languages.fixtures.LexerTestCase
import org.junit.Test

class BazelQueryLexerTest : LexerTestCase() {
  // just target test
  @Test
  fun `should lex a simple query with just target`() {
    val code = "//path/to:target"

    code shouldLexTo
      listOf(
        "BazelQuery:UNQUOTED_WORD",
      )
  }

  // commands tests
  @Test
  fun `should lex a simple one expression query`() {
    val code = "buildfiles(loadfiles(//path/to:target))"

    code shouldLexTo
      listOf(
        "BazelQuery:BUILDFILES",
        "BazelQuery:(",
        "BazelQuery:LOADFILES",
        "BazelQuery:(",
        "BazelQuery:UNQUOTED_WORD",
        "BazelQuery:)",
        "BazelQuery:)",
      )
  }

  @Test
  fun `should lex a simple two expression query`() {
    val code = "somepath(visible(//path/to:target1, //path/to:target2), //path/to:target3)"

    code shouldLexTo
      listOf(
        "BazelQuery:SOMEPATH",
        "BazelQuery:(",
        "BazelQuery:VISIBLE",
        "BazelQuery:(",
        "BazelQuery:UNQUOTED_WORD",
        "BazelQuery:,",
        "WHITE_SPACE",
        "BazelQuery:UNQUOTED_WORD",
        "BazelQuery:)",
        "BazelQuery:,",
        "WHITE_SPACE",
        "BazelQuery:UNQUOTED_WORD",
        "BazelQuery:)",
      )
  }

  @Test
  fun `should lex a simple word and expression query`() {
    val code = "labels(//path/to:target1,kind(//path/to:target2, //path/to:target3))"

    code shouldLexTo
      listOf(
        "BazelQuery:LABELS",
        "BazelQuery:(",
        "BazelQuery:UNQUOTED_WORD",
        "BazelQuery:,",
        "BazelQuery:KIND",
        "BazelQuery:(",
        "BazelQuery:UNQUOTED_WORD",
        "BazelQuery:,",
        "WHITE_SPACE",
        "BazelQuery:UNQUOTED_WORD",
        "BazelQuery:)",
        "BazelQuery:)",
      )
  }

  @Test
  fun `should lex a simple expression and optional int query`() {
    val code = "some(allrdeps(//path/to:target2), 2)"

    code shouldLexTo
      listOf(
        "BazelQuery:SOME",
        "BazelQuery:(",
        "BazelQuery:ALLRDEPS",
        "BazelQuery:(",
        "BazelQuery:UNQUOTED_WORD",
        "BazelQuery:)",
        "BazelQuery:,",
        "WHITE_SPACE",
        "BazelQuery:INTEGER",
        "BazelQuery:)",
      )
  }

  @Test
  fun `should lex a simple rdeps query`() {
    val code = "rdeps(//path/to:target1, rdeps(//path/to:target2, //path/to:target3), 1)"

    code shouldLexTo
      listOf(
        "BazelQuery:RDEPS",
        "BazelQuery:(",
        "BazelQuery:UNQUOTED_WORD",
        "BazelQuery:,",
        "WHITE_SPACE",
        "BazelQuery:RDEPS",
        "BazelQuery:(",
        "BazelQuery:UNQUOTED_WORD",
        "BazelQuery:,",
        "WHITE_SPACE",
        "BazelQuery:UNQUOTED_WORD",
        "BazelQuery:)",
        "BazelQuery:,",
        "WHITE_SPACE",
        "BazelQuery:INTEGER",
        "BazelQuery:)",
      )
  }

  @Test
  fun `should lex a simple attr query`() {
    val code = "attr(//path/to:target1, \"[\\[ ]value[,\\]]\", deps(//path/to:target2))"

    code shouldLexTo
      listOf(
        "BazelQuery:ATTR",
        "BazelQuery:(",
        "BazelQuery:UNQUOTED_WORD",
        "BazelQuery:,",
        "WHITE_SPACE",
        "BazelQuery:DQ_WORD",
        "BazelQuery:,",
        "WHITE_SPACE",
        "BazelQuery:DEPS",
        "BazelQuery:(",
        "BazelQuery:UNQUOTED_WORD",
        "BazelQuery:)",
        "BazelQuery:)",
      )
  }

  @Test
  fun `should lex a simple rbuildfiles query`() {
    val code = "rbuildfiles(//path/to:target1,//path/to:target2, '//path/to:target3', //path/to:target4)"

    code shouldLexTo
      listOf(
        "BazelQuery:RBUILDFILES",
        "BazelQuery:(",
        "BazelQuery:UNQUOTED_WORD",
        "BazelQuery:,",
        "BazelQuery:UNQUOTED_WORD",
        "BazelQuery:,",
        "WHITE_SPACE",
        "BazelQuery:SQ_WORD",
        "BazelQuery:,",
        "WHITE_SPACE",
        "BazelQuery:UNQUOTED_WORD",
        "BazelQuery:)",
      )
  }

  // keywords tests
  @Test
  fun `should lex a simple query with let`() {
    val code = "let a = deps(//path/to:my_target, 4) in deps(\$a, 3)"

    code shouldLexTo
      listOf(
        "BazelQuery:LET",
        "WHITE_SPACE",
        "BazelQuery:UNQUOTED_WORD",
        "WHITE_SPACE",
        "BazelQuery:EQUALS",
        "WHITE_SPACE",
        "BazelQuery:DEPS",
        "BazelQuery:(",
        "BazelQuery:UNQUOTED_WORD",
        "BazelQuery:,",
        "WHITE_SPACE",
        "BazelQuery:INTEGER",
        "BazelQuery:)",
        "WHITE_SPACE",
        "BazelQuery:IN",
        "WHITE_SPACE",
        "BazelQuery:DEPS",
        "BazelQuery:(",
        "BazelQuery:UNQUOTED_WORD",
        "BazelQuery:,",
        "WHITE_SPACE",
        "BazelQuery:INTEGER",
        "BazelQuery:)",
      )
  }

  @Test
  fun `should lex a simple query with set`() {
    val code = "set(//path/to:target1 //path/to:target2 //path/to:target3)"

    code shouldLexTo
      listOf(
        "BazelQuery:SET",
        "BazelQuery:(",
        "BazelQuery:UNQUOTED_WORD",
        "WHITE_SPACE",
        "BazelQuery:UNQUOTED_WORD",
        "WHITE_SPACE",
        "BazelQuery:UNQUOTED_WORD",
        "BazelQuery:)",
      )
  }

  @Test
  fun `should lex a simple query with operations on sets`() {
    val code =
      "//path/to:target1 + //path/to:target2 intersect //path/to:target3" +
        " - //path/to:target4 ^ //path/to:target5 union //path/to:target6 except //path/to:target7"

    code shouldLexTo
      listOf(
        "BazelQuery:UNQUOTED_WORD",
        "WHITE_SPACE",
        "BazelQuery:UNION",
        "WHITE_SPACE",
        "BazelQuery:UNQUOTED_WORD",
        "WHITE_SPACE",
        "BazelQuery:INTERSECT",
        "WHITE_SPACE",
        "BazelQuery:UNQUOTED_WORD",
        "WHITE_SPACE",
        "BazelQuery:EXCEPT",
        "WHITE_SPACE",
        "BazelQuery:UNQUOTED_WORD",
        "WHITE_SPACE",
        "BazelQuery:INTERSECT",
        "WHITE_SPACE",
        "BazelQuery:UNQUOTED_WORD",
        "WHITE_SPACE",
        "BazelQuery:UNION",
        "WHITE_SPACE",
        "BazelQuery:UNQUOTED_WORD",
        "WHITE_SPACE",
        "BazelQuery:EXCEPT",
        "WHITE_SPACE",
        "BazelQuery:UNQUOTED_WORD",
      )
  }

  // quotation tests
  @Test
  fun `should lex a simple query with double quotation`() {
    val code =
      "rdeps(\"//path/to:target1\", \"//path/to:target2\", 3) " +
        "union rbuildfiles(//path/to:target3, \"//path/to:target4\")"

    code shouldLexTo
      listOf(
        "BazelQuery:RDEPS",
        "BazelQuery:(",
        "BazelQuery:DQ_WORD",
        "BazelQuery:,",
        "WHITE_SPACE",
        "BazelQuery:DQ_WORD",
        "BazelQuery:,",
        "WHITE_SPACE",
        "BazelQuery:INTEGER",
        "BazelQuery:)",
        "WHITE_SPACE",
        "BazelQuery:UNION",
        "WHITE_SPACE",
        "BazelQuery:RBUILDFILES",
        "BazelQuery:(",
        "BazelQuery:UNQUOTED_WORD",
        "BazelQuery:,",
        "WHITE_SPACE",
        "BazelQuery:DQ_WORD",
        "BazelQuery:)",
      )
  }

  @Test
  fun `should lex a simple query with single quotation`() {
    val code = "somepath('//path/to:target1', 'path/to:target2') except deps('//path/to:target3')"

    code shouldLexTo
      listOf(
        "BazelQuery:SOMEPATH",
        "BazelQuery:(",
        "BazelQuery:SQ_WORD",
        "BazelQuery:,",
        "WHITE_SPACE",
        "BazelQuery:SQ_WORD",
        "BazelQuery:)",
        "WHITE_SPACE",
        "BazelQuery:EXCEPT",
        "WHITE_SPACE",
        "BazelQuery:DEPS",
        "BazelQuery:(",
        "BazelQuery:SQ_WORD",
        "BazelQuery:)",
      )
  }

  @Test
  fun `should lex a simple query with mixed quotation`() {
    val code = "deps(\"//path/to:target1\", 2) + deps('//path/to:target2')"

    code shouldLexTo
      listOf(
        "BazelQuery:DEPS",
        "BazelQuery:(",
        "BazelQuery:DQ_WORD",
        "BazelQuery:,",
        "WHITE_SPACE",
        "BazelQuery:INTEGER",
        "BazelQuery:)",
        "WHITE_SPACE",
        "BazelQuery:UNION",
        "WHITE_SPACE",
        "BazelQuery:DEPS",
        "BazelQuery:(",
        "BazelQuery:SQ_WORD",
        "BazelQuery:)",
      )
  }

  // flags test
  @Test
  fun `should lex flags`() {
    val code = "--noimplicit_deps --output=graph --order_output full"

    code shouldLexTo
      listOf(
        "BazelQuery:FLAG_NO_VAL",
        "BazelQuery:WHITE_SPACE",
        "BazelQuery:FLAG",
        "BazelQuery:EQUALS",
        "BazelQuery:UNQUOTED_VAL",
        "BazelQuery:WHITE_SPACE",
        "BazelQuery:FLAG",
        "BazelQuery:EQUALS",
        "BazelQuery:UNQUOTED_VAL",
      )
  }

  private infix fun String.shouldLexTo(expectedTokens: List<String>) {
    doLexerTest(this, BazelQueryLexer(), expectedTokens)
  }
}
