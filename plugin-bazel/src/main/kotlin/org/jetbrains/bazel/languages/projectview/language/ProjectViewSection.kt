package org.jetbrains.bazel.languages.projectview.language

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.bazelrc.flags.Flag
import org.jetbrains.bazel.languages.projectview.completion.FlagCompletionProvider
import org.jetbrains.bazel.languages.projectview.completion.SimpleCompletionProvider
import org.jetbrains.bazel.projectview.model.supportedSections
import java.nio.file.Path

const val ALLOW_MANUAL_TARGETS_SYNC = "allow_manual_targets_sync"
typealias ALLOW_MANUAL_TARGETS_SYNC_TYPE = Boolean

const val ANDROID_MIN_SDK = "android_min_sdk"
typealias ANDROID_MIN_SDK_TYPE = Int

const val BAZEL_BINARY = "bazel_binary"
typealias BAZEL_BINARY_TYPE = Path

const val BUILD_FLAGS = "build_flags"
typealias BUILD_FLAGS_TYPE = Flag

const val TEST_FLAGS = "test_flags"
typealias TEST_FLAGS_TYPE = Flag

const val SYNC_FLAGS = "sync_flags"
typealias SYNC_FLAGS_TYPE = Flag

const val DEBUG_FLAGS = "debug_flags"
typealias DEBUG_FLAGS_TYPE = Flag

const val DERIVE_TARGETS_FROM_DIRECTORIES = "derive_targets_from_directories"
typealias DERIVE_TARGETS_FROM_DIRECTORIES_TYPE = Boolean

const val DIRECTORIES = "directories"
typealias DIRECTORIES_INNER_TYPE = Path
typealias DIRECTORIES_TYPE = ProjectViewSection.Value<DIRECTORIES_INNER_TYPE>

const val ENABLE_NATIVE_ANDROID_RULES = "enable_native_android_rules"
typealias ENABLE_NATIVE_ANDROID_RULES_TYPE = Boolean

const val ENABLED_RULES = "enabled_rules"
typealias ENABLED_RULES_TYPE = String

const val EXPERIMENTAL_ADD_TRANSITIVE_COMPILE_TIME_JARS = "experimental_add_transitive_compile_time_jars"
typealias EXPERIMENTAL_ADD_TRANSITIVE_COMPILE_TIME_JARS_TYPE = Boolean

const val EXPERIMENTAL_NO_PRUNE_TRANSITIVE_COMPILE_TIME_JARS_PATTERNS = "experimental_no_prune_transitive_compile_time_jars_patterns"
typealias EXPERIMENTAL_NO_PRUNE_TRANSITIVE_COMPILE_TIME_JARS_PATTERNS_TYPE = String

const val EXPERIMENTAL_TRANSITIVE_COMPILE_TIME_JARS_TARGET_KINDS = "experimental_transitive_compile_time_jars_target_kinds"
typealias EXPERIMENTAL_TRANSITIVE_COMPILE_TIME_JARS_TARGET_KINDS_TYPE = String

const val EXPERIMENTAL_PRIORITIZE_LIBRARIES_OVER_MODULES_TARGET_KINDS = "experimental_prioritize_libraries_over_modules_target_kinds"
typealias EXPERIMENTAL_PRIORITIZE_LIBRARIES_OVER_MODULES_TARGET_KINDS_TYPE = String

const val IDE_JAVA_HOME_OVERRIDE = "ide_java_home_override"
typealias IDE_JAVA_HOME_OVERRIDE_TYPE = Path

const val IMPORT_DEPTH = "import_depth"
typealias IMPORT_DEPTH_TYPE = Int

const val SHARD_APPROACH = "shard_approach"
typealias SHARD_APPROACH_TYPE = String

const val SHARD_SYNC = "shard_sync"
typealias SHARD_SYNC_TYPE = Boolean

const val TARGET_SHARD_SIZE = "target_shard_size"
typealias TARGET_SHARD_SIZE_TYPE = Int

const val TARGETS = "targets"
typealias TARGETS_INNER_TYPE = Label
typealias TARGETS_TYPE = ProjectViewSection.Value<TARGETS_INNER_TYPE>

const val IMPORT_RUN_CONFIGURATIONS = "import_run_configurations"
typealias IMPORT_RUN_CONFIGURATIONS_TYPE = Path

const val TEST_SOURCES = "test_sources"

const val GAZELLE_TARGET = "gazelle_target"
typealias GAZELLE_TARGET_TYPE = String

const val INDEX_ALL_FILES_IN_DIRECTORIES = "index_all_files_in_directories"
typealias INDEX_ALL_FILES_IN_DIRECTORIES_TYPE = Boolean

const val PYTHON_CODE_GENERATOR_RULE_NAMES = "python_code_generator_rule_names"
typealias PYTHON_CODE_GENERATOR_RULE_NAMES_TYPE = String

const val IMPORT_IJARS = "import_ijars"
typealias IMPORT_IJARS_TYPE = Boolean

// Not supported by our plugin.
const val USE_QUERY_SYNC = "use_query_sync"
const val WORKSPACE_TYPE = "workspace_type"
const val ADDITIONAL_LANGUAGES = "additional_languages"
const val JAVA_LANGUAGE_LEVEL = "java_language_level"
const val EXCLUDE_LIBRARY = "exclude_library"
const val ANDROID_SDK_PLATFORM = "android_sdk_platform"
const val GENERATED_ANDROID_RESOURCE_DIRECTORIES = "generated_android_resource_directories"
const val TS_CONFIG_RULES = "ts_config_rules"

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

  class IdentityValueParser : SectionValueParser<String> {
    override fun parse(value: String): String = value
  }

  class BooleanValueParser : SectionValueParser<Boolean> {
    override fun parse(value: String): Boolean? = value.toBooleanStrictOrNull()
  }

  class IntegerValueParser : SectionValueParser<Int> {
    override fun parse(value: String): Int? = value.toIntOrNull()
  }

  class FlagValueParser : SectionValueParser<Flag> {
    override fun parse(value: String): Flag? = Flag.byName(value)
  }

  class PathValueParser : SectionValueParser<Path> {
    override fun parse(value: String): Path? = Path.of(value)
  }

  class LabelValueParser : SectionValueParser<Label> {
    override fun parse(value: String): Label = Label.parse(value)
  }

  sealed interface Value<T> {
    class Included<T>(val value: T) : Value<T>

    class Excluded<T>(val value: T) : Value<T>
  }

  class IncludedExcludedParser<T>(private val actualParser: SectionValueParser<T>) : SectionValueParser<Value<T>> {
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
        completionProvider = null,
        sectionValueParser = IncludedExcludedParser(LabelValueParser()),
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
