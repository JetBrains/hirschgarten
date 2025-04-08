package org.jetbrains.bazel.languages.projectview.language

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey

data class ProjectViewSection(
  val itemParser: ItemParser,
  val highlightColor: TextAttributesKey,
  val isList: Boolean,
) {
  fun parseItem(text: String) = itemParser.parseItem(text)

  sealed interface ItemParser {
    fun parseItem(text: kotlin.String): ParsingResult =
      if (text.isEmpty()) {
        ParsingResult.Error("Empty item!")
      } else {
        parseNonEmptyItem(text)
      }

    fun parseNonEmptyItem(text: kotlin.String): ParsingResult = ParsingResult.OK

    data object NonNegativeInteger : ItemParser {
      override fun parseNonEmptyItem(text: kotlin.String): ParsingResult =
        parseWithPredicate(
          text == "0" || (text[0] != '0' && text.all { it in '0'..'9' }),
          "Invalid number!",
        )
    }

    data object Boolean : ItemParser {
      override fun parseNonEmptyItem(text: kotlin.String): ParsingResult {
        val booleanValues = setOf("true", "false")
        return parseWithPredicate(
          text in booleanValues,
          "Invalid boolean",
        )
      }
    }

    data object String : ItemParser

    sealed interface ParsingResult {
      object OK : ParsingResult

      data class Error(val message: kotlin.String) : ParsingResult
    }
  }

  companion object {
    val NonNegativeInteger = ProjectViewSection(ItemParser.NonNegativeInteger, DefaultLanguageHighlighterColors.NUMBER, false)

    val Boolean = ProjectViewSection(ItemParser.Boolean, DefaultLanguageHighlighterColors.KEYWORD, false)
    val String = ProjectViewSection(ItemParser.String, DefaultLanguageHighlighterColors.STRING, false)
    val ListString = ProjectViewSection(ItemParser.String, DefaultLanguageHighlighterColors.STRING, true)

    /** A map of registered section keywords. */
    val KEYWORD_MAP: Map<ProjectViewSyntaxKey, ProjectViewSection> =
      mapOf(
        "allow_manual_targets_sync" to Boolean,
        "android_min_sdk" to NonNegativeInteger,
        "bazel_binary" to String,
        "build_flags" to ListString,
        "derive_targets_from_directories" to Boolean,
        "directories" to ListString,
        "enable_native_android_rules" to Boolean,
        "enabled_rules" to ListString,
        "experimental_add_transitive_compile_time_jars" to Boolean,
        "experimental_no_prune_transitive_compile_time_jars_patterns" to ListString,
        "experimental_transitive_compile_time_jars_target_kinds" to ListString,
        "ide_java_home_override" to String,
        "import_depth" to NonNegativeInteger,
        "shard_approach" to String,
        "shard_sync" to Boolean,
        "sync_flags" to ListString,
        "target_shard_size" to NonNegativeInteger,
        "targets" to ListString,
        "import_run_configurations" to ListString,
      )

    private fun parseWithPredicate(p: Boolean, errorMessage: String): ItemParser.ParsingResult =
      if (p) {
        ItemParser.ParsingResult.OK
      } else {
        ItemParser.ParsingResult.Error(errorMessage)
      }
  }
}
