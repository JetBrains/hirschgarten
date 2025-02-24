package org.jetbrains.bazel.languages.bazelquery.lexer

import org.jetbrains.bazel.languages.fixtures.LexerTestCase
import org.junit.Test

class BazelqueryLexerFlagTest : LexerTestCase() {
  @Test
  fun `should lex a no value flag`() {
    val code = "bazel query '//path/to:target' --keep_going"

    code shouldLexTo
      listOf(
        "Bazelquery:BAZEL",
        "WHITE_SPACE",
        "Bazelquery:QUERY",
        "WHITE_SPACE",
        "Bazelquery:'",
        "Bazelquery:UNQUOTED_WORD",
        "Bazelquery:'",
        "WHITE_SPACE",
        "Bazelquery:FLAG_NO_VAL",
      )
  }

  @Test
  fun `should lex a value flag with = value`() {
    val code = "bazel query '//path/to:target' --output=label_kind"

    code shouldLexTo
      listOf(
        "Bazelquery:BAZEL",
        "WHITE_SPACE",
        "Bazelquery:QUERY",
        "WHITE_SPACE",
        "Bazelquery:'",
        "Bazelquery:UNQUOTED_WORD",
        "Bazelquery:'",
        "WHITE_SPACE",
        "Bazelquery:FLAG",
        "Bazelquery:EQUALS",
        "Bazelquery:UNQUOTED_VAL",
      )
  }

  @Test
  fun `should lex a value flag with space value`() {
    val code = "bazel query '//path/to:target' --output label_kind"

    code shouldLexTo
      listOf(
        "Bazelquery:BAZEL",
        "WHITE_SPACE",
        "Bazelquery:QUERY",
        "WHITE_SPACE",
        "Bazelquery:'",
        "Bazelquery:UNQUOTED_WORD",
        "Bazelquery:'",
        "WHITE_SPACE",
        "Bazelquery:FLAG",
        "Bazelquery:EQUALS",
        "Bazelquery:UNQUOTED_VAL",
      )
  }

  private infix fun String.shouldLexTo(expectedTokens: List<String>) {
    doLexerTest(this, BazelqueryLexer(), expectedTokens)
  }
}
