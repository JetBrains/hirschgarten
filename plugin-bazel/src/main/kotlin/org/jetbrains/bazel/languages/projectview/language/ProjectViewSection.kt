package org.jetbrains.bazel.languages.projectview.language

object ProjectViewSection {
  sealed interface Parser {
    sealed interface Scalar : Parser {
      data object Integer : Scalar

      data object Boolean : Scalar

      data object String : Scalar
    }

    sealed interface List<T : Scalar> : Parser {
      data object String : List<Scalar.String>
    }
  }

  /** A map of registered section keywords. */
  val KEYWORD_MAP: Map<ProjectViewSyntaxKey, Parser> =
    mapOf(
      "allow_manual_targets_sync" to Parser.Scalar.Boolean,
      "android_min_sdk" to Parser.Scalar.Integer,
      "bazel_binary" to Parser.Scalar.String,
      "build_flags" to Parser.List.String,
      "derive_targets_from_directories" to Parser.Scalar.Boolean,
      "directories" to Parser.List.String,
      "enable_native_android_rules" to Parser.Scalar.Boolean,
      "enabled_rules" to Parser.List.String,
      "experimental_add_transitive_compile_time_jars" to Parser.Scalar.Boolean,
      "experimental_no_prune_transitive_compile_time_jars_patterns" to Parser.List.String,
      "experimental_transitive_compile_time_jars_target_kinds" to Parser.List.String,
      "experimental_prioritize_libraries_over_modules_target_kinds" to Parser.List.String,
      "ide_java_home_override" to Parser.Scalar.String,
      "import_depth" to Parser.Scalar.Integer,
      "shard_approach" to Parser.Scalar.String,
      "shard_sync" to Parser.Scalar.Boolean,
      "sync_flags" to Parser.List.String,
      "target_shard_size" to Parser.Scalar.Integer,
      "targets" to Parser.List.String,
      "import_run_configurations" to Parser.List.String,
      "test_sources" to Parser.List.String, // used by Google's plugin
      "gazelle_target" to Parser.Scalar.String,
      "index_all_files_in_directories" to Parser.Scalar.Boolean,
      "python_code_generator_rule_names" to Parser.List.String,
    )
}
