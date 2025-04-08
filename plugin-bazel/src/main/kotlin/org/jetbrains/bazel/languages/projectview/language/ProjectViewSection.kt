package org.jetbrains.bazel.languages.projectview.language

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey

data class ProjectViewSection(
  val item: ProjectViewSectionItem,
  val isList: Boolean,
) {
  fun parseItem(text: String) = item.parse(text)

  fun getHighlightColor() = item.getHighlightColor()

  sealed interface ProjectViewSectionItem {
    fun getHighlightColor(): TextAttributesKey = DefaultLanguageHighlighterColors.IDENTIFIER

    fun parse(text: kotlin.String): ParsingResult =
      if (text.isEmpty()) {
        ParsingResult.Error("Empty item!")
      } else {
        parseNonEmptyItem(text)
      }

    fun parseNonEmptyItem(text: kotlin.String): ParsingResult = ParsingResult.OK

    data object NonNegativeInteger : ProjectViewSectionItem {
      override fun getHighlightColor(): TextAttributesKey = DefaultLanguageHighlighterColors.NUMBER

      override fun parseNonEmptyItem(text: kotlin.String): ParsingResult =
        parseWithPredicate(
          text == "0" || (text[0] != '0' && text.all { it in '0'..'9' }),
          "Invalid number!",
        )
    }

    data object Boolean : ProjectViewSectionItem {
      override fun getHighlightColor(): TextAttributesKey = DefaultLanguageHighlighterColors.KEYWORD

      override fun parseNonEmptyItem(text: kotlin.String): ParsingResult {
        val booleanValues = setOf("true", "false")
        return parseWithPredicate(
          text in booleanValues,
          "Invalid boolean",
        )
      }
    }

    data object Identifier : ProjectViewSectionItem

    data object Path : ProjectViewSectionItem
  }

  sealed interface ParsingResult {
    object OK : ParsingResult

    data class Error(val message: kotlin.String) : ParsingResult
  }

  companion object {
    val NonNegativeInteger = ProjectViewSection(ProjectViewSectionItem.NonNegativeInteger, false)
    val Boolean = ProjectViewSection(ProjectViewSectionItem.Boolean, false)
    val Identifier = ProjectViewSection(ProjectViewSectionItem.Identifier, false)
    val Path = ProjectViewSection(ProjectViewSectionItem.Path, false)

    val ListIdentifier = ProjectViewSection(ProjectViewSectionItem.Identifier,  true)
    val ListPath = ProjectViewSection(ProjectViewSectionItem.Path, true)

    /** A map of registered section keywords. */
    val KEYWORD_MAP: Map<ProjectViewSyntaxKey, ProjectViewSection> =
      mapOf(
        "allow_manual_targets_sync" to Boolean,
        "android_min_sdk" to NonNegativeInteger,
        "bazel_binary" to Path,
        "build_flags" to ListIdentifier,
        "derive_targets_from_directories" to Boolean,
        "directories" to ListPath,
        "enable_native_android_rules" to Boolean,
        "enabled_rules" to ListIdentifier,
        "experimental_add_transitive_compile_time_jars" to Boolean,
        "experimental_no_prune_transitive_compile_time_jars_patterns" to ListIdentifier,
        "experimental_transitive_compile_time_jars_target_kinds" to ListIdentifier,
        "ide_java_home_override" to Path,
        "import_depth" to NonNegativeInteger,
        "shard_approach" to Identifier,
        "shard_sync" to Boolean,
        "sync_flags" to ListIdentifier,
        "target_shard_size" to NonNegativeInteger,
        "targets" to ListPath,
        "import_run_configurations" to ListIdentifier,
      )

    private fun parseWithPredicate(p: Boolean, errorMessage: String): ParsingResult =
      if (p) {
        ParsingResult.OK
      } else {
        ParsingResult.Error(errorMessage)
      }
  }
}
