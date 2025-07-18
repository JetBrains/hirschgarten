package org.jetbrains.bazel.languages.projectview.language

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import org.jetbrains.bazel.languages.projectview.completion.DirectoriesCompletionProvider
import org.jetbrains.bazel.languages.projectview.completion.FiletypeCompletionProvider
import org.jetbrains.bazel.languages.projectview.completion.FlagCompletionProvider
import org.jetbrains.bazel.languages.projectview.completion.SimpleCompletionProvider
import org.jetbrains.bazel.languages.projectview.completion.TargetCompletionProvider
import org.jetbrains.bazel.projectview.model.supportedSections

object ProjectViewSection {
  sealed interface SectionType {
    sealed interface Scalar : SectionType {
      data object Integer : Scalar

      data object Boolean : Scalar

      data object String : Scalar
    }

    sealed interface List<T : Scalar> : SectionType {
      data object String : List<Scalar.String>
    }
  }

  data class SectionMetadata(
    val sectionName: ProjectViewSyntaxKey,
    val sectionType: SectionType,
    val completionProvider: CompletionProvider<CompletionParameters>? = null,
  )

  fun isSectionSupported(sectionName: ProjectViewSyntaxKey): Boolean = supportedSections.contains(sectionName)

  /*
   * The map below should contain all syntax-wise valid section names.
   * However, the ones that are actually supported by the plugin are defined in the ProjectView data class.
   * This way we can recognize the keyword and annotate it accordingly.
   * */
  val KEYWORD_MAP: Map<ProjectViewSyntaxKey, SectionMetadata> =
    listOf(
      SectionMetadata("allow_manual_targets_sync", SectionType.Scalar.Boolean, booleanCompletionProvider()),
      SectionMetadata("android_min_sdk", SectionType.Scalar.Integer),
      SectionMetadata("bazel_binary", SectionType.Scalar.String),
      SectionMetadata("build_flags", SectionType.List.String, FlagCompletionProvider("build")),
      SectionMetadata("test_flags", SectionType.List.String, FlagCompletionProvider("test")),
      SectionMetadata("derive_targets_from_directories", SectionType.Scalar.Boolean, booleanCompletionProvider()),
      SectionMetadata("directories", SectionType.List.String, DirectoriesCompletionProvider()),
      SectionMetadata("enable_native_android_rules", SectionType.Scalar.Boolean, booleanCompletionProvider()),
      SectionMetadata("enabled_rules", SectionType.List.String),
      SectionMetadata("experimental_add_transitive_compile_time_jars", SectionType.Scalar.Boolean, booleanCompletionProvider()),
      SectionMetadata("experimental_no_prune_transitive_compile_time_jars_patterns", SectionType.List.String),
      SectionMetadata("experimental_transitive_compile_time_jars_target_kinds", SectionType.List.String),
      SectionMetadata("experimental_prioritize_libraries_over_modules_target_kinds", SectionType.List.String),
      SectionMetadata("ide_java_home_override", SectionType.Scalar.String),
      SectionMetadata("import_depth", SectionType.Scalar.Integer),
      SectionMetadata("shard_approach", SectionType.Scalar.String),
      SectionMetadata("shard_sync", SectionType.Scalar.Boolean, booleanCompletionProvider()),
      SectionMetadata("sync_flags", SectionType.List.String, FlagCompletionProvider("sync")),
      SectionMetadata("target_shard_size", SectionType.Scalar.Integer),
      SectionMetadata("targets", SectionType.List.String, TargetCompletionProvider()),
      SectionMetadata("import_run_configurations", SectionType.List.String, FiletypeCompletionProvider(".xml")),
      SectionMetadata("test_sources", SectionType.List.String), // used by Google's plugin
      SectionMetadata("gazelle_target", SectionType.Scalar.String),
      SectionMetadata("index_all_files_in_directories", SectionType.Scalar.Boolean, booleanCompletionProvider()),
      SectionMetadata("python_code_generator_rule_names", SectionType.List.String),
      SectionMetadata("use_query_sync", SectionType.Scalar.Boolean, booleanCompletionProvider()),
      SectionMetadata(
        "workspace_type",
        SectionType.Scalar.String,
        SimpleCompletionProvider(listOf("java", "python", "dart", "android", "javascript")),
      ),
      SectionMetadata(
        "additional_languages",
        SectionType.List.String,
        SimpleCompletionProvider(
          listOf("android", "dart", "java", "javascript", "kotlin", "python", "typescript", "go", "c"),
        ),
      ),
      SectionMetadata("import", SectionType.Scalar.String, FiletypeCompletionProvider(".bazelproject")),
      SectionMetadata("java_language_level", SectionType.Scalar.String),
      SectionMetadata("exclude_library", SectionType.List.String),
      SectionMetadata("android_sdk_platform", SectionType.Scalar.String),
      SectionMetadata("generated_android_resource_directories", SectionType.List.String),
      SectionMetadata("ts_config_rules", SectionType.List.String),
      SectionMetadata("import_ijars", SectionType.Scalar.Boolean, booleanCompletionProvider()),
      SectionMetadata("debug_flags", SectionType.List.String, FlagCompletionProvider("debug")),
    ).associateBy { it.sectionName }

  private fun booleanCompletionProvider() = SimpleCompletionProvider(listOf("true", "false"))
}
