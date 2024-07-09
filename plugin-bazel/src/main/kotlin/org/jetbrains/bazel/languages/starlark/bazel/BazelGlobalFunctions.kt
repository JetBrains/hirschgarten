package org.jetbrains.bazel.languages.starlark.bazel

object BazelGlobalFunctions {
  val STARLARK_FUNCTIONS = setOf(
    "abs",
    "all",
    "any",
    "bool",
    "dict",
    "dir",
    "enumerate",
    "fail",
    "float",
    "getattr",
    "hasattr",
    "hash",
    "int",
    "len",
    "list",
    "load",
    "max",
    "min",
    "print",
    "range",
    "repr",
    "reversed",
    "sorted",
    "str",
    "tuple",
    "type",
    "zip",
  )

  val EXTENSION_FUNCTIONS = setOf(
    "analysis_test_transition",
    "aspect",
    "configuration_field",
    "depset",
    "exec_group",
    "module_extension",
    "provider",
    "repository_rule",
    "rule",
    "select",
    "subrule",
    "tag_class",
    "visibility",
  )

  val BUILD_FUNCTIONS = setOf(
    "depset",
    "existing_rule",
    "existing_rules",
    "exports_files",
    "glob",
    "module_name",
    "module_version",
    "package",
    "package_group",
    "package_name",
    "package_relative_label",
    "repo_name",
    "repository_name",
    "select",
    "subpackages",
  )

  val MODULE_FUNCTIONS = setOf(
    "archive_override",
    "bazel_dep",
    "git_override",
    "include",
    "local_path_override",
    "module",
    "multiple_version_override",
    "register_execution_platforms",
    "register_toolchains",
    "single_version_override",
    "use_extension",
    "use_repo",
    "use_repo_rule",
  )

  val WORKSPACE_FUNCTIONS = setOf(
    "bind",
    "register_execution_platforms",
    "register_toolchains",
    "workspace",
  )
}
