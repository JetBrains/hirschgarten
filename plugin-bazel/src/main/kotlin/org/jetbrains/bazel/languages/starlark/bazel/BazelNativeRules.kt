package org.jetbrains.bazel.languages.starlark.bazel

data class BazelNativeRuleArgument(
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

  val NATIVE_RULES_MAP =
    mapOf(
      // Java.
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
      addNativeRule(
        "cc_binary",
        "https://bazel.build/reference/be/c-cpp#cc_binary",
        setOf(
          BazelNativeRuleArgument("name", BAZEL_EMPTY_STRING, true),
          BazelNativeRuleArgument("deps", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("srcs", BAZEL_EMPTY_LIST, true),
          BazelNativeRuleArgument("data", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("additional_linker_inputs", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("conlyopts", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("copts", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("cxxopts", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("defines", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("dynamic_deps", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("includes", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("link_extra_lib", BAZEL_EMPTY_STRING, false),
          BazelNativeRuleArgument("linkopts", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("linkshared", BAZEL_FALSE, false),
          BazelNativeRuleArgument("linkstatic", BAZEL_TRUE, false),
          BazelNativeRuleArgument("local_defines", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("malloc", BAZEL_EMPTY_STRING, false),
          BazelNativeRuleArgument("module_interfaces", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("nocopts", BAZEL_EMPTY_STRING, false),
          BazelNativeRuleArgument("reexport_deps", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("stamp", "-1", false),
          BazelNativeRuleArgument("win_def_file", BAZEL_NONE, false),
        ),
      ),
      addNativeRule(
        "cc_import",
        "https://bazel.build/reference/be/c-cpp#cc_import",
        setOf(
          BazelNativeRuleArgument("name", BAZEL_EMPTY_STRING, true),
          BazelNativeRuleArgument("deps", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("hdrs", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("alwayslink", BAZEL_FALSE, false),
          BazelNativeRuleArgument("includes", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("interface_library", BAZEL_NONE, false),
          BazelNativeRuleArgument("linkopts", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("objects", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("pic_objects", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("pic_static_library", BAZEL_NONE, false),
          BazelNativeRuleArgument("shared_library", BAZEL_NONE, false),
          BazelNativeRuleArgument("static_library", BAZEL_NONE, false),
          BazelNativeRuleArgument("system_provided", BAZEL_FALSE, false),
        ),
      ),
      addNativeRule(
        "cc_library",
        "https://bazel.build/reference/be/c-cpp#cc_library",
        setOf(
          BazelNativeRuleArgument("name", BAZEL_EMPTY_STRING, true),
          BazelNativeRuleArgument("deps", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("srcs", BAZEL_EMPTY_LIST, true),
          BazelNativeRuleArgument("data", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("hdrs", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("additional_compiler_inputs", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("additional_linker_inputs", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("alwayslink", BAZEL_FALSE, false),
          BazelNativeRuleArgument("conlyopts", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("copts", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("cxxopts", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("defines", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("hdrs_check", BAZEL_EMPTY_STRING, false),
          BazelNativeRuleArgument("implementation_deps", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("include_prefix", BAZEL_EMPTY_STRING, false),
          BazelNativeRuleArgument("includes", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("linkopts", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("linkstamp", BAZEL_NONE, false),
          BazelNativeRuleArgument("linkstatic", BAZEL_FALSE, false),
          BazelNativeRuleArgument("local_defines", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("module_interfaces", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("strip_include_prefix", BAZEL_EMPTY_STRING, false),
          BazelNativeRuleArgument("textual_hdrs", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("win_def_file", BAZEL_NONE, false),
        ),
      ),
      addNativeRule(
        "cc_shared_library",
        "https://bazel.build/reference/be/c-cpp#cc_shared_library",
        setOf(
          BazelNativeRuleArgument("name", BAZEL_EMPTY_STRING, true),
          BazelNativeRuleArgument("deps", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("additional_linker_inputs", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("dynamic_deps", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("exports_filter", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("roots", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("shared_lib_name", BAZEL_EMPTY_STRING, false),
          BazelNativeRuleArgument("static_deps", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("user_link_flags", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("win_def_file", BAZEL_NONE, false),
        ),
      ),
      addNativeRule(
        "cc_static_library",
        "https://bazel.build/reference/be/c-cpp#cc_static_library",
        setOf(
          BazelNativeRuleArgument("name", BAZEL_EMPTY_STRING, true),
          BazelNativeRuleArgument("deps", BAZEL_EMPTY_LIST, false),
        ),
      ),
      addNativeRule(
        "cc_test",
        "https://bazel.build/reference/be/c-cpp#cc_test",
        setOf(
          BazelNativeRuleArgument("name", BAZEL_EMPTY_STRING, true),
          BazelNativeRuleArgument("deps", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("src", BAZEL_EMPTY_LIST, true),
          BazelNativeRuleArgument("data", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("additional_linker_inputs", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("conlyopts", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("copts", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("cxxopts", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("defines", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("dynamic_deps", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("hdrs_check", BAZEL_EMPTY_STRING, false),
          BazelNativeRuleArgument("includes", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("link_extra_lib", BAZEL_EMPTY_STRING, false),
          BazelNativeRuleArgument("linkopts", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("linkshared", BAZEL_FALSE, false),
          BazelNativeRuleArgument("linkstatic", BAZEL_FALSE, false),
          BazelNativeRuleArgument("local_defines", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("malloc", BAZEL_EMPTY_STRING, false),
          BazelNativeRuleArgument("module_interfaces", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("nocopts", BAZEL_EMPTY_STRING, false),
          BazelNativeRuleArgument("reexport_deps", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("stamp", "0", false),
          BazelNativeRuleArgument("win_def_file", BAZEL_NONE, false),
        ),
      ),
      addNativeRule(
        "cc_toolchain",
        "https://bazel.build/reference/be/c-cpp#cc_toolchain",
        setOf(
          BazelNativeRuleArgument("name", BAZEL_EMPTY_STRING, true),
          BazelNativeRuleArgument("all_files", BAZEL_EMPTY_STRING, true),
          BazelNativeRuleArgument("ar_files", BAZEL_NONE, false),
          BazelNativeRuleArgument("as_files", BAZEL_NONE, false),
          BazelNativeRuleArgument("compiler_files", BAZEL_NONE, true),
          BazelNativeRuleArgument("compiler_files_without_includes", BAZEL_NONE, false),
          BazelNativeRuleArgument("coverage_files", BAZEL_NONE, false),
          BazelNativeRuleArgument("dwp_files", BAZEL_NONE, true),
          BazelNativeRuleArgument("dynamic_runtime_lib", BAZEL_NONE, false),
          BazelNativeRuleArgument("libc_top", BAZEL_NONE, false),
          BazelNativeRuleArgument("linker_files", BAZEL_NONE, true),
          BazelNativeRuleArgument("module_map", BAZEL_NONE, false),
          BazelNativeRuleArgument("objcopy_files", BAZEL_NONE, true),
          BazelNativeRuleArgument("output_licenses", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("static_runtime_lib", BAZEL_NONE, false),
          BazelNativeRuleArgument("strip_files", BAZEL_NONE, true),
          BazelNativeRuleArgument("supports_header_parsing", BAZEL_FALSE, false),
          BazelNativeRuleArgument("supports_param_files", BAZEL_TRUE, false),
          BazelNativeRuleArgument("toolchain_config", BAZEL_TRUE, true),
          BazelNativeRuleArgument("toolchain_identifier", BAZEL_EMPTY_STRING, false),
        ),
      ),
      addNativeRule(
        "cc_toolchain_suite",
        "https://bazel.build/reference/be/c-cpp#cc_toolchain_suite",
        setOf(
          BazelNativeRuleArgument("name", BAZEL_EMPTY_STRING, true),
        ),
      ),
      addNativeRule(
        "fdo_prefetch_hints",
        "https://bazel.build/reference/be/c-cpp#fdo_prefetch_hints",
        setOf(
          BazelNativeRuleArgument("name", BAZEL_EMPTY_STRING, true),
          BazelNativeRuleArgument("profile", BAZEL_NONE, true),
        ),
      ),
      addNativeRule(
        "fdo_profile",
        "https://bazel.build/reference/be/c-cpp#fdo_profile",
        setOf(
          BazelNativeRuleArgument("name", BAZEL_EMPTY_STRING, true),
          BazelNativeRuleArgument("memprof_profile", BAZEL_NONE, false),
          BazelNativeRuleArgument("profile", BAZEL_NONE, true),
          BazelNativeRuleArgument("proto_profile", BAZEL_NONE, false),
        ),
      ),
      addNativeRule(
        "memprof_profile",
        "https://bazel.build/reference/be/c-cpp#memprof_profile",
        setOf(
          BazelNativeRuleArgument("name", BAZEL_EMPTY_STRING, true),
          BazelNativeRuleArgument("profile", BAZEL_NONE, true),
        ),
      ),
      addNativeRule(
        "propeller_optimize",
        "https://bazel.build/reference/be/c-cpp#propeller_optimize",
        setOf(
          BazelNativeRuleArgument("name", BAZEL_EMPTY_STRING, true),
          BazelNativeRuleArgument("cc_profile", BAZEL_NONE, true),
          BazelNativeRuleArgument("ld_profile", BAZEL_NONE, true),
        ),
      ),
      // Objective-C
      addNativeRule(
        "objc_import",
        "https://bazel.build/reference/be/objective-c#objc_import",
        setOf(
          BazelNativeRuleArgument("name", BAZEL_EMPTY_STRING, true),
          BazelNativeRuleArgument("deps", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("hdrs", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("alwayslink", BAZEL_FALSE, false),
          BazelNativeRuleArgument("archives", BAZEL_NONE, true),
          BazelNativeRuleArgument("includes", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("sdk_dylibs", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("sdk_frameworks", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("sdk_includes", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("textual_hdrs", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("weak_sdk_frameworks", BAZEL_EMPTY_LIST, false),
        ),
      ),
      addNativeRule(
        "objc_library",
        "https://bazel.build/reference/be/objective-c#objc_library",
        setOf(
          BazelNativeRuleArgument("name", BAZEL_EMPTY_STRING, true),
          BazelNativeRuleArgument("deps", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("srcs", BAZEL_EMPTY_LIST, true),
          BazelNativeRuleArgument("hdrs", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("alwayslink", BAZEL_FALSE, false),
          BazelNativeRuleArgument("conlyopts", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("copts", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("cxxopts", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("defines", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("enable_modules", BAZEL_FALSE, false),
          BazelNativeRuleArgument("implementation_deps", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("includes", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("linkopts", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("module_map", BAZEL_NONE, false),
          BazelNativeRuleArgument("module_name", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("non_arc_srcs", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("pch", BAZEL_NONE, false),
          BazelNativeRuleArgument("sdk_dylibs", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("sdk_frameworks", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("sdk_includes", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("stamp", BAZEL_FALSE, false),
          BazelNativeRuleArgument("textual_hdrs", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("weak_sdk_frameworks", BAZEL_EMPTY_LIST, false),
        ),
      ),
      // Protocol buffer.
      addNativeRule(
        "cc_proto_library",
        "https://bazel.build/reference/be/protocol-buffer#cc_proto_library",
        setOf(
          BazelNativeRuleArgument("name", BAZEL_EMPTY_STRING, true),
          BazelNativeRuleArgument("deps", BAZEL_EMPTY_LIST, false),
        ),
      ),
      addNativeRule(
        "java_lite_proto_library",
        "https://bazel.build/reference/be/protocol-buffer#java_lite_proto_library",
        setOf(
          BazelNativeRuleArgument("name", BAZEL_EMPTY_STRING, true),
          BazelNativeRuleArgument("deps", BAZEL_EMPTY_LIST, false),
        ),
      ),
      addNativeRule(
        "java_proto_library",
        "https://bazel.build/reference/be/protocol-buffer#java_proto_library",
        setOf(
          BazelNativeRuleArgument("name", BAZEL_EMPTY_STRING, true),
          BazelNativeRuleArgument("deps", BAZEL_EMPTY_LIST, false),
        ),
      ),
      addNativeRule(
        "proto_library",
        "https://bazel.build/reference/be/protocol-buffer#proto_library",
        setOf(
          BazelNativeRuleArgument("name", BAZEL_EMPTY_STRING, true),
          BazelNativeRuleArgument("deps", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("srcs", BAZEL_EMPTY_LIST, true),
          BazelNativeRuleArgument("allow_exports", BAZEL_NONE, false),
          BazelNativeRuleArgument("exports", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("import_prefix", BAZEL_EMPTY_STRING, false),
          BazelNativeRuleArgument("strip_import_prefix", BAZEL_EMPTY_STRING, false),
        ),
      ),
      addNativeRule(
        "py_proto_library",
        "https://bazel.build/reference/be/protocol-buffer#py_proto_library",
        setOf(
          BazelNativeRuleArgument("name", BAZEL_EMPTY_STRING, true),
          BazelNativeRuleArgument("deps", BAZEL_EMPTY_LIST, false),
        ),
      ),
      addNativeRule(
        "proto_lang_toolchain",
        "https://bazel.build/reference/be/protocol-buffer#proto_lang_toolchain",
        setOf(
          BazelNativeRuleArgument("name", BAZEL_EMPTY_STRING, true),
          BazelNativeRuleArgument("allowlist_different_package", BAZEL_NONE, false),
          BazelNativeRuleArgument("blacklisted_protos", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("command_line", BAZEL_EMPTY_STRING, true),
          BazelNativeRuleArgument("mnemonic", BAZEL_EMPTY_STRING, false),
          BazelNativeRuleArgument("output_files", BAZEL_EMPTY_STRING, false),
          BazelNativeRuleArgument("plugin", BAZEL_NONE, false),
          BazelNativeRuleArgument("plugin_format_flag", BAZEL_EMPTY_STRING, false),
          BazelNativeRuleArgument("progress_message", BAZEL_EMPTY_STRING, false),
          BazelNativeRuleArgument("protoc_minimal_do_not_use", BAZEL_NONE, false),
          BazelNativeRuleArgument("runtime", BAZEL_NONE, false),
          BazelNativeRuleArgument("toolchain_type", BAZEL_NONE, false),
        ),
      ),
      addNativeRule(
        "proto_toolchain",
        "https://bazel.build/reference/be/protocol-buffer#proto_toolchain",
        setOf(
          BazelNativeRuleArgument("name", BAZEL_EMPTY_STRING, true),
          BazelNativeRuleArgument("command_line", BAZEL_EMPTY_STRING, false),
          BazelNativeRuleArgument("mnemonic", BAZEL_EMPTY_STRING, false),
          BazelNativeRuleArgument("output_files", BAZEL_EMPTY_STRING, false),
          BazelNativeRuleArgument("progress_message", BAZEL_EMPTY_STRING, false),
          BazelNativeRuleArgument("proto_compiler", BAZEL_NONE, false),
        ),
      ),
      // Python.
      addNativeRule(
        "py_binary",
        "https://bazel.build/reference/be/python#py_binary",
        setOf(
          BazelNativeRuleArgument("name", BAZEL_EMPTY_STRING, true),
          BazelNativeRuleArgument("deps", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("srcs", BAZEL_EMPTY_LIST, true),
          BazelNativeRuleArgument("data", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("imports", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("legacy_create_init", "-1", false),
          BazelNativeRuleArgument("main", BAZEL_NONE, false),
          BazelNativeRuleArgument("precompile", BAZEL_EMPTY_STRING, false),
          BazelNativeRuleArgument("precompile_invalidation_mode", BAZEL_EMPTY_STRING, false),
          BazelNativeRuleArgument("precompile_optimize_level", "0", false),
          BazelNativeRuleArgument("precompile_source_retention", BAZEL_EMPTY_STRING, false),
          BazelNativeRuleArgument("pyc_collection", BAZEL_EMPTY_STRING, false),
          BazelNativeRuleArgument("python_version", BAZEL_EMPTY_STRING, false),
          BazelNativeRuleArgument("srcs_version", BAZEL_EMPTY_STRING, false),
          BazelNativeRuleArgument("stamp", "-1", false),
        ),
      ),
      addNativeRule(
        "py_library",
        "https://bazel.build/reference/be/python#py_library",
        setOf(
          BazelNativeRuleArgument("name", BAZEL_EMPTY_STRING, true),
          BazelNativeRuleArgument("deps", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("srcs", BAZEL_EMPTY_LIST, true),
          BazelNativeRuleArgument("data", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("imports", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("precompile", BAZEL_EMPTY_STRING, false),
          BazelNativeRuleArgument("precompile_invalidation_mode", BAZEL_EMPTY_STRING, false),
          BazelNativeRuleArgument("precompile_optimize_level", "0", false),
          BazelNativeRuleArgument("precompile_source_retention", BAZEL_EMPTY_STRING, false),
          BazelNativeRuleArgument("srcs_version", BAZEL_EMPTY_STRING, false),
        ),
      ),
      addNativeRule(
        "py_test",
        "https://bazel.build/reference/be/python#py_test",
        setOf(
          BazelNativeRuleArgument("name", BAZEL_EMPTY_STRING, true),
          BazelNativeRuleArgument("deps", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("srcs", BAZEL_EMPTY_LIST, true),
          BazelNativeRuleArgument("data", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("imports", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("legacy_create_init", "-1", false),
          BazelNativeRuleArgument("main", BAZEL_NONE, false),
          BazelNativeRuleArgument("precompile", BAZEL_EMPTY_STRING, false),
          BazelNativeRuleArgument("precompile_invalidation_mode", BAZEL_EMPTY_STRING, false),
          BazelNativeRuleArgument("precompile_optimize_level", "0", false),
          BazelNativeRuleArgument("precompile_source_retention", BAZEL_EMPTY_STRING, false),
          BazelNativeRuleArgument("pyc_collection", BAZEL_EMPTY_STRING, false),
          BazelNativeRuleArgument("python_version", BAZEL_EMPTY_STRING, false),
          BazelNativeRuleArgument("srcs_version", BAZEL_EMPTY_STRING, false),
          BazelNativeRuleArgument("stamp", "0", false),
        ),
      ),
      addNativeRule(
        "py_runtime",
        "https://bazel.build/reference/be/python#py_runtime",
        setOf(
          BazelNativeRuleArgument("name", BAZEL_EMPTY_STRING, true),
          BazelNativeRuleArgument("abi_flags", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("bootstrap_template", BAZEL_NONE, false),
          BazelNativeRuleArgument("coverage_tool", BAZEL_NONE, false),
          BazelNativeRuleArgument("files", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("implementation_name", BAZEL_EMPTY_STRING, false),
          BazelNativeRuleArgument("interpreter", BAZEL_NONE, false),
          BazelNativeRuleArgument("interpreter_path", BAZEL_EMPTY_STRING, false),
          BazelNativeRuleArgument("interpreter_version_info", BAZEL_STRUCT, false),
          BazelNativeRuleArgument("pyc_tag", BAZEL_EMPTY_STRING, false),
          BazelNativeRuleArgument("python_version", BAZEL_EMPTY_STRING, false),
          BazelNativeRuleArgument("stage2_bootstrap_template", BAZEL_EMPTY_STRING, false),
          BazelNativeRuleArgument("stub_shebang", BAZEL_EMPTY_STRING, false),
          BazelNativeRuleArgument("zip_main_template", BAZEL_EMPTY_STRING, false),
        ),
      ),
      // Shell.
      addNativeRule(
        "sh_binary",
        "https://bazel.build/reference/be/shell#sh_binary",
        setOf(
          BazelNativeRuleArgument("name", BAZEL_EMPTY_STRING, true),
          BazelNativeRuleArgument("deps", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("srcs", BAZEL_EMPTY_LIST, true),
          BazelNativeRuleArgument("env_inherit", BAZEL_EMPTY_LIST, false),
        ),
      ),
      addNativeRule(
        "sh_library",
        "https://bazel.build/reference/be/shell#sh_library",
        setOf(
          BazelNativeRuleArgument("name", BAZEL_EMPTY_STRING, true),
          BazelNativeRuleArgument("deps", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("srcs", BAZEL_EMPTY_LIST, true),
        ),
      ),
      addNativeRule(
        "sh_test",
        "https://bazel.build/reference/be/shell#sh_test",
        setOf(
          BazelNativeRuleArgument("name", BAZEL_EMPTY_STRING, true),
          BazelNativeRuleArgument("deps", BAZEL_EMPTY_LIST, false),
          BazelNativeRuleArgument("srcs", BAZEL_EMPTY_LIST, true),
        ),
      ),
    )

  val ruleNames = NATIVE_RULES_MAP.keys

  fun getRuleByName(name: String): BazelNativeRule? = NATIVE_RULES_MAP[name]
}
