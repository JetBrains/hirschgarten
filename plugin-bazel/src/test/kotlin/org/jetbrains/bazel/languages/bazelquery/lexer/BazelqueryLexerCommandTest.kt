package org.jetbrains.bazel.languages.bazelquery.lexer

import org.jetbrains.bazel.languages.fixtures.LexerTestCase
import org.junit.Test

class BazelqueryLexerCommandTest : LexerTestCase() {
  @Test
  fun `should lex a quoted target`() {
    val code = "bazel query '//path/to:target'"

    code shouldLexTo
      listOf(
        "Bazelquery:BAZEL",
        "WHITE_SPACE",
        "Bazelquery:QUERY",
        "WHITE_SPACE",
        "Bazelquery:'",
        "Bazelquery:UNQUOTED_WORD",
        "Bazelquery:'",
      )
  }

  @Test
  fun `should lex a simple allpaths query`() {
    val code = "bazel query 'allpaths(//path/to:target1, //path/to:target2)'"

    code shouldLexTo
      listOf(
        "Bazelquery:BAZEL",
        "WHITE_SPACE",
        "Bazelquery:QUERY",
        "WHITE_SPACE",
        "Bazelquery:'",
        "Bazelquery:COMMAND",
        "Bazelquery:(",
        "Bazelquery:UNQUOTED_WORD",
        "Bazelquery:,",
        "WHITE_SPACE",
        "Bazelquery:UNQUOTED_WORD",
        "Bazelquery:)",
        "Bazelquery:'",
      )
  }

  @Test
  fun `should lex a simple attr query`() {
    val code = "bazel query 'attr(\"data\", \".{3,}\", //path/to:target)'"

    code shouldLexTo
      listOf(
        "Bazelquery:BAZEL",
        "WHITE_SPACE",
        "Bazelquery:QUERY",
        "WHITE_SPACE",
        "Bazelquery:'",
        "Bazelquery:COMMAND",
        "Bazelquery:(",
        "Bazelquery:DQ_WORD",
        "Bazelquery:,",
        "WHITE_SPACE",
        "Bazelquery:DQ_WORD",
        "Bazelquery:,",
        "WHITE_SPACE",
        "Bazelquery:UNQUOTED_WORD",
        "Bazelquery:)",
        "Bazelquery:'",
      )
  }

  @Test
  fun `should lex a simple buildfiles and rbuildfiles query`() {
    val code1 = "bazel query 'buildfiles(//...)'"
    val code2 = "bazel query 'rbuildfiles(//...)'"

    val list = listOf(
      "Bazelquery:BAZEL",
      "WHITE_SPACE",
      "Bazelquery:QUERY",
      "WHITE_SPACE",
      "Bazelquery:'",
      "Bazelquery:COMMAND",
      "Bazelquery:(",
      "Bazelquery:UNQUOTED_WORD",
      "Bazelquery:)",
      "Bazelquery:'",
    )

    code1 shouldLexTo list
    code2 shouldLexTo list
  }

  @Test
  fun `should lex a simple deps query`() {
    val code = "bazel query 'deps(//path/to:my_target)'"

    code shouldLexTo
      listOf(
        "Bazelquery:BAZEL",
        "WHITE_SPACE",
        "Bazelquery:QUERY",
        "WHITE_SPACE",
        "Bazelquery:'",
        "Bazelquery:COMMAND",
        "Bazelquery:(",
        "Bazelquery:UNQUOTED_WORD",
        "Bazelquery:)",
        "Bazelquery:'",
      )
  }

  @Test
  fun `should lex a simple filter query`() {
    val code = "bazel query 'filter(\"target\", //...)'"

    code shouldLexTo
      listOf(
        "Bazelquery:BAZEL",
        "WHITE_SPACE",
        "Bazelquery:QUERY",
        "WHITE_SPACE",
        "Bazelquery:'",
        "Bazelquery:COMMAND",
        "Bazelquery:(",
        "Bazelquery:DQ_WORD",
        "Bazelquery:,",
        "WHITE_SPACE",
        "Bazelquery:UNQUOTED_WORD",
        "Bazelquery:)",
        "Bazelquery:'",
      )
  }

  @Test
  fun `should lex a simple kind query`() {
    val code = "bazel query 'kind(\"cc_library\", //...)'"

    code shouldLexTo
      listOf(
        "Bazelquery:BAZEL",
        "WHITE_SPACE",
        "Bazelquery:QUERY",
        "WHITE_SPACE",
        "Bazelquery:'",
        "Bazelquery:COMMAND",
        "Bazelquery:(",
        "Bazelquery:DQ_WORD",
        "Bazelquery:,",
        "WHITE_SPACE",
        "Bazelquery:UNQUOTED_WORD",
        "Bazelquery:)",
        "Bazelquery:'",
      )
  }

  @Test
  fun `should lex a simple labels query`() {
    val code = "bazel query 'labels(srcs, //...)'"

    code shouldLexTo
      listOf(
        "Bazelquery:BAZEL",
        "WHITE_SPACE",
        "Bazelquery:QUERY",
        "WHITE_SPACE",
        "Bazelquery:'",
        "Bazelquery:COMMAND",
        "Bazelquery:(",
        "Bazelquery:UNQUOTED_WORD",
        "Bazelquery:,",
        "WHITE_SPACE",
        "Bazelquery:UNQUOTED_WORD",
        "Bazelquery:)",
        "Bazelquery:'",
      )
  }

  @Test
  fun `should lex a simple loadfiles query`() {
    val code = "bazel query 'loadfiles(//...)'"

    code shouldLexTo
      listOf(
        "Bazelquery:BAZEL",
        "WHITE_SPACE",
        "Bazelquery:QUERY",
        "WHITE_SPACE",
        "Bazelquery:'",
        "Bazelquery:COMMAND",
        "Bazelquery:(",
        "Bazelquery:UNQUOTED_WORD",
        "Bazelquery:)",
        "Bazelquery:'",
      )
  }

  @Test
  fun `should lex a simple rdeps query`() {
    val code1 = "bazel query 'rdeps(//path/to:my_target, //...)'"
    val code2 = "bazel query 'rdeps(//path/to:my_target, //..., 1)'"

    code1 shouldLexTo listOf(
      "Bazelquery:BAZEL",
      "WHITE_SPACE",
      "Bazelquery:QUERY",
      "WHITE_SPACE",
      "Bazelquery:'",
      "Bazelquery:COMMAND",
      "Bazelquery:(",
      "Bazelquery:UNQUOTED_WORD",
      "Bazelquery:,",
      "WHITE_SPACE",
      "Bazelquery:UNQUOTED_WORD",
      "Bazelquery:)",
      "Bazelquery:'",
    )

    code2 shouldLexTo listOf(
      "Bazelquery:BAZEL",
      "WHITE_SPACE",
      "Bazelquery:QUERY",
      "WHITE_SPACE",
      "Bazelquery:'",
      "Bazelquery:COMMAND",
      "Bazelquery:(",
      "Bazelquery:UNQUOTED_WORD",
      "Bazelquery:,",
      "WHITE_SPACE",
      "Bazelquery:UNQUOTED_WORD",
      "Bazelquery:,",
      "WHITE_SPACE",
      "Bazelquery:UNQUOTED_WORD",
      "Bazelquery:)",
      "Bazelquery:'",
    )
  }

  @Test
  fun `should lex a simple allrdeps query`() {
    val code1 = "bazel query 'allrdeps(//path/to:my_target)'"
    val code2 = "bazel query 'allrdeps(//path/to:my_target, 2)'"

    code1 shouldLexTo listOf(
      "Bazelquery:BAZEL",
      "WHITE_SPACE",
      "Bazelquery:QUERY",
      "WHITE_SPACE",
      "Bazelquery:'",
      "Bazelquery:COMMAND",
      "Bazelquery:(",
      "Bazelquery:UNQUOTED_WORD",
      "Bazelquery:)",
      "Bazelquery:'",
    )

    code2 shouldLexTo listOf(
      "Bazelquery:BAZEL",
      "WHITE_SPACE",
      "Bazelquery:QUERY",
      "WHITE_SPACE",
      "Bazelquery:'",
      "Bazelquery:COMMAND",
      "Bazelquery:(",
      "Bazelquery:UNQUOTED_WORD",
      "Bazelquery:,",
      "WHITE_SPACE",
      "Bazelquery:UNQUOTED_WORD",
      "Bazelquery:)",
      "Bazelquery:'",
    )
  }

  @Test
  fun `should lex a simple same_pkg_direct_rdeps query`() {
    val code = "bazel query 'same_pkg_direct_rdeps(//path/to:my_target)'"

    code shouldLexTo
      listOf(
        "Bazelquery:BAZEL",
        "WHITE_SPACE",
        "Bazelquery:QUERY",
        "WHITE_SPACE",
        "Bazelquery:'",
        "Bazelquery:COMMAND",
        "Bazelquery:(",
        "Bazelquery:UNQUOTED_WORD",
        "Bazelquery:)",
        "Bazelquery:'",
      )
  }

  @Test
  fun `should lex a simple siblings query`() {
    val code = "bazel query 'siblings(//path/to:my_target)'"

    code shouldLexTo
      listOf(
        "Bazelquery:BAZEL",
        "WHITE_SPACE",
        "Bazelquery:QUERY",
        "WHITE_SPACE",
        "Bazelquery:'",
        "Bazelquery:COMMAND",
        "Bazelquery:(",
        "Bazelquery:UNQUOTED_WORD",
        "Bazelquery:)",
        "Bazelquery:'",
      )
  }

  @Test
  fun `should lex a simple some query`() {
    val code1 = "bazel query 'some(//...)'"
    val code2 = "bazel query 'some(//..., 3)'"

    code1 shouldLexTo listOf(
      "Bazelquery:BAZEL",
      "WHITE_SPACE",
      "Bazelquery:QUERY",
      "WHITE_SPACE",
      "Bazelquery:'",
      "Bazelquery:COMMAND",
      "Bazelquery:(",
      "Bazelquery:UNQUOTED_WORD",
      "Bazelquery:)",
      "Bazelquery:'",
    )

    code2 shouldLexTo listOf(
      "Bazelquery:BAZEL",
      "WHITE_SPACE",
      "Bazelquery:QUERY",
      "WHITE_SPACE",
      "Bazelquery:'",
      "Bazelquery:COMMAND",
      "Bazelquery:(",
      "Bazelquery:UNQUOTED_WORD",
      "Bazelquery:,",
      "WHITE_SPACE",
      "Bazelquery:UNQUOTED_WORD",
      "Bazelquery:)",
      "Bazelquery:'",
    )
  }

  @Test
  fun `should lex a simple somepath and allpath query`() {
    val code1 = "bazel query 'somepath(//set1, //set2)'"
    val code2 = "bazel query 'allpaths(//set1, //set2)'"

    val list = listOf(
      "Bazelquery:BAZEL",
      "WHITE_SPACE",
      "Bazelquery:QUERY",
      "WHITE_SPACE",
      "Bazelquery:'",
      "Bazelquery:COMMAND",
      "Bazelquery:(",
      "Bazelquery:UNQUOTED_WORD",
      "Bazelquery:,",
      "WHITE_SPACE",
      "Bazelquery:UNQUOTED_WORD",
      "Bazelquery:)",
      "Bazelquery:'",
    )

    code1 shouldLexTo list
    code2 shouldLexTo list
  }

  @Test
  fun `should lex a simple tests query`() {
    val code = "bazel query 'tests(//...)'"

    code shouldLexTo
      listOf(
        "Bazelquery:BAZEL",
        "WHITE_SPACE",
        "Bazelquery:QUERY",
        "WHITE_SPACE",
        "Bazelquery:'",
        "Bazelquery:COMMAND",
        "Bazelquery:(",
        "Bazelquery:UNQUOTED_WORD",
        "Bazelquery:)",
        "Bazelquery:'",
      )
  }

  @Test
  fun `should lex a simple visible query`() {
    val code = "bazel query 'visible(//set1, //set2)'"

    code shouldLexTo
      listOf(
        "Bazelquery:BAZEL",
        "WHITE_SPACE",
        "Bazelquery:QUERY",
        "WHITE_SPACE",
        "Bazelquery:'",
        "Bazelquery:COMMAND",
        "Bazelquery:(",
        "Bazelquery:UNQUOTED_WORD",
        "Bazelquery:,",
        "WHITE_SPACE",
        "Bazelquery:UNQUOTED_WORD",
        "Bazelquery:)",
        "Bazelquery:'",
      )
  }

  @Test
  fun `should lex a query with intersect`() {
    val code = "bazel query 'deps(//my/package:target1) intersect deps(//my/package:target2)'"

    code shouldLexTo
      listOf(
        "Bazelquery:BAZEL",
        "WHITE_SPACE",
        "Bazelquery:QUERY",
        "WHITE_SPACE",
        "Bazelquery:'",
        "Bazelquery:COMMAND",
        "Bazelquery:(",
        "Bazelquery:UNQUOTED_WORD",
        "Bazelquery:)",
        "WHITE_SPACE",
        "Bazelquery:INTERSECT",
        "WHITE_SPACE",
        "Bazelquery:COMMAND",
        "Bazelquery:(",
        "Bazelquery:UNQUOTED_WORD",
        "Bazelquery:)",
        "Bazelquery:'",
      )
  }


  private infix fun String.shouldLexTo(expectedTokens: List<String>) {
    doLexerTest(this, BazelqueryLexer(), expectedTokens)
  }
}
