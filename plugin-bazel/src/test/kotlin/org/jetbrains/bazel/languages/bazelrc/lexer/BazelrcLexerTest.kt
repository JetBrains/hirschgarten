package org.jetbrains.bazel.languages.bazelrc.lexer

import org.jetbrains.bazel.languages.fixtures.LexerTestCase
import org.junit.Test

class BazelrcLexerTest : LexerTestCase() {
  @Test
  fun `should lex a simple command`() {
    "build --strip=never" shouldLexTo
      listOf("Bazelrc:COMMAND", "WHITE_SPACE", "Bazelrc:FLAG")
  }

  @Test
  fun `should lex a scoped command`() {
    // given
    val code = "build:memcheck  --test_timeout=3600"

    code shouldLexTo
      listOf("Bazelrc:COMMAND", "Bazelrc::", "Bazelrc:CONFIG", "WHITE_SPACE", "Bazelrc:FLAG")
  }

  @Test
  fun `command should only tokenize at the start`() {
    // given
    val code = "build:memcheck  build --test_timeout=3600"

    // when & then
    code shouldLexTo
      listOf(
        "Bazelrc:COMMAND",
        "Bazelrc::",
        "Bazelrc:CONFIG",
        "WHITE_SPACE",
        "Bazelrc:FLAG",
        "WHITE_SPACE",
        "Bazelrc:FLAG",
      )
  }

  @Test
  fun `handle quotes`() {
    // given
    val code = "'build' --test_timeout=3600"

    // when & then
    code shouldLexTo
      listOf(
        "Bazelrc:'",
        "Bazelrc:COMMAND",
        "Bazelrc:'",
        "WHITE_SPACE",
        "Bazelrc:FLAG",
      )
  }

  @Test
  fun `handle double quotes and spaces`() {
    // given
    val code =
      """
      |"build:a a a a" --test_timeout=3600
      """.trimMargin()

    // when & then
    code shouldLexTo
      listOf(
        "Bazelrc:\"",
        "Bazelrc:COMMAND",
        "Bazelrc::",
        "Bazelrc:CONFIG",
        "Bazelrc:\"",
        "WHITE_SPACE",
        "Bazelrc:FLAG",
      )
  }

  @Test
  fun `handle single quotes and spaces`() {
    // given
    val code = "'build:a a a a' --test_timeout=3600"

    // when & then
    code shouldLexTo
      listOf(
        "Bazelrc:'",
        "Bazelrc:COMMAND",
        "Bazelrc::",
        "Bazelrc:CONFIG",
        "Bazelrc:'",
        "WHITE_SPACE",
        "Bazelrc:FLAG",
      )
  }

  @Test
  fun `newline should reset line lexing`() {
    // given
    val code =
      """
      |build:memcheck 
      |  --strip=ne\\
      |ver --test_timeout=3600
      """.trimMargin()

    // when & then
    code shouldLexTo
      listOf(
        "Bazelrc:COMMAND",
        "Bazelrc::",
        "Bazelrc:CONFIG",
        "WHITE_SPACE",
        "Bazelrc:COMMAND",
        "WHITE_SPACE",
        "Bazelrc:FLAG",
      )
  }

  @Test
  fun `unclosed quote ends at newline`() {
    val code =
      """
      |"adsfa adsf adsf asd: adsfasdfasd asdfasdf
      |
      |asdfas:
      """.trimMargin()

    // when & then
    code shouldLexTo
      listOf(
        "Bazelrc:\"",
        "Bazelrc:COMMAND",
        "Bazelrc::",
        "Bazelrc:CONFIG",
        "WHITE_SPACE",
        "Bazelrc:COMMAND",
        "Bazelrc::",
      )
  }

  @Test fun `bibi`() {
    val code =
      """
      |"asdfa dsf asdf asdf asdf asdf asdf
      |asdfasd
      """.trimMargin()

    // when & then
    code shouldLexTo
      listOf(
        "Bazelrc:\"",
        "Bazelrc:COMMAND",
        "WHITE_SPACE",
        "Bazelrc:COMMAND",
      )
  }

  private infix fun String.shouldLexTo(expectedTokens: List<String>) {
    doLexerTest(this, BazelrcHighlightingLexer(), expectedTokens)
  }
}
