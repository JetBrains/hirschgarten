package org.jetbrains.bazel.languages.starlark.bazel

data class Parameter(
  val name: String,
  val default: String,
  val required: Boolean = false,
  val docString: String? = null,
)

data class GlobalFunction(
  val name: String,
  val params: List<Parameter> = emptyList(),
  val docString: String? = null,
)

object BazelGlobalFunctions {
  val STARLARK_FUNCTIONS =
    mapOf(
      "abs" to GlobalFunction("abs"),
      "all" to GlobalFunction("all"),
      "any" to GlobalFunction("any"),
      "bool" to GlobalFunction("bool"),
      "dict" to GlobalFunction("dict"),
      "dir" to GlobalFunction("dir"),
      "enumerate" to GlobalFunction("enumerate"),
      "fail" to GlobalFunction("fail"),
      "float" to GlobalFunction("float"),
      "getattr" to GlobalFunction("getattr"),
      "hasattr" to GlobalFunction("hasattr"),
      "hash" to GlobalFunction("hash"),
      "int" to GlobalFunction("int"),
      "len" to GlobalFunction("len"),
      "list" to GlobalFunction("list"),
      "max" to GlobalFunction("max"),
      "min" to GlobalFunction("min"),
      "print" to GlobalFunction("print"),
      "range" to GlobalFunction("range"),
      "repr" to GlobalFunction("repr"),
      "reversed" to GlobalFunction("reversed"),
      "sorted" to GlobalFunction("sorted"),
      "str" to GlobalFunction("str"),
      "tuple" to GlobalFunction("tuple"),
      "type" to GlobalFunction("type"),
      "zip" to GlobalFunction("zip"),
    )

  val EXTENSION_FUNCTIONS =
    mapOf(
      "analysis_test_transition" to GlobalFunction("analysis_test_transition"),
      "aspect" to GlobalFunction("aspect"),
      "configuration_field" to GlobalFunction("configuration_field"),
      "depset" to GlobalFunction("depset"),
      "exec_group" to GlobalFunction("exec_group"),
      "load" to GlobalFunction("load"),
      "module_extension" to GlobalFunction("module_extension"),
      "provider" to GlobalFunction("provider"),
      "repository_rule" to GlobalFunction("repository_rule"),
      "rule" to GlobalFunction("rule"),
      "select" to GlobalFunction("select"),
      "subrule" to GlobalFunction("subrule"),
      "tag_class" to GlobalFunction("tag_class"),
      "visibility" to GlobalFunction("visibility"),
    )

  val BUILD_FUNCTIONS =
    mapOf(
      "depset" to GlobalFunction("depset"),
      "existing_rule" to GlobalFunction("existing_rule"),
      "existing_rules" to GlobalFunction("existing_rules"),
      "exports_files" to GlobalFunction("exports_files"),
      "glob" to GlobalFunction("glob"),
      "load" to GlobalFunction("load"),
      "module_name" to GlobalFunction("module_name"),
      "module_version" to GlobalFunction("module_version"),
      "package" to GlobalFunction("package"),
      "package_group" to GlobalFunction("package_group"),
      "package_name" to GlobalFunction("package_name"),
      "package_relative_label" to GlobalFunction("package_relative_label"),
      "repo_name" to GlobalFunction("repo_name"),
      "repository_name" to GlobalFunction("repository_name"),
      "select" to GlobalFunction("select"),
      "subpackages" to GlobalFunction("subpackages"),
    )

  val MODULE_FUNCTIONS =
    mapOf(
      "archive_override" to GlobalFunction("archive_override"),
      "bazel_dep" to GlobalFunction("bazel_dep"),
      "git_override" to GlobalFunction("git_override"),
      "include" to GlobalFunction("include"),
      "local_path_override" to GlobalFunction("local_path_override"),
      "module" to GlobalFunction("module"),
      "multiple_version_override" to GlobalFunction("multiple_version_override"),
      "register_execution_platforms" to GlobalFunction("register_execution_platforms"),
      "register_toolchains" to GlobalFunction("register_toolchains"),
      "single_version_override" to GlobalFunction("single_version_override"),
      "use_extension" to GlobalFunction("use_extension"),
      "use_repo" to GlobalFunction("use_repo"),
      "use_repo_rule" to GlobalFunction("use_repo_rule"),
    )

  val WORKSPACE_FUNCTIONS =
    mapOf(
      "bind" to GlobalFunction("bind"),
      "register_execution_platforms" to GlobalFunction("register_execution_platforms"),
      "register_toolchains" to GlobalFunction("register_toolchains"),
      "workspace" to GlobalFunction("workspace"),
    )
}
