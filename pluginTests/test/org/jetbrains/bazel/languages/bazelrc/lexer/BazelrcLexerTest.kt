package org.jetbrains.bazel.languages.bazelrc.lexer

import org.jetbrains.bazel.languages.fixtures.LexerTestCase
import org.junit.jupiter.api.Test

class BazelrcLexerTest : LexerTestCase() {
  @Test
  fun `should lex a simple command`() {
    val code = "build --strip=never"

    code shouldLexTo
      listOf(
        "Bazelrc:COMMAND",
        "WHITE_SPACE",
        "Bazelrc:FLAG",
        "Bazelrc:=",
        "Bazelrc:VALUE",
      )
  }

  @Test
  fun `should lex a scoped command`() {
    // given
    val code = "build:memcheck  --test_timeout=3600"

    code shouldLexTo
      listOf(
        "Bazelrc:COMMAND",
        "Bazelrc::",
        "Bazelrc:CONFIG",
        "WHITE_SPACE",
        "Bazelrc:FLAG",
        "Bazelrc:=",
        "Bazelrc:VALUE",
      )
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
        "Bazelrc:VALUE",
        "WHITE_SPACE",
        "Bazelrc:FLAG",
        "Bazelrc:=",
        "Bazelrc:VALUE",
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
        "Bazelrc:=",
        "Bazelrc:VALUE",
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
        "Bazelrc:=",
        "Bazelrc:VALUE",
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

  @Test
  fun `flag list respect continuations`() {
    val code =
      """
      'build' \
        --test_timeout 3600 \
        --asdf=asdf asdf asdf
      """.trimIndent()

    code shouldLexTo
      listOf(
        "Bazelrc:'",
        "Bazelrc:COMMAND",
        "Bazelrc:'",
        "WHITE_SPACE",
        "Bazelrc:FLAG",
        "WHITE_SPACE",
        "Bazelrc:VALUE",
        "WHITE_SPACE",
        "Bazelrc:FLAG",
        "Bazelrc:=",
        "Bazelrc:VALUE",
        "WHITE_SPACE",
        "Bazelrc:VALUE",
        "WHITE_SPACE",
        "Bazelrc:VALUE",
      )
  }

  @Test
  fun `flags are tokenized at =`() {
    val code = "'build' --test_timeout 3600 --asdf=asdf asdf asdf"

    code shouldLexTo
      listOf(
        "Bazelrc:'",
        "Bazelrc:COMMAND",
        "Bazelrc:'",
        "WHITE_SPACE",
        "Bazelrc:FLAG",
        "WHITE_SPACE",
        "Bazelrc:VALUE",
        "WHITE_SPACE",
        "Bazelrc:FLAG",
        "Bazelrc:=",
        "Bazelrc:VALUE",
        "WHITE_SPACE",
        "Bazelrc:VALUE",
        "WHITE_SPACE",
        "Bazelrc:VALUE",
      )
  }

  @Test
  fun `imports are lexed`() {
    val code =
      """
      try-import %workspace%/.engflow.bazelrc
      
      import %workspace%/.engflow.bazelrc
      
      build
      """.trimIndent()

    code shouldLexTo
      listOf(
        "Bazelrc:IMPORT",
        "WHITE_SPACE",
        "Bazelrc:VALUE",
        "WHITE_SPACE",
        "Bazelrc:IMPORT",
        "WHITE_SPACE",
        "Bazelrc:VALUE",
        "WHITE_SPACE",
        "Bazelrc:COMMAND",
      )
  }

  private infix fun String.shouldLexTo(expectedTokens: List<String>) {
    doLexerTest(this, BazelrcLexer(), expectedTokens)
  }
}
