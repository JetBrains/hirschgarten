package org.jetbrains.bazel.languages.starlark.bazel

data class BazelGlobalFunctionParameter(
  val name: String,
  val default: String?,
  val positional: Boolean,
  val required: Boolean = false,
  val docString: String? = null,
)

data class BazelGlobalFunction(
  val name: String,
  val docString: String? = null,
  val params: List<BazelGlobalFunctionParameter> = emptyList(),
)

object BazelGlobalFunctions {
  val STARLARK_FUNCTIONS =
    mapOf(
      "abs" to BazelGlobalFunction("abs"),
      "all" to BazelGlobalFunction("all"),
      "any" to BazelGlobalFunction("any"),
      "bool" to BazelGlobalFunction("bool"),
      "dict" to BazelGlobalFunction("dict"),
      "dir" to BazelGlobalFunction("dir"),
      "enumerate" to BazelGlobalFunction("enumerate"),
      "fail" to BazelGlobalFunction("fail"),
      "float" to BazelGlobalFunction("float"),
      "getattr" to BazelGlobalFunction("getattr"),
      "hasattr" to BazelGlobalFunction("hasattr"),
      "hash" to BazelGlobalFunction("hash"),
      "int" to BazelGlobalFunction("int"),
      "len" to BazelGlobalFunction("len"),
      "list" to BazelGlobalFunction("list"),
      "max" to BazelGlobalFunction("max"),
      "min" to BazelGlobalFunction("min"),
      "print" to BazelGlobalFunction("print"),
      "range" to BazelGlobalFunction("range"),
      "repr" to BazelGlobalFunction("repr"),
      "reversed" to BazelGlobalFunction("reversed"),
      "sorted" to BazelGlobalFunction("sorted"),
      "str" to BazelGlobalFunction("str"),
      "tuple" to BazelGlobalFunction("tuple"),
      "type" to BazelGlobalFunction("type"),
      "zip" to BazelGlobalFunction("zip"),
    )

  val EXTENSION_FUNCTIONS =
    mapOf(
      "analysis_test_transition" to BazelGlobalFunction("analysis_test_transition"),
      "aspect" to BazelGlobalFunction("aspect"),
      "configuration_field" to BazelGlobalFunction("configuration_field"),
      "depset" to BazelGlobalFunction("depset"),
      "exec_group" to BazelGlobalFunction("exec_group"),
      "load" to BazelGlobalFunction("load"),
      "module_extension" to BazelGlobalFunction("module_extension"),
      "provider" to BazelGlobalFunction("provider"),
      "repository_rule" to BazelGlobalFunction("repository_rule"),
      "rule" to BazelGlobalFunction("rule"),
      "select" to BazelGlobalFunction("select"),
      "subrule" to BazelGlobalFunction("subrule"),
      "tag_class" to BazelGlobalFunction("tag_class"),
      "visibility" to BazelGlobalFunction("visibility"),
    )

  val WORKSPACE_FUNCTIONS =
    mapOf(
      "bind" to BazelGlobalFunction("bind"),
      "register_execution_platforms" to BazelGlobalFunction("register_execution_platforms"),
      "register_toolchains" to BazelGlobalFunction("register_toolchains"),
      "workspace" to BazelGlobalFunction("workspace"),
    )
}
