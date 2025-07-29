package org.jetbrains.bazel.languages.projectview.language

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import org.jetbrains.bazel.languages.bazelrc.flags.Flag
import org.jetbrains.bazel.languages.projectview.completion.FlagCompletionProvider
import org.jetbrains.bazel.languages.projectview.completion.SimpleCompletionProvider
import org.jetbrains.bazel.projectview.model.supportedSections
import java.nio.file.Path

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

  interface SectionValueParser<T> {
    fun parse(value: String): T?
  }

  private class IdentityValueParser : SectionValueParser<String> {
    override fun parse(value: String): String = value
  }

  private class BooleanValueParser : SectionValueParser<Boolean> {
    override fun parse(value: String): Boolean? = value.toBooleanStrictOrNull()
  }

  private class IntegerValueParser : SectionValueParser<Int> {
    override fun parse(value: String): Int? = value.toIntOrNull()
  }

  private class FlagValueParser : SectionValueParser<Flag> {
    override fun parse(value: String): Flag? = Flag.byName(value)
  }

  private class PathValueParser : SectionValueParser<Path> {
    override fun parse(value: String): Path? = Path.of(value)
  }

  sealed interface Value<T> {
    class Included<T>(val value: T) : Value<T>

    class Excluded<T>(val value: T) : Value<T>
  }

  private class IncludedExcludedParser<T>(private val actualParser: SectionValueParser<T>) : SectionValueParser<Value<T>> {
    override fun parse(value: String): Value<T>? =
      if (value.startsWith("-")) {
        actualParser.parse(value.substring(1))?.let { Value.Excluded(it) }
      } else {
        actualParser.parse(value)?.let { Value.Included(it) }
      }
  }

  data class SectionMetadata(
    val sectionName: ProjectViewSyntaxKey,
    val sectionType: SectionType,
    val completionProvider: CompletionProvider<CompletionParameters>? = null,
    val sectionValueParser: SectionValueParser<*> = IdentityValueParser(),
  )

  fun isSectionSupported(sectionName: ProjectViewSyntaxKey): Boolean = supportedSections.contains(sectionName)

  const val ALLOW_MANUAL_TARGETS_SYNC = "allow_manual_targets_sync"
  const val ANDROID_MIN_SDK = "android_min_sdk"
  const val BAZEL_BINARY = "bazel_binary"
  const val BUILD_FLAGS = "build_flags"
  const val TEST_FLAGS = "test_flags"
  const val DERIVE_TARGETS_FROM_DIRECTORIES = "derive_targets_from_directories"
  const val DIRECTORIES = "directories"
  const val ENABLE_NATIVE_ANDROID_RULES = "enable_native_android_rules"
  const val ENABLED_RULES = "enabled_rules"
  const val EXPERIMENTAL_ADD_TRANSITIVE_COMPILE_TIME_JARS = "experimental_add_transitive_compile_time_jars"
  const val EXPERIMENTAL_NO_PRUNE_TRANSITIVE_COMPILE_TIME_JARS_PATTERNS = "experimental_no_prune_transitive_compile_time_jars_patterns"
  const val EXPERIMENTAL_TRANSITIVE_COMPILE_TIME_JARS_TARGET_KINDS = "experimental_transitive_compile_time_jars_target_kinds"
  const val EXPERIMENTAL_PRIORITIZE_LIBRARIES_OVER_MODULES_TARGET_KINDS = "experimental_prioritize_libraries_over_modules_target_kinds"
  const val IDE_JAVA_HOME_OVERRIDE = "ide_java_home_override"
  const val IMPORT_DEPTH = "import_depth"
  const val SHARD_APPROACH = "shard_approach"
  const val SHARD_SYNC = "shard_sync"
  const val SYNC_FLAGS = "sync_flags"
  const val TARGET_SHARD_SIZE = "target_shard_size"
  const val TARGETS = "targets"
  const val IMPORT_RUN_CONFIGURATIONS = "import_run_configurations"
  const val TEST_SOURCES = "test_sources"
  const val GAZELLE_TARGET = "gazelle_target"
  const val INDEX_ALL_FILES_IN_DIRECTORIES = "index_all_files_in_directories"
  const val PYTHON_CODE_GENERATOR_RULE_NAMES = "python_code_generator_rule_names"
  const val USE_QUERY_SYNC = "use_query_sync"
  const val WORKSPACE_TYPE = "workspace_type"
  const val ADDITIONAL_LANGUAGES = "additional_languages"
  const val JAVA_LANGUAGE_LEVEL = "java_language_level"
  const val EXCLUDE_LIBRARY = "exclude_library"
  const val ANDROID_SDK_PLATFORM = "android_sdk_platform"
  const val GENERATED_ANDROID_RESOURCE_DIRECTORIES = "generated_android_resource_directories"
  const val TS_CONFIG_RULES = "ts_config_rules"
  const val IMPORT_IJARS = "import_ijars"
  const val DEBUG_FLAGS = "debug_flags"

  /*
   * The map below should contain all syntax-wise valid section names.
   * However, the ones that are actually supported by the plugin are defined in the ProjectView data class.
   * This way we can recognize the keyword and annotate it accordingly.
   * */
  val KEYWORD_MAP: Map<ProjectViewSyntaxKey, SectionMetadata> =
    listOf(
      SectionMetadata(
        sectionName = ALLOW_MANUAL_TARGETS_SYNC,
        sectionType = SectionType.Scalar.Boolean,
        completionProvider = booleanCompletionProvider(),
        sectionValueParser = BooleanValueParser(),
      ),
      SectionMetadata(
        sectionName = ANDROID_MIN_SDK,
        sectionType = SectionType.Scalar.Integer,
        completionProvider = null,
        sectionValueParser = IntegerValueParser(),
      ),
      SectionMetadata(
        sectionName = BAZEL_BINARY,
        sectionType = SectionType.Scalar.String,
        completionProvider = null,
        sectionValueParser = PathValueParser(),
      ),
      SectionMetadata(
        sectionName = BUILD_FLAGS,
        sectionType = SectionType.List.String,
        completionProvider = FlagCompletionProvider("build"),
        sectionValueParser = FlagValueParser(),
      ),
      SectionMetadata(
        sectionName = TEST_FLAGS,
        sectionType = SectionType.List.String,
        completionProvider = FlagCompletionProvider("test"),
        sectionValueParser = FlagValueParser(),
      ),
      SectionMetadata(
        sectionName = DERIVE_TARGETS_FROM_DIRECTORIES,
        sectionType = SectionType.Scalar.Boolean,
        completionProvider = booleanCompletionProvider(),
        sectionValueParser = BooleanValueParser(),
      ),
      SectionMetadata(
        sectionName = DIRECTORIES,
        sectionType = SectionType.List.String,
        completionProvider = null,
        sectionValueParser = IncludedExcludedParser(PathValueParser()),
      ),
      SectionMetadata(
        sectionName = ENABLE_NATIVE_ANDROID_RULES,
        sectionType = SectionType.Scalar.Boolean,
        completionProvider = booleanCompletionProvider(),
        sectionValueParser = BooleanValueParser(),
      ),
      SectionMetadata(
        sectionName = ENABLED_RULES,
        sectionType = SectionType.List.String,
        completionProvider = null,
        sectionValueParser = IdentityValueParser(),
      ),
      SectionMetadata(
        sectionName = EXPERIMENTAL_ADD_TRANSITIVE_COMPILE_TIME_JARS,
        sectionType = SectionType.Scalar.Boolean,
        completionProvider = booleanCompletionProvider(),
        sectionValueParser = BooleanValueParser(),
      ),
      SectionMetadata(
        sectionName = EXPERIMENTAL_NO_PRUNE_TRANSITIVE_COMPILE_TIME_JARS_PATTERNS,
        sectionType = SectionType.List.String,
      ),
      SectionMetadata(
        sectionName = EXPERIMENTAL_TRANSITIVE_COMPILE_TIME_JARS_TARGET_KINDS,
        sectionType = SectionType.List.String,
      ),
      SectionMetadata(
        sectionName = EXPERIMENTAL_PRIORITIZE_LIBRARIES_OVER_MODULES_TARGET_KINDS,
        sectionType = SectionType.List.String,
      ),
      SectionMetadata(
        sectionName = IDE_JAVA_HOME_OVERRIDE,
        sectionType = SectionType.Scalar.String,
        completionProvider = null,
        sectionValueParser = PathValueParser(),
      ),
      SectionMetadata(
        sectionName = IMPORT_DEPTH,
        sectionType = SectionType.Scalar.Integer,
        completionProvider = null,
        sectionValueParser = IntegerValueParser(),
      ),
      SectionMetadata(
        sectionName = SHARD_APPROACH,
        sectionType = SectionType.Scalar.String,
      ),
      SectionMetadata(
        sectionName = SHARD_SYNC,
        sectionType = SectionType.Scalar.Boolean,
        completionProvider = booleanCompletionProvider(),
        sectionValueParser = BooleanValueParser(),
      ),
      SectionMetadata(
        sectionName = SYNC_FLAGS,
        sectionType = SectionType.List.String,
        completionProvider = FlagCompletionProvider("sync"),
        sectionValueParser = FlagValueParser(),
      ),
      SectionMetadata(
        sectionName = TARGET_SHARD_SIZE,
        sectionType = SectionType.Scalar.Integer,
        completionProvider = null,
        sectionValueParser = IntegerValueParser(),
      ),
      SectionMetadata(
        sectionName = TARGETS,
        sectionType = SectionType.List.String,
      ),
      SectionMetadata(
        sectionName = IMPORT_RUN_CONFIGURATIONS,
        sectionType = SectionType.List.String,
        completionProvider = null,
        sectionValueParser = PathValueParser(),
      ),
      SectionMetadata(
        sectionName = TEST_SOURCES,
        sectionType = SectionType.List.String, // used by Google's plugin
      ),
      SectionMetadata(
        sectionName = GAZELLE_TARGET,
        sectionType = SectionType.Scalar.String,
      ),
      SectionMetadata(
        sectionName = INDEX_ALL_FILES_IN_DIRECTORIES,
        sectionType = SectionType.Scalar.Boolean,
        completionProvider = booleanCompletionProvider(),
        sectionValueParser = BooleanValueParser(),
      ),
      SectionMetadata(
        sectionName = PYTHON_CODE_GENERATOR_RULE_NAMES,
        sectionType = SectionType.List.String,
      ),
      SectionMetadata(
        sectionName = USE_QUERY_SYNC,
        sectionType = SectionType.Scalar.Boolean,
        completionProvider = booleanCompletionProvider(),
        sectionValueParser = BooleanValueParser(),
      ),
      SectionMetadata(
        sectionName = WORKSPACE_TYPE,
        sectionType = SectionType.Scalar.String,
        completionProvider =
          SimpleCompletionProvider(
            listOf("java", "python", "dart", "android", "javascript"),
          ),
      ),
      SectionMetadata(
        sectionName = ADDITIONAL_LANGUAGES,
        sectionType = SectionType.List.String,
        completionProvider =
          SimpleCompletionProvider(
            listOf(
              "android",
              "dart",
              "java",
              "javascript",
              "kotlin",
              "python",
              "typescript",
              "go",
              "c",
            ),
          ),
      ),
      SectionMetadata(
        sectionName = JAVA_LANGUAGE_LEVEL,
        sectionType = SectionType.Scalar.String,
      ),
      SectionMetadata(
        sectionName = EXCLUDE_LIBRARY,
        sectionType = SectionType.List.String,
      ),
      SectionMetadata(
        sectionName = ANDROID_SDK_PLATFORM,
        sectionType = SectionType.Scalar.String,
      ),
      SectionMetadata(
        sectionName = GENERATED_ANDROID_RESOURCE_DIRECTORIES,
        sectionType = SectionType.List.String,
      ),
      SectionMetadata(
        sectionName = TS_CONFIG_RULES,
        sectionType = SectionType.List.String,
      ),
      SectionMetadata(
        sectionName = IMPORT_IJARS,
        sectionType = SectionType.Scalar.Boolean,
        completionProvider = booleanCompletionProvider(),
        sectionValueParser = BooleanValueParser(),
      ),
      SectionMetadata(
        sectionName = DEBUG_FLAGS,
        sectionType = SectionType.List.String,
        completionProvider = FlagCompletionProvider("debug"),
        sectionValueParser = FlagValueParser(),
      ),
    ).associateBy { it.sectionName }

  private fun booleanCompletionProvider() = SimpleCompletionProvider(listOf("true", "false"))
}
