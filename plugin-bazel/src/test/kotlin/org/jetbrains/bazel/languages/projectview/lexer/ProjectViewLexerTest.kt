package org.jetbrains.bazel.languages.projectview.lexer

import org.jetbrains.bazel.languages.fixtures.LexerTestCase
import org.jetbrains.bazel.languages.projectview.language.ProjectViewSection
import org.jetbrains.bazel.languages.projectview.language.ProjectViewSyntaxKey
import org.junit.Test

class ProjectViewLexerTest : LexerTestCase() {
  sealed interface Section {
    data class Scalar(val name: String) : Section {
      override fun getExpectedTokens(): kotlin.collections.List<String> = SCALAR_SECTION

      override fun getExampleCode() = "$name: value"
    }

    data class List(val name: String) : Section {
      override fun getExpectedTokens(): kotlin.collections.List<String> = LIST_SECTION

      override fun getExampleCode() = "$name:\n  value"
    }

    fun getExpectedTokens(): kotlin.collections.List<String>

    fun getExampleCode(): String

    companion object {
      fun create(key: ProjectViewSyntaxKey, sectionType: ProjectViewSection.SectionType): Section =
        when (sectionType) {
          is ProjectViewSection.SectionType.Scalar -> Scalar(key)
          is ProjectViewSection.SectionType.List -> List(key)
        }

      private val SCALAR_SECTION =
        listOf(
          "ProjectView:section_keyword",
          "ProjectView:colon",
          "ProjectView:whitespace",
          "ProjectView:identifier",
        )

      /** A vector section. */
      private val LIST_SECTION =
        listOf(
          "ProjectView:section_keyword",
          "ProjectView:colon",
          "ProjectView:newline",
          "ProjectView:indent",
          "ProjectView:identifier",
        )
    }
  }

  @Test
  fun `should lex import keywords`() {
    // given
    val code =
      """
        |import /pathA
        |try_import /pathB
      """.trimMargin()

    // when & then
    code shouldLexTo
      listOf(
        "ProjectView:import_keyword",
        "ProjectView:whitespace",
        "ProjectView:identifier",
        "ProjectView:newline",
        "ProjectView:import_keyword",
        "ProjectView:whitespace",
        "ProjectView:identifier",
      )
  }

  @Test
  fun `should lex registered sections`() {
    ProjectViewSection.KEYWORD_MAP.map { (key, metadata) -> Section.create(key, metadata.sectionType) }.forEach { section ->
      section.getExampleCode() shouldLexTo
        section.getExpectedTokens()
    }
  }

  @Test
  fun `should lex comments`() {
    // given
    val code =
      """
        |# comment here
        |targets:
        |  //java/com/google/work:target
      """.trimMargin()

    // when & then
    code shouldLexTo
      listOf(
        "ProjectView:comment",
        "ProjectView:newline",
        "ProjectView:section_keyword",
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
        "ProjectView:section_keyword",
        "ProjectView:colon",
        "ProjectView:newline",
        "ProjectView:indent",
        "ProjectView:identifier",
        "ProjectView:newline",
        "ProjectView:indent",
        "ProjectView:identifier",
        "ProjectView:newline",
        "ProjectView:newline",
        "ProjectView:section_keyword",
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
