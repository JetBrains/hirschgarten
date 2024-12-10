package org.jetbrains.bazel.languages.projectview.lexer

import org.jetbrains.bazel.languages.fixtures.LexerTestCase
import org.junit.Test

class ProjectViewLexerTest : LexerTestCase() {
  @Test
  fun `should lex import`() {
    // given
    val code = "import java/com/google/work/.blazeproject"

    // when & then
    code shouldLexTo
      listOf(
        "ProjectView:scalar_keyword",
        "ProjectView:whitespace",
        "ProjectView:identifier",
      )
  }

  @Test
  fun `should lex scala keyword`() {
    // given
    val code = "workspace_type: intellij_plugin"

    // when & then
    code shouldLexTo
      listOf(
        "ProjectView:scalar_keyword",
        "ProjectView:colon",
        "ProjectView:whitespace",
        "ProjectView:identifier",
      )
  }

  @Test
  fun `should lex comments`() {
    // given
    val code =
      """
        |# comment here
        |import_target_output:
        |  //java/com/google/work:target
      """.trimMargin()

    // when & then
    code shouldLexTo
      listOf(
        "ProjectView:comment",
        "ProjectView:newline",
        "ProjectView:list_keyword",
        "ProjectView:colon",
        "ProjectView:newline",
        "ProjectView:indent",
        "ProjectView:identifier",
        "ProjectView:colon",
        "ProjectView:identifier",
      )
  }

  @Test
  fun `should fuse whitespace`() {
    // given
    val code = "    # this comment is preceded and followed by whitespace           "

    // when & then
    code shouldLexTo
      listOf(
        "ProjectView:whitespace",
        "ProjectView:comment",
      )
  }

  @Test
  fun `should lex minimal project view`() {
    // given
    val code =
      """
        |directories:
        |  src/kotlin/app
        |  src/kotlin/utils
        |
        |targets:
        |  //src/kotlin/app/...:all
        |  //src/kotlin/utils/...:all
      """.trimMargin()

    // when & then
    code shouldLexTo
      listOf(
        "ProjectView:list_keyword",
        "ProjectView:colon",
        "ProjectView:newline",
        "ProjectView:indent",
        "ProjectView:identifier",
        "ProjectView:newline",
        "ProjectView:indent",
        "ProjectView:identifier",
        "ProjectView:newline",
        "ProjectView:newline",
        "ProjectView:list_keyword",
        "ProjectView:colon",
        "ProjectView:newline",
        "ProjectView:indent",
        "ProjectView:identifier",
        "ProjectView:colon",
        "ProjectView:identifier",
        "ProjectView:newline",
        "ProjectView:indent",
        "ProjectView:identifier",
        "ProjectView:colon",
        "ProjectView:identifier",
      )
  }

  @Test
  fun `should lex standard project view`() {
    // given
    val code =
      """
        |directories:
        |  src/kotlin/app
        |  src/kotlin/utils
        |
        |targets:
        |  //src/kotlin/app/...:all
        |  //src/kotlin/utils/...:all
      """.trimMargin()

    // when & then
    code shouldLexTo
      listOf(
        "ProjectView:list_keyword",
        "ProjectView:colon",
        "ProjectView:newline",
        "ProjectView:indent",
        "ProjectView:identifier",
        "ProjectView:newline",
        "ProjectView:indent",
        "ProjectView:identifier",
        "ProjectView:newline",
        "ProjectView:newline",
        "ProjectView:list_keyword",
        "ProjectView:colon",
        "ProjectView:newline",
        "ProjectView:indent",
        "ProjectView:identifier",
        "ProjectView:colon",
        "ProjectView:identifier",
        "ProjectView:newline",
        "ProjectView:indent",
        "ProjectView:identifier",
        "ProjectView:colon",
        "ProjectView:identifier",
      )
  }

  private infix fun String.shouldLexTo(expectedTokens: List<String>) {
    doLexerTest(this, ProjectViewLexer(), expectedTokens)
  }
}
