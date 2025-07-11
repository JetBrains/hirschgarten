package org.jetbrains.bazel.languages.projectview.language

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

  data class SectionMetadata(val sectionName: ProjectViewSyntaxKey, val sectionType: SectionType)

  fun isSectionSupported(sectionName: ProjectViewSyntaxKey): Boolean {
    return supportedSections.contains(sectionName)
  }

  /*
   * The map below should contain all syntax-wise valid section names.
   * However, the ones that are actually supported by the plugin are defined in the ProjectView data class.
   * This way we can recognize the keyword and annotate it accordingly.
   * */
  val KEYWORD_MAP: Map<ProjectViewSyntaxKey, SectionMetadata> =
    listOf(
      SectionMetadata("allow_manual_targets_sync", SectionType.Scalar.Boolean),
      SectionMetadata("android_min_sdk", SectionType.Scalar.Integer),
      SectionMetadata("bazel_binary", SectionType.Scalar.String),
      SectionMetadata("build_flags", SectionType.List.String),
      SectionMetadata("derive_targets_from_directories", SectionType.Scalar.Boolean),
      SectionMetadata("directories", SectionType.List.String),
      SectionMetadata("enable_native_android_rules", SectionType.Scalar.Boolean),
      SectionMetadata("enabled_rules", SectionType.List.String),
      SectionMetadata("experimental_add_transitive_compile_time_jars", SectionType.Scalar.Boolean),
      SectionMetadata("experimental_no_prune_transitive_compile_time_jars_patterns", SectionType.List.String),
      SectionMetadata("experimental_transitive_compile_time_jars_target_kinds", SectionType.List.String),
      SectionMetadata("experimental_prioritize_libraries_over_modules_target_kinds", SectionType.List.String),
      SectionMetadata("ide_java_home_override", SectionType.Scalar.String),
      SectionMetadata("import_depth", SectionType.Scalar.Integer),
      SectionMetadata("shard_approach", SectionType.Scalar.String),
      SectionMetadata("shard_sync", SectionType.Scalar.Boolean),
      SectionMetadata("sync_flags", SectionType.List.String),
      SectionMetadata("target_shard_size", SectionType.Scalar.Integer),
      SectionMetadata("targets", SectionType.List.String),
      SectionMetadata("import_run_configurations", SectionType.List.String),
      SectionMetadata("test_sources", SectionType.List.String), // used by Google's plugin
      SectionMetadata("gazelle_target", SectionType.Scalar.String),
      SectionMetadata("index_all_files_in_directories", SectionType.Scalar.Boolean),
      SectionMetadata("python_code_generator_rule_names", SectionType.List.String),
      SectionMetadata("use_query_sync", SectionType.Scalar.Boolean),
      SectionMetadata("workspace_type", SectionType.Scalar.String),
      SectionMetadata("additional_languages", SectionType.List.String),
      SectionMetadata("java_language_level", SectionType.Scalar.String),
      SectionMetadata("exclude_library", SectionType.List.String),
      SectionMetadata("test_flags", SectionType.List.String),
      SectionMetadata("android_sdk_platform", SectionType.Scalar.String),
      SectionMetadata("generated_android_resource_directories", SectionType.List.String),
      SectionMetadata("ts_config_rules", SectionType.List.String),
      SectionMetadata("import_ijars", SectionType.Scalar.Boolean),
    ).associateBy { it.sectionName }
}
