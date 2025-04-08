package org.jetbrains.bazel.languages.projectview.language

object ProjectViewSection {
  sealed interface Parser {
    fun parseItem(text: String): ParsingResult =
      if (text.isEmpty()) {
        ParsingResult.Error("Empty item!")
      } else {
        parseNonEmptyItem(text)
      }

    fun parseNonEmptyItem(text: String): ParsingResult = ParsingResult.OK

    sealed interface Scalar : Parser {
      data object NonNegativeInteger : Scalar {
        override fun parseNonEmptyItem(text: kotlin.String): ParsingResult =
          parseWithPredicate(
            text == "0" || (text[0] != '0' && text.all { it in '0'..'9' }),
            "Invalid number!",
          )
      }

      data object Boolean : Scalar {
        override fun parseNonEmptyItem(text: kotlin.String): ParsingResult {
          val booleanValues = setOf("true", "false")
          return parseWithPredicate(
            text in booleanValues,
            "Invalid boolean",
          )
        }
      }

      data object String : Scalar
    }

    data class List(val itemParser: Scalar) : Parser {
      override fun parseItem(text: String) = itemParser.parseItem(text)

      companion object {
        val stringListParser = List(Scalar.String)
      }
    }

    sealed interface ParsingResult {
      object OK : ParsingResult

      data class Error(val message: String) : ParsingResult
    }
  }

  /** A map of registered section keywords. */
  val KEYWORD_MAP: Map<ProjectViewSyntaxKey, Parser> =
    mapOf(
      "allow_manual_targets_sync" to Parser.Scalar.Boolean,
      "android_min_sdk" to Parser.Scalar.NonNegativeInteger,
      "bazel_binary" to Parser.Scalar.String,
      "build_flags" to Parser.List.stringListParser,
      "derive_targets_from_directories" to Parser.Scalar.Boolean,
      "directories" to Parser.List.stringListParser,
      "enable_native_android_rules" to Parser.Scalar.Boolean,
      "enabled_rules" to Parser.List.stringListParser,
      "experimental_add_transitive_compile_time_jars" to Parser.Scalar.Boolean,
      "experimental_no_prune_transitive_compile_time_jars_patterns" to Parser.List.stringListParser,
      "experimental_transitive_compile_time_jars_target_kinds" to Parser.List.stringListParser,
      "ide_java_home_override" to Parser.Scalar.String,
      "import_depth" to Parser.Scalar.NonNegativeInteger,
      "shard_approach" to Parser.Scalar.String,
      "shard_sync" to Parser.Scalar.Boolean,
      "sync_flags" to Parser.List.stringListParser,
      "target_shard_size" to Parser.Scalar.NonNegativeInteger,
      "targets" to Parser.List.stringListParser,
      "import_run_configurations" to Parser.List.stringListParser,
    )

  private fun parseWithPredicate(p: Boolean, errorMessage: String): Parser.ParsingResult =
    if (p) {
      Parser.ParsingResult.OK
    } else {
      Parser.ParsingResult.Error(errorMessage)
    }
}
