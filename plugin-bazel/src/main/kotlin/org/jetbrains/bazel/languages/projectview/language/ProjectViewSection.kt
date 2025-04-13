package org.jetbrains.bazel.languages.projectview.language

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey

data class ProjectViewSection<T>(val item: ProjectViewSectionItem<T>, val isList: Boolean) {
  fun parseItem(text: String) = item.parse(text)

  val highlightColor: TextAttributesKey = item.getHighlightColor()

  sealed interface ProjectViewSectionItem<T> {
    fun getHighlightColor(): TextAttributesKey = DefaultLanguageHighlighterColors.IDENTIFIER

    fun parse(text: String): ParsingResult<T> =
      if (text.isEmpty()) {
        ParsingResult.Error("Empty item")
      } else {
        parseNonEmptyItem(text)
      }

    fun parseNonEmptyItem(text: String): ParsingResult<T>

    data object Integer : ProjectViewSectionItem<Int> {
      override fun getHighlightColor(): TextAttributesKey = DefaultLanguageHighlighterColors.NUMBER

      override fun parseNonEmptyItem(text: String): ParsingResult<Int> {
        val int = text.toIntOrNull()

        return if (int == null || (text[0] == '0' && text.length > 1)) {
          ParsingResult.Error("Invalid number")
        } else {
          ParsingResult.Ok(int)
        }
      }
    }

    data object Boolean : ProjectViewSectionItem<kotlin.Boolean> {
      override fun getHighlightColor(): TextAttributesKey = DefaultLanguageHighlighterColors.KEYWORD

      override fun parseNonEmptyItem(text: String): ParsingResult<kotlin.Boolean> =
        when (text) {
          "true" -> ParsingResult.Ok(true)
          "false" -> ParsingResult.Ok(false)
          else -> ParsingResult.Error("Invalid boolean")
        }
    }

    interface SimpleSection : ProjectViewSectionItem<String> {
      override fun parseNonEmptyItem(text: String): ParsingResult<String> = ParsingResult.Ok(text)
    }

    data object Identifier : SimpleSection

    data object Path : SimpleSection
  }

  sealed interface ParsingResult<out T> {
    data class Ok<out T>(val result: T) : ParsingResult<T>

    data class Error(val message: String) : ParsingResult<Nothing>
  }

  companion object {
    val Integer = ProjectViewSection(ProjectViewSectionItem.Integer, false)
    val Boolean = ProjectViewSection(ProjectViewSectionItem.Boolean, false)
    val Identifier = ProjectViewSection(ProjectViewSectionItem.Identifier, false)
    val Path = ProjectViewSection(ProjectViewSectionItem.Path, false)

    val ListIdentifier = ProjectViewSection(ProjectViewSectionItem.Identifier, true)
    val ListPath = ProjectViewSection(ProjectViewSectionItem.Path, true)

    /** A map of registered section keywords. */
    val KEYWORD_MAP: Map<ProjectViewSyntaxKey, ProjectViewSection<*>> =
      mapOf(
        "allow_manual_targets_sync" to Boolean,
        "android_min_sdk" to Integer,
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
        "import_depth" to Integer,
        "shard_approach" to Identifier,
        "shard_sync" to Boolean,
        "sync_flags" to ListIdentifier,
        "target_shard_size" to Integer,
        "targets" to ListPath,
        "import_run_configurations" to ListIdentifier,
      )
  }
}
