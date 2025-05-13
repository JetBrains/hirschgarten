package org.jetbrains.bazel.languages.projectview.language

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey

sealed interface ProjectViewSection {
  data class Meta(val parser: ProjectViewSectionParser<ProjectViewSection>, val textAttributesKey: TextAttributesKey?)

  sealed interface Scalar<out T> : ProjectViewSection {
    val value: T

    data class Boolean(override val value: kotlin.Boolean) : Scalar<kotlin.Boolean> {
      companion object {
        val META = Meta(ProjectViewSectionParser.Scalar.Boolean, DefaultLanguageHighlighterColors.KEYWORD)
      }
    }

    data class Identifier(override val value: String) : Scalar<String> {
      companion object {
        val META = Meta(ProjectViewSectionParser.Scalar.Identifier, null)
      }
    }

    data class Int(override val value: kotlin.Int) : Scalar<kotlin.Int> {
      companion object {
        val META = Meta(ProjectViewSectionParser.Scalar.Int, DefaultLanguageHighlighterColors.NUMBER)
      }
    }

    data class Path(override val value: String) : Scalar<String> {
      companion object {
        val META = Meta(ProjectViewSectionParser.Scalar.Path, null)
      }
    }
  }

  sealed interface List<out T> : ProjectViewSection {
    val values: kotlin.collections.List<T>

    data class Identifiers(override val values: kotlin.collections.List<String>) : List<String> {
      companion object {
        val META = Meta(ProjectViewSectionParser.List.Identifiers, null)
      }
    }

    data class Paths(override val values: kotlin.collections.List<String>) : List<String> {
      companion object {
        val META = Meta(ProjectViewSectionParser.List.Paths, null)
      }
    }
  }

  companion object {
    /** A map of registered section keywords. */
    val KEYWORD_MAP =
      mapOf<ProjectViewSyntaxKey, Meta>(
        "allow_manual_targets_sync" to Scalar.Boolean.META,
        "android_min_sdk" to Scalar.Int.META,
        "bazel_binary" to Scalar.Path.META,
        "build_flags" to List.Identifiers.META,
        "derive_targets_from_directories" to Scalar.Boolean.META,
        "directories" to List.Paths.META,
        "enable_native_android_rules" to Scalar.Boolean.META,
        "enabled_rules" to List.Identifiers.META,
        "experimental_add_transitive_compile_time_jars" to Scalar.Boolean.META,
        "experimental_no_prune_transitive_compile_time_jars_patterns" to List.Identifiers.META,
        "experimental_transitive_compile_time_jars_target_kinds" to List.Identifiers.META,
        "ide_java_home_override" to Scalar.Path.META,
        "import_depth" to Scalar.Int.META,
        "shard_approach" to Scalar.Identifier.META,
        "shard_sync" to Scalar.Boolean.META,
        "sync_flags" to List.Identifiers.META,
        "target_shard_size" to Scalar.Int.META,
        "targets" to List.Paths.META,
        "import_run_configurations" to List.Identifiers.META,
      )
  }
}
