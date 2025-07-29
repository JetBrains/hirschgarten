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

  /*
   * The map below should contain all syntax-wise valid section names.
   * However, the ones that are actually supported by the plugin are defined in the ProjectView data class.
   * This way we can recognize the keyword and annotate it accordingly.
   * */
  val KEYWORD_MAP: Map<ProjectViewSyntaxKey, SectionMetadata> =
    listOf(
      SectionMetadata(
        sectionName = "allow_manual_targets_sync",
        sectionType = SectionType.Scalar.Boolean,
        completionProvider = booleanCompletionProvider(),
        sectionValueParser = BooleanValueParser(),
      ),
      SectionMetadata(
        sectionName = "android_min_sdk",
        sectionType = SectionType.Scalar.Integer,
        completionProvider = null,
        sectionValueParser = IntegerValueParser(),
      ),
      SectionMetadata(
        sectionName = "bazel_binary",
        sectionType = SectionType.Scalar.String,
        completionProvider = null,
        sectionValueParser = PathValueParser(),
      ),
      SectionMetadata(
        sectionName = "build_flags",
        sectionType = SectionType.List.String,
        completionProvider = FlagCompletionProvider("build"),
        sectionValueParser = FlagValueParser(),
      ),
      SectionMetadata(
        sectionName = "test_flags",
        sectionType = SectionType.List.String,
        completionProvider = FlagCompletionProvider("test"),
        sectionValueParser = FlagValueParser(),
      ),
      SectionMetadata(
        sectionName = "derive_targets_from_directories",
        sectionType = SectionType.Scalar.Boolean,
        completionProvider = booleanCompletionProvider(),
        sectionValueParser = BooleanValueParser(),
      ),
      SectionMetadata(
        sectionName = "directories",
        sectionType = SectionType.List.String,
        completionProvider = null,
        sectionValueParser = IncludedExcludedParser(PathValueParser()),
      ),
      SectionMetadata(
        sectionName = "enable_native_android_rules",
        sectionType = SectionType.Scalar.Boolean,
        completionProvider = booleanCompletionProvider(),
        sectionValueParser = BooleanValueParser(),
      ),
      SectionMetadata(
        sectionName = "enabled_rules",
        sectionType = SectionType.List.String,
        completionProvider = null,
        sectionValueParser = IdentityValueParser(),
      ),
      SectionMetadata(
        sectionName = "experimental_add_transitive_compile_time_jars",
        sectionType = SectionType.Scalar.Boolean,
        completionProvider = booleanCompletionProvider(),
        sectionValueParser = BooleanValueParser(),
      ),
      SectionMetadata(
        sectionName = "experimental_no_prune_transitive_compile_time_jars_patterns",
        sectionType = SectionType.List.String,
      ),
      SectionMetadata(
        sectionName = "experimental_transitive_compile_time_jars_target_kinds",
        sectionType = SectionType.List.String,
      ),
      SectionMetadata(
        sectionName = "experimental_prioritize_libraries_over_modules_target_kinds",
        sectionType = SectionType.List.String,
      ),
      SectionMetadata(
        sectionName = "ide_java_home_override",
        sectionType = SectionType.Scalar.String,
        completionProvider = null,
        sectionValueParser = PathValueParser(),
      ),
      SectionMetadata(
        sectionName = "import_depth",
        sectionType = SectionType.Scalar.Integer,
        completionProvider = null,
        sectionValueParser = IntegerValueParser(),
      ),
      SectionMetadata(
        sectionName = "shard_approach",
        sectionType = SectionType.Scalar.String,
      ),
      SectionMetadata(
        sectionName = "shard_sync",
        sectionType = SectionType.Scalar.Boolean,
        completionProvider = booleanCompletionProvider(),
        sectionValueParser = BooleanValueParser(),
      ),
      SectionMetadata(
        sectionName = "sync_flags",
        sectionType = SectionType.List.String,
        completionProvider = FlagCompletionProvider("sync"),
        sectionValueParser = FlagValueParser(),
      ),
      SectionMetadata(
        sectionName = "target_shard_size",
        sectionType = SectionType.Scalar.Integer,
        completionProvider = null,
        sectionValueParser = IntegerValueParser(),
      ),
      SectionMetadata(
        sectionName = "targets",
        sectionType = SectionType.List.String,
      ),
      SectionMetadata(
        sectionName = "import_run_configurations",
        sectionType = SectionType.List.String,
        completionProvider = null,
        sectionValueParser = PathValueParser(),
      ),
      SectionMetadata(
        sectionName = "test_sources",
        sectionType = SectionType.List.String, // used by Google's plugin
      ),
      SectionMetadata(
        sectionName = "gazelle_target",
        sectionType = SectionType.Scalar.String,
      ),
      SectionMetadata(
        sectionName = "index_all_files_in_directories",
        sectionType = SectionType.Scalar.Boolean,
        completionProvider = booleanCompletionProvider(),
        sectionValueParser = BooleanValueParser(),
      ),
      SectionMetadata(
        sectionName = "python_code_generator_rule_names",
        sectionType = SectionType.List.String,
      ),
      SectionMetadata(
        sectionName = "use_query_sync",
        sectionType = SectionType.Scalar.Boolean,
        completionProvider = booleanCompletionProvider(),
        sectionValueParser = BooleanValueParser(),
      ),
      SectionMetadata(
        sectionName = "workspace_type",
        sectionType = SectionType.Scalar.String,
        completionProvider =
          SimpleCompletionProvider(
            listOf("java", "python", "dart", "android", "javascript"),
          ),
      ),
      SectionMetadata(
        sectionName = "additional_languages",
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
        sectionName = "java_language_level",
        sectionType = SectionType.Scalar.String,
      ),
      SectionMetadata(
        sectionName = "exclude_library",
        sectionType = SectionType.List.String,
      ),
      SectionMetadata(
        sectionName = "android_sdk_platform",
        sectionType = SectionType.Scalar.String,
      ),
      SectionMetadata(
        sectionName = "generated_android_resource_directories",
        sectionType = SectionType.List.String,
      ),
      SectionMetadata(
        sectionName = "ts_config_rules",
        sectionType = SectionType.List.String,
      ),
      SectionMetadata(
        sectionName = "import_ijars",
        sectionType = SectionType.Scalar.Boolean,
        completionProvider = booleanCompletionProvider(),
        sectionValueParser = BooleanValueParser(),
      ),
      SectionMetadata(
        sectionName = "debug_flags",
        sectionType = SectionType.List.String,
        completionProvider = FlagCompletionProvider("debug"),
        sectionValueParser = FlagValueParser(),
      ),
    ).associateBy { it.sectionName }

  private fun booleanCompletionProvider() = SimpleCompletionProvider(listOf("true", "false"))
}
