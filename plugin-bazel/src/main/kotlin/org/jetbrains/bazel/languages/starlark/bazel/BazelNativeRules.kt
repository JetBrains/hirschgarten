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
  ): Pair<String, BazelNativeRule> = Pair(name, BazelNativeRule(name, docsLink, arguments))

  const val BAZEL_TRUE = "True"
  const val BAZEL_FALSE = "False"
  const val BAZEL_NONE = "None"
  const val BAZEL_EMPTY_LIST = "[]"
  const val BAZEL_EMPTY_STRING = "\"\""

  val NATIVE_RULES_MAP =
    mapOf(
      // Java
      addNativeRule(
        "java_binary",
        "https://bazel.build/reference/be/java#java_binary",
        setOf(
          BazelNativeRuleArgument("name", BAZEL_EMPTY_STRING, true),
          BazelNativeRuleArgument("deps", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("srcs", BAZEL_EMPTY_LIST, true),
          BazelNativeRuleArgument("resources", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("add_exports", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("add_opens", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("classpath_resources", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("deploy_env", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("deploy_manifest_lines", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("javacopts", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("jvm_flags", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("launcher", BAZEL_NONE, false),
          BazelNativeRuleArgument("main_class", BAZEL_EMPTY_STRING, false),
          BazelNativeRuleArgument("neverlink", BAZEL_FALSE, false),
          BazelNativeRuleArgument("plugins", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("resource_strip_prefix", BAZEL_EMPTY_STRING, false),
          BazelNativeRuleArgument("runtime_deps", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("stamp", "-1", false),
          BazelNativeRuleArgument("use_launcher", BAZEL_TRUE, false),
          BazelNativeRuleArgument("use_testrunner", BAZEL_FALSE, false),
        ),
      ),
      addNativeRule(
        "java_import",
        "https://bazel.build/reference/be/java#java_import",
        setOf(
          BazelNativeRuleArgument("name", BAZEL_EMPTY_STRING, true),
          BazelNativeRuleArgument("deps", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("srcs", BAZEL_EMPTY_LIST, true),
          BazelNativeRuleArgument("add_exports", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("add_opens", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("constraints", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("exports", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("jars", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("neverlink", BAZEL_FALSE, false),
          BazelNativeRuleArgument("proguard_specs", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("runtime_deps", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("srcjar", BAZEL_NONE, false),
        ),
      ),
      addNativeRule(
        "java_library",
        "https://bazel.build/reference/be/java#java_library",
        setOf(
          BazelNativeRuleArgument("name", BAZEL_EMPTY_STRING, true),
          BazelNativeRuleArgument("deps", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("srcs", BAZEL_EMPTY_LIST, true),
          BazelNativeRuleArgument("data", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("resources", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("add_exports", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("add_opens", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("bootclasspath", BAZEL_NONE, false),
          BazelNativeRuleArgument("exported_plugins", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("exports", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("javacopts", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("neverlink", BAZEL_FALSE, false),
          BazelNativeRuleArgument("plugins", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("proguard_specs", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("resource_strip_prefix", BAZEL_EMPTY_STRING, false),
          BazelNativeRuleArgument("runtime_deps", BAZEL_EMPTY_LIST, false),
        ),
      ),
      addNativeRule(
        "java_test",
        "https://bazel.build/reference/be/java#java_test",
        setOf(
          BazelNativeRuleArgument("name", BAZEL_EMPTY_STRING, true),
          BazelNativeRuleArgument("deps", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("srcs", BAZEL_EMPTY_LIST, true),
          BazelNativeRuleArgument("data", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("resources", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("add_exports", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("add_opens", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("create_executable", BAZEL_TRUE, false),
          BazelNativeRuleArgument("deploy_manifest_lines", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("javacopts", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("jvm_flags", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("launcher", BAZEL_NONE, false),
          BazelNativeRuleArgument("launcher", BAZEL_NONE, false),
          BazelNativeRuleArgument("main_class", BAZEL_EMPTY_STRING, false),
          BazelNativeRuleArgument("neverlink", BAZEL_FALSE, false),
          BazelNativeRuleArgument("plugins", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("resource_strip_prefix", BAZEL_FALSE, false),
          BazelNativeRuleArgument("runtime_deps", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("stamp", "0", false),
          BazelNativeRuleArgument("test_class", BAZEL_EMPTY_STRING, false),
          BazelNativeRuleArgument("use_launcher", BAZEL_TRUE, false),
          BazelNativeRuleArgument("use_testrunner", BAZEL_TRUE, false),
        ),
      ),
      addNativeRule(
        "java_package_configuration",
        "https://bazel.build/reference/be/java#java_package_configuration",
        setOf(
          BazelNativeRuleArgument("name", BAZEL_EMPTY_STRING, true),
          BazelNativeRuleArgument("data", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("javacopts", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("output_licenses", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("packages", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("system", BAZEL_NONE, false),
        ),
      ),
      addNativeRule(
        "java_plugin",
        "https://bazel.build/reference/be/java#java_plugin",
        setOf(
          BazelNativeRuleArgument("name", BAZEL_EMPTY_STRING, true),
          BazelNativeRuleArgument("deps", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("srcs", BAZEL_EMPTY_LIST, true),
          BazelNativeRuleArgument("data", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("resources", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("add_exports", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("add_opens", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("generates_api", BAZEL_FALSE, false),
          BazelNativeRuleArgument("javacopts", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("neverlink", BAZEL_FALSE, false),
          BazelNativeRuleArgument("output_licenses", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("plugins", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("processor_class", BAZEL_EMPTY_STRING, false),
          BazelNativeRuleArgument("proguard_specs", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("resource_strip_prefix", BAZEL_EMPTY_STRING, false),
        ),
      ),
      addNativeRule(
        "java_runtime",
        "https://bazel.build/reference/be/java#java_runtime",
        setOf(
          BazelNativeRuleArgument("name", BAZEL_EMPTY_STRING, true),
          BazelNativeRuleArgument("deps", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("default_cds", BAZEL_NONE, false),
          BazelNativeRuleArgument("hermetic_srcs", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("hermetic_static_libs", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("java", BAZEL_NONE, false),
          BazelNativeRuleArgument("java_home", BAZEL_EMPTY_STRING, false),
          BazelNativeRuleArgument("lib_ct_sym", BAZEL_NONE, false),
          BazelNativeRuleArgument("lib_modules", BAZEL_NONE, false),
          BazelNativeRuleArgument("output_licenses", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("version", "0", false),
        ),
      ),
      addNativeRule(
        "java_single_jar",
        "https://bazel.build/reference/be/java#java_single_jar",
        setOf(
          BazelNativeRuleArgument("name", BAZEL_EMPTY_STRING, true),
          BazelNativeRuleArgument("deps", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("compress", "preserve", false),
          BazelNativeRuleArgument("deploy_env", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("deploy_manifest_lines", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("exclude_build_data", BAZEL_TRUE, false),
          BazelNativeRuleArgument("multi_release", BAZEL_TRUE, false),
        ),
      ),
      addNativeRule(
        "java_toolchain",
        "https://bazel.build/reference/be/java#java_toolchain",
        setOf(
          BazelNativeRuleArgument("name", BAZEL_EMPTY_STRING, true),
          BazelNativeRuleArgument("android_lint_data", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("android_lint_jvm_opts", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("android_lint_opts", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("android_lint_package_configuration", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("android_lint_runner", BAZEL_NONE, false),
          BazelNativeRuleArgument("bootclasspath", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("deps_checker", BAZEL_NONE, false),
          BazelNativeRuleArgument("forcibly_disable_header_compilation", BAZEL_FALSE, false),
          BazelNativeRuleArgument("genclass", BAZEL_NONE, false),
          BazelNativeRuleArgument("header_compiler", BAZEL_NONE, false),
          BazelNativeRuleArgument("header_compiler_builtin_processors", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("header_compiler_direct", BAZEL_NONE, false),
          BazelNativeRuleArgument("ijar", BAZEL_NONE, false),
          BazelNativeRuleArgument("jacocorunner", BAZEL_NONE, false),
          BazelNativeRuleArgument("java_runtime", BAZEL_NONE, false),
          BazelNativeRuleArgument("javabuilder", BAZEL_NONE, false),
          BazelNativeRuleArgument("javabuilder_data", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("javabuilder_jvm_opts", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("javac_supports_multiplex_workers", BAZEL_TRUE, false),
          BazelNativeRuleArgument("javac_supports_worker_cancellation", BAZEL_TRUE, false),
          BazelNativeRuleArgument("javac_supports_worker_multiplex_sandboxing", BAZEL_FALSE, false),
          BazelNativeRuleArgument("javac_supports_workers", BAZEL_TRUE, false),
          BazelNativeRuleArgument("javacopts", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("jspecify_implicit_deps", BAZEL_NONE, false),
          BazelNativeRuleArgument("jspecify_javacopts", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("jspecify_packages", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("jspecify_processor", BAZEL_NONE, false),
          BazelNativeRuleArgument("jspecify_processor_class", BAZEL_EMPTY_STRING, false),
          BazelNativeRuleArgument("jspecify_stubs", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("jvm_opts", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("oneversion", BAZEL_NONE, false),
          BazelNativeRuleArgument("oneversion_allowlist", BAZEL_NONE, false),
          BazelNativeRuleArgument("oneversion_allowlist_for_tests", BAZEL_NONE, false),
          BazelNativeRuleArgument("package_configuration", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("proguard_allowlister", "@bazel_tools//tools/jdk:proguard_whitelister", false),
          BazelNativeRuleArgument("reduced_classpath_incompatible_processors", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("singlejar", BAZEL_NONE, false),
          BazelNativeRuleArgument("source_version", BAZEL_EMPTY_STRING, false),
          BazelNativeRuleArgument("target_version", BAZEL_EMPTY_STRING, false),
          BazelNativeRuleArgument("timezone_data", BAZEL_NONE, false),
          BazelNativeRuleArgument("tools", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("turbine_data", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("turbine_jvm_opts", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("xlint", BAZEL_EMPTY_LIST, false),
        ),
      ),
      // C/C++
    )

  fun getRuleNames(): Set<String> = NATIVE_RULES_MAP.keys
}
