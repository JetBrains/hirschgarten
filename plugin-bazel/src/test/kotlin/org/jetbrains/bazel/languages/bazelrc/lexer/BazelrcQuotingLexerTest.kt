package org.jetbrains.bazel.languages.bazelrc.lexer

import org.jetbrains.bazel.languages.fixtures.LexerTestCase
import org.junit.Test

class BazelrcQuotingLexerTest : LexerTestCase() {
  @Test
  fun `command config double quoting`() {
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
        "Bazelrc:=",
        "Bazelrc:VALUE",
      )
  }

  @Test
  fun `command config single quoting`() {
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
        "Bazelrc:=",
        "Bazelrc:VALUE",
      )
  }

  @Test
  fun `double quote value`() {
    val code = """build --bibi="asdfasfd as""""

    code shouldLexTo
      listOf(
        "Bazelrc:COMMAND",
        "WHITE_SPACE",
        "Bazelrc:FLAG",
        "Bazelrc:=",
        "Bazelrc:\"",
        "Bazelrc:VALUE",
        "Bazelrc:\"",
      )
  }

  @Test
  fun `double quoted flag=value`() {
    val code = """build "--flag\" i=asdfasfd as""""

    code shouldLexTo
      listOf(
        "Bazelrc:COMMAND",
        "WHITE_SPACE",
        "Bazelrc:\"",
        "Bazelrc:FLAG",
        "Bazelrc:=",
        "Bazelrc:VALUE",
        "Bazelrc:\"",
      )
  }

  @Test
  fun `single quoted value`() {
    val code = """build --bibi='asdfas\'fd as'"""

    code shouldLexTo
      listOf(
        "Bazelrc:COMMAND",
        "WHITE_SPACE",
        "Bazelrc:FLAG",
        "Bazelrc:=",
        "Bazelrc:'",
        "Bazelrc:VALUE",
        "Bazelrc:'",
      )
  }

  @Test
  fun `single quoted flag=value`() {
    val code = """build '--flag\' " i=asdfasfd as'"""

    code shouldLexTo
      listOf(
        "Bazelrc:COMMAND",
        "WHITE_SPACE",
        "Bazelrc:'",
        "Bazelrc:FLAG",
        "Bazelrc:=",
        "Bazelrc:VALUE",
        "Bazelrc:'",
      )
  }

  @Test
  fun `unterminated command quotes end at newline`() {
    val code =
      """
      |"asdfa dsf asdf asdf asdf asdf asdf
      |'asdfa dsf asdf asdf asdf asdf asdf
      """.trimMargin()

    // when & then
    code shouldLexTo
      listOf(
        "Bazelrc:\"",
        "Bazelrc:COMMAND",
        "WHITE_SPACE",
        "Bazelrc:'",
        "Bazelrc:COMMAND",
      )
  }

  @Test
  fun `unterminated config quotes end at newline`() {
    val code =
      """
      |build:"asdfa dsf asdf asdf asdf asdf asdf
      |build:'asdfa dsf asdf asdf asdf asdf asdf
      """.trimMargin()

    // when & then
    code shouldLexTo
      listOf(
        "Bazelrc:COMMAND",
        "Bazelrc::",
        "Bazelrc:\"",
        "Bazelrc:CONFIG",
        "WHITE_SPACE",
        "Bazelrc:COMMAND",
        "Bazelrc::",
        "Bazelrc:'",
        "Bazelrc:CONFIG",
      )
  }

  @Test
  fun `unterminated flag quotes end at newline`() {
    val code =
      """
      |build "--adsfas=asdfa dsf asdf asdf asdf asdf asdf
      |build '--asdfas=asdfa dsf asdf asdf asdf asdf asdf
      """.trimMargin()

    // when & then
    code shouldLexTo
      listOf(
        "Bazelrc:COMMAND",
        "WHITE_SPACE",
        "Bazelrc:\"",
        "Bazelrc:FLAG",
        "Bazelrc:=",
        "Bazelrc:VALUE",
        "WHITE_SPACE",
        "Bazelrc:COMMAND",
        "WHITE_SPACE",
        "Bazelrc:'",
        "Bazelrc:FLAG",
        "Bazelrc:=",
        "Bazelrc:VALUE",
      )
  }

  @Test
  fun `unterminated flag value quotes end at newline`() {
    val code =
      """
      |build --adsfas="asdfa dsf asdf asdf asdf asdf asdf
      |build --asdfas='asdfa dsf asdf asdf asdf asdf asdf
      """.trimMargin()

    // when & then
    code shouldLexTo
      listOf(
        "Bazelrc:COMMAND",
        "WHITE_SPACE",
        "Bazelrc:FLAG",
        "Bazelrc:=",
        "Bazelrc:\"",
        "Bazelrc:VALUE",
        "WHITE_SPACE",
        "Bazelrc:COMMAND",
        "WHITE_SPACE",
        "Bazelrc:FLAG",
        "Bazelrc:=",
        "Bazelrc:'",
        "Bazelrc:VALUE",
      )
  }

  @Test fun `unclosed quoted line ends at newline`() {
    val code =
      """
      "query:batch --noshow_progress

      "unknown : b at ch" --noshow_progress
      """.trimIndent()

    code shouldLexTo
      listOf(
        "Bazelrc:\"",
        "Bazelrc:COMMAND",
        "Bazelrc::",
        "Bazelrc:CONFIG",
        "WHITE_SPACE",
        "Bazelrc:\"",
        "Bazelrc:COMMAND",
        "Bazelrc::",
        "Bazelrc:CONFIG",
        "Bazelrc:\"",
        "WHITE_SPACE",
        "Bazelrc:FLAG",
      )
  }

  @Test fun `values with quotes`() {
    var code = """build --define=ij_product="intellij-2025.1""""

    code shouldLexTo
      listOf(
        "Bazelrc:COMMAND",
        "WHITE_SPACE",
        "Bazelrc:FLAG",
        "Bazelrc:=",
        "Bazelrc:VALUE",
        "Bazelrc:\"",
        "Bazelrc:VALUE",
        "Bazelrc:\"",
      )
  }

  private infix fun String.shouldLexTo(expectedTokens: List<String>) {
    super.doLexerTest(this, BazelrcLexer(), expectedTokens)
  }
}
