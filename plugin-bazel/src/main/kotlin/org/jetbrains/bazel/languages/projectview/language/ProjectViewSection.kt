package org.jetbrains.bazel.languages.projectview.language

import com.intellij.openapi.editor.colors.TextAttributesKey
import org.jetbrains.bazel.languages.projectview.language.ProjectViewSectionParser.ListSectionParser
import org.jetbrains.bazel.languages.projectview.language.ProjectViewSectionParser.ScalarSectionParser

sealed class ProjectViewSection(open val textAttributesKey: TextAttributesKey?) {
  data class Scalar<out T>(val item: T, override val textAttributesKey: TextAttributesKey?) : ProjectViewSection(textAttributesKey)

  data class List<out T>(val items: kotlin.collections.List<T>, override val textAttributesKey: TextAttributesKey?) :
    ProjectViewSection(textAttributesKey)

  companion object {
    /** A map of registered section keywords. */
    val KEYWORD_MAP =
      mapOf<ProjectViewSyntaxKey, ProjectViewSectionParser>(
        "allow_manual_targets_sync" to ScalarSectionParser.Boolean,
        "android_min_sdk" to ScalarSectionParser.Int,
        "bazel_binary" to ScalarSectionParser.Path,
        "build_flags" to ListSectionParser.Identifiers,
        "derive_targets_from_directories" to ScalarSectionParser.Boolean,
        "directories" to ListSectionParser.Paths,
        "enable_native_android_rules" to ScalarSectionParser.Boolean,
        "enabled_rules" to ListSectionParser.Identifiers,
        "experimental_add_transitive_compile_time_jars" to ScalarSectionParser.Boolean,
        "experimental_no_prune_transitive_compile_time_jars_patterns" to ListSectionParser.Identifiers,
        "experimental_transitive_compile_time_jars_target_kinds" to ListSectionParser.Identifiers,
        "ide_java_home_override" to ScalarSectionParser.Path,
        "import_depth" to ScalarSectionParser.Int,
        "shard_approach" to ScalarSectionParser.Identifier,
        "shard_sync" to ScalarSectionParser.Boolean,
        "sync_flags" to ListSectionParser.Identifiers,
        "target_shard_size" to ScalarSectionParser.Int,
        "targets" to ListSectionParser.Paths,
        "import_run_configurations" to ListSectionParser.Identifiers,
      )
  }
}
