package org.jetbrains.bazel.languages.projectview.lexer

object ProjectViewTokenTypes {
  @JvmField val SPACE = ProjectViewTokenType("SPACE")

  @JvmField val TAB = ProjectViewTokenType("TAB")

  @JvmField val LINE_BREAK = ProjectViewTokenType("LINE_BREAK")

  @JvmField val COMMENT = ProjectViewTokenType("COMMENT")

  @JvmField val STRING = ProjectViewTokenType("STRING")

  @JvmField val INT = ProjectViewTokenType("INT")

  @JvmField val BOOL = ProjectViewTokenType("BOOL")

  // Keywords:
  // https://github.com/JetBrains/bazel-bsp/blob/master/executioncontext/projectview/README.md
  @JvmField val IMPORT_KEYWORD = ProjectViewTokenType("import")

  @JvmField val TARGETS_KEYWORD = ProjectViewTokenType("targets")

  @JvmField val BAZEL_BINARY_KEYWORD = ProjectViewTokenType("bazel_binary")

  @JvmField val DIRECTORIES_KEYWORD = ProjectViewTokenType("directories")

  @JvmField
  val DERIVE_TARGETS_FROM_DIRECTORIES_KEYWORD =
      ProjectViewTokenType("derive_targets_from_directories")

  @JvmField val IMPORT_DEPTH_KEYWORD = ProjectViewTokenType("import_depth")

  @JvmField val WORKSPACE_TYPE_KEYWORD = ProjectViewTokenType("workspace_type")

  @JvmField val ADDITIONAL_LANGUAGES_KEYWORD = ProjectViewTokenType("additional_languages")

  @JvmField val JAVA_LANGUAGE_LEVEL_KEYWORD = ProjectViewTokenType("java_language_level")

  @JvmField val TEST_SOURCES_KEYWORD = ProjectViewTokenType("test_sources")

  @JvmField val SHARD_SYNC_KEYWORD = ProjectViewTokenType("shard_sync")

  @JvmField val TARGET_SHARD_SIZE_KEYWORD = ProjectViewTokenType("target_shard_size")

  @JvmField val EXCLUDE_LIBRARY_KEYWORD = ProjectViewTokenType("exclude_library")

  @JvmField val BUILD_FLAGS_KEYWORD = ProjectViewTokenType("build_flags")

  @JvmField val SYNC_FLAGS_KEYWORD = ProjectViewTokenType("sync_flags")

  @JvmField val TEST_FLAGS_KEYWORD = ProjectViewTokenType("test_flags")

  @JvmField val IMPORT_RUN_CONFIGURATION_KEYWORD = ProjectViewTokenType("import_run_configuration")

  @JvmField val ANDROID_SDK_PLATFORM_KEYWORD = ProjectViewTokenType("android_sdk_platform")

  @JvmField val ANDROID_MIN_SDK_KEYWORD = ProjectViewTokenType("android_min_sdk")

  @JvmField
  val GENERATED_ANDROID_RESOURCE_DIRECTORIES_KEYWORD =
      ProjectViewTokenType("generated_android_resource_directories")

  @JvmField val TS_CONFIG_RULES_KEYWORD = ProjectViewTokenType("ts_config_rules")

  @JvmField val BUILD_MANUAL_TARGETS_KEYWORD = ProjectViewTokenType("build_minimal_targets")

  @JvmField val ENABLED_RULES_KEYWORD = ProjectViewTokenType("enabled_rules")

  @JvmField val PRODUCE_TRACE_LOG_KEYWORD = ProjectViewTokenType("produce_trace_log")

  // Identifier
  @JvmField val IDENTIFIER = ProjectViewTokenType("IDENTIFIER")

  // Symbols
  @JvmField val MINUS = ProjectViewTokenType("-")

  @JvmField val MULT = ProjectViewTokenType("*")

  @JvmField val DIV = ProjectViewTokenType("/")

  @JvmField val DOT = ProjectViewTokenType(".")

  @JvmField val EQ = ProjectViewTokenType("=")

  @JvmField val COLON = ProjectViewTokenType(":")
}
