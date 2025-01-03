package org.jetbrains.bazel.languages.starlark.bazel

class BazelNativeRuleArgument(
  val name: String,
  val default: String,
  val required: Boolean,
)

class BazelNativeRule(
  val name: String,
  val docsLink: String,
  val arguments: Set<BazelNativeRuleArgument>,
)

object BazelNativeRules {
  // Apply to all native build rules. TODO converto to BazelNativeRuleArgument
  val COMMON_ARGUMENTS =
    setOf(
      "compatible_with",
      "deprecation",
      "distribs",
      "exec_compatible_with",
      "exec_properties",
      "features",
      "restricted_to",
      "tags",
      "target_compatible_with",
      "testonly",
      "toolchains",
      "visibility",
    )

  // Apply to all *_test rules. TODO converto to BazelNativeRuleArgument
  val TEST_COMMON_ARGUMENTS =
    setOf(
      "args",
      "env",
      "env_inherit",
      "size",
      "timeout",
      "flaky",
      "shard_count",
      "local",
    )

  // Apply to all *_binary rules. TODO converto to BazelNativeRuleArgument
  val BINARY_COMMON_RULES =
    setOf(
      "args",
      "env",
      "output_licenses",
    )

  private fun addNativeRule(
    name: String,
    docsLink: String,
    arguments: Set<BazelNativeRuleArgument>,
  ): Pair<String, BazelNativeRule> {
    return Pair(name, BazelNativeRule(name, docsLink, arguments))
  }

  val NATIVE_RULES_MAP =
    mapOf(
      // Java
      addNativeRule(
        "java_binary",
        "https://bazel.build/reference/be/java#java_binary",
        setOf(
          BazelNativeRuleArgument("name", "\"\"", true),
          BazelNativeRuleArgument("deps", "[]", false),
          BazelNativeRuleArgument("srcs", "[]", true),
          BazelNativeRuleArgument("resources", "[]", false),
          BazelNativeRuleArgument("add_exports", "[]", false),
          BazelNativeRuleArgument("add_opens", "[]", false),
          BazelNativeRuleArgument("classpath_resources", "[]", false),
          BazelNativeRuleArgument("deploy_env", "[]", false),
          BazelNativeRuleArgument("deploy_manifest_lines", "[]", false),
          BazelNativeRuleArgument("javacopts", "[]", false),
          BazelNativeRuleArgument("jvm_flags", "[]", false),
          BazelNativeRuleArgument("launcher", "None", false),
          BazelNativeRuleArgument("main_class", "\"\"", false),
          BazelNativeRuleArgument("neverlink", "False", false),
          BazelNativeRuleArgument("plugins", "[]", false),
          BazelNativeRuleArgument("resource_strip_prefix", "\"\"", false),
          BazelNativeRuleArgument("runtime_deps", "[]", false),
          BazelNativeRuleArgument("stamp", "-1", false),
          BazelNativeRuleArgument("use_launcher", "True", false),
          BazelNativeRuleArgument("use_testrunner", "False", false),
        )
      ),
      addNativeRule(
        "java_import",
        "https://bazel.build/reference/be/java#java_import",
        setOf(
          BazelNativeRuleArgument("name", "\"\"", true),
          BazelNativeRuleArgument("deps", "[]", false),
          BazelNativeRuleArgument("srcs", "[]", true),
          BazelNativeRuleArgument("add_exports", "[]", false),
          BazelNativeRuleArgument("add_opens", "[]", false),
          BazelNativeRuleArgument("constraints", "[]", false),
          BazelNativeRuleArgument("exports", "[]", false),
          BazelNativeRuleArgument("jars", "[]", false),
          BazelNativeRuleArgument("neverlink", "False", false),
          BazelNativeRuleArgument("proguard_specs", "[]", false),
          BazelNativeRuleArgument("runtime_deps", "[]", false),
          BazelNativeRuleArgument("srcjar", "None", false),
        )
      ),
      addNativeRule(
        "java_library",
        "https://bazel.build/reference/be/java#java_library",
        setOf(
          BazelNativeRuleArgument("name", "\"\"", true),
          BazelNativeRuleArgument("deps", "[]", false),
          BazelNativeRuleArgument("srcs", "[]", true),
          BazelNativeRuleArgument("data", "[]", false),
          BazelNativeRuleArgument("resources", "[]", false),
          BazelNativeRuleArgument("add_exports", "[]", false),
          BazelNativeRuleArgument("add_opens", "[]", false),
          BazelNativeRuleArgument("bootclasspath", "None", false),
          BazelNativeRuleArgument("exported_plugins", "[]", false),
          BazelNativeRuleArgument("exports", "[]", false),
          BazelNativeRuleArgument("javacopts", "[]", false),
          BazelNativeRuleArgument("neverlink", "False", false),
          BazelNativeRuleArgument("plugins", "[]", false),
          BazelNativeRuleArgument("proguard_specs", "[]", false),
          BazelNativeRuleArgument("resource_strip_prefix", "\"\"", false),
          BazelNativeRuleArgument("runtime_deps", "[]", false),
        )
      )
    )

  fun getRuleNames(): Set<String> = NATIVE_RULES_MAP.keys
}
