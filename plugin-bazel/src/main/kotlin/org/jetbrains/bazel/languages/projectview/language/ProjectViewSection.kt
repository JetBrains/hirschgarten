package org.jetbrains.bazel.languages.projectview.language

import com.intellij.openapi.editor.colors.TextAttributesKey

sealed class ProjectViewSection(open val textAttributesKey: TextAttributesKey?) {
  data class Scalar<out T>(val item: T, override val textAttributesKey: TextAttributesKey?) : ProjectViewSection(textAttributesKey)

  data class List<out T>(val items: kotlin.collections.List<T>, override val textAttributesKey: TextAttributesKey?) :
    ProjectViewSection(textAttributesKey)

  companion object {
    /** A map of registered section keywords. */
    val KEYWORD_MAP: Map<ProjectViewSyntaxKey, ProjectViewSectionParser> =
      mapOf(
        "allow_manual_targets_sync" to ProjectViewSectionParser.ScalarSectionParser.Boolean,
        "android_min_sdk" to ProjectViewSectionParser.ScalarSectionParser.Int,
        "bazel_binary" to ProjectViewSectionParser.ScalarSectionParser.Path,
        "build_flags" to ProjectViewSectionParser.ListSectionParser.Identifiers,
        "derive_targets_from_directories" to ProjectViewSectionParser.ScalarSectionParser.Boolean,
        "directories" to ProjectViewSectionParser.ListSectionParser.Paths,
        "enable_native_android_rules" to ProjectViewSectionParser.ScalarSectionParser.Boolean,
        "enabled_rules" to ProjectViewSectionParser.ListSectionParser.Identifiers,
        "experimental_add_transitive_compile_time_jars" to ProjectViewSectionParser.ScalarSectionParser.Boolean,
        "experimental_no_prune_transitive_compile_time_jars_patterns" to ProjectViewSectionParser.ListSectionParser.Identifiers,
        "experimental_transitive_compile_time_jars_target_kinds" to ProjectViewSectionParser.ListSectionParser.Identifiers,
        "ide_java_home_override" to ProjectViewSectionParser.ScalarSectionParser.Path,
        "import_depth" to ProjectViewSectionParser.ScalarSectionParser.Int,
        "shard_approach" to ProjectViewSectionParser.ScalarSectionParser.Identifier,
        "shard_sync" to ProjectViewSectionParser.ScalarSectionParser.Boolean,
        "sync_flags" to ProjectViewSectionParser.ListSectionParser.Identifiers,
        "target_shard_size" to ProjectViewSectionParser.ScalarSectionParser.Int,
        "targets" to ProjectViewSectionParser.ListSectionParser.Paths,
        "import_run_configurations" to ProjectViewSectionParser.ListSectionParser.Identifiers,
      )
  }
}
