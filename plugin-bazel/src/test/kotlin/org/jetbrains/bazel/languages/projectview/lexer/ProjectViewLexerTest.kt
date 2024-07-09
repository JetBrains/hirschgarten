package org.jetbrains.bazel.languages.projectview.lexer

import org.jetbrains.bazel.languages.fixtures.LexerTestCase
import org.junit.Test


class ProjectViewLexerTest : LexerTestCase() {
  @Test
  fun `should lex boolean literal`() {
    // given
    val code = "derive_targets_from_directories: false"

    // when & then
    code shouldLexTo listOf(
      "ProjectView:derive_targets_from_directories",
      "ProjectView::",
      "ProjectView:SPACE",
      "ProjectView:BOOL",
    )
  }

  @Test
  fun `should lex int literal`() {
    // given
    val code = "import_depth: 0"

    // when & then
    code shouldLexTo listOf(
      "ProjectView:import_depth",
      "ProjectView::",
      "ProjectView:SPACE",
      "ProjectView:INT",
    )
  }

  @Test
  fun `should lex multiline property`() {
    // given
    val code =
      """
        |enabled_rules:
        |  rules_kotlin
        |  rules_java
      """.trimMargin()

    // when & then
    code shouldLexTo listOf(
      "ProjectView:enabled_rules",
      "ProjectView::",
      "ProjectView:LINE_BREAK",
      "ProjectView:SPACE",
      "ProjectView:SPACE",
      "ProjectView:IDENTIFIER",
      "ProjectView:LINE_BREAK",
      "ProjectView:SPACE",
      "ProjectView:SPACE",
      "ProjectView:IDENTIFIER",
    )
  }

  private infix fun String.shouldLexTo(expectedTokens: List<String>) {
    doLexerTest(this, ProjectViewLexer(), expectedTokens)
  }
}