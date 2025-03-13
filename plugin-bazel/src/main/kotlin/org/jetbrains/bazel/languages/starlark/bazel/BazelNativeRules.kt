package org.jetbrains.bazel.languages.starlark.bazel

data class BazelNativeRuleArgument(
  val name: String,
  val default: String,
  val required: Boolean,
  val docString: String = "",
)

data class BazelNativeRule(
  val name: String,
  val docsLink: String,
  val arguments: Set<BazelNativeRuleArgument>,
  val docString: String = "",
)

object BazelNativeRules {
  private fun addNativeRule(
    name: String,
    docsLink: String,
    arguments: Set<BazelNativeRuleArgument>,
  ): Pair<String, BazelNativeRule> = Pair(name, BazelNativeRule(name, docsLink, arguments))

  const val BAZEL_TRUE = "True"
  const val BAZEL_FALSE = "False"
  const val BAZEL_NONE = "None"
  const val BAZEL_EMPTY_LIST = "[]"
  const val BAZEL_EMPTY_STRING = "\"\""
  const val BAZEL_STRUCT = "{}"

  fun getRuleArguments(ruleString: String): Set<BazelNativeRuleArgument> {
    val rule = NATIVE_RULES_MAP[ruleString]
    if (rule == null) {
      return setOf()
    }

    val args = rule.arguments union COMMON_ARGUMENTS

    if (ruleString.endsWith("test")) {
      return args union TEST_COMMON_ARGUMENTS
    }

    if (ruleString.endsWith("binary")) {
      return args union BINARY_COMMON_RULES
    }

    return args
  }

  // Apply to all native build rules.
  val COMMON_ARGUMENTS =
    setOf(
      BazelNativeRuleArgument(
        "compatible_with",
        BAZEL_EMPTY_LIST,
        false,
      ),
      BazelNativeRuleArgument(
        "deprecation",
        BAZEL_NONE,
        false,
      ),
      BazelNativeRuleArgument(
        "distribs",
        BAZEL_EMPTY_LIST,
        false,
      ),
      BazelNativeRuleArgument(
        "exec_compatible_with",
        BAZEL_EMPTY_LIST,
        false,
      ),
      BazelNativeRuleArgument(
        "exec_properties",
        BAZEL_STRUCT,
        false,
      ),
      BazelNativeRuleArgument(
        "features",
        BAZEL_EMPTY_LIST,
        false,
      ),
      BazelNativeRuleArgument(
        "restricted_to",
        BAZEL_EMPTY_LIST,
        false,
      ),
      BazelNativeRuleArgument(
        "tags",
        BAZEL_EMPTY_LIST,
        false,
      ),
      BazelNativeRuleArgument(
        "target_compatible_with",
        BAZEL_EMPTY_LIST,
        false,
      ),
      BazelNativeRuleArgument(
        "testonly",
        BAZEL_FALSE,
        false,
      ),
      BazelNativeRuleArgument(
        "toolchains",
        BAZEL_EMPTY_LIST,
        false,
      ),
      BazelNativeRuleArgument(
        "visibility",
        BAZEL_EMPTY_LIST,
        false,
      ),
    )

  // Apply to all *_test rules.
  val TEST_COMMON_ARGUMENTS =
    setOf(
      BazelNativeRuleArgument(
        "args",
        BAZEL_EMPTY_LIST,
        false,
      ),
      BazelNativeRuleArgument(
        "env",
        BAZEL_STRUCT,
        false,
      ),
      BazelNativeRuleArgument(
        "env_inherit",
        BAZEL_EMPTY_LIST,
        false,
      ),
      BazelNativeRuleArgument(
        "size",
        BAZEL_EMPTY_STRING,
        false,
      ),
      BazelNativeRuleArgument(
        "timeout",
        BAZEL_EMPTY_STRING,
        false,
      ),
      BazelNativeRuleArgument(
        "flaky",
        BAZEL_FALSE,
        false,
      ),
      BazelNativeRuleArgument(
        "shard_count",
        "-1",
        false,
      ),
      BazelNativeRuleArgument(
        "local",
        BAZEL_FALSE,
        false,
      ),
    )

  // Apply to all *_binary rules.
  val BINARY_COMMON_RULES =
    setOf(
      BazelNativeRuleArgument(
        "args",
        BAZEL_EMPTY_LIST,
        false,
      ),
      BazelNativeRuleArgument(
        "env",
        BAZEL_STRUCT,
        false,
      ),
      BazelNativeRuleArgument(
        "output_licenses",
        BAZEL_EMPTY_LIST,
        false,
      ),
    )

  private fun ruleSetsToMap(rules: List<Set<BazelNativeRule>>): Map<String, BazelNativeRule> =
    rules.flatten().associateBy(
      keySelector = { it.name },
      valueTransform = { it },
    )

  val NATIVE_RULES_MAP = ruleSetsToMap(RULES)

  val ruleNames = NATIVE_RULES_MAP.keys

  fun getRuleByName(name: String): BazelNativeRule? = NATIVE_RULES_MAP[name]
}
