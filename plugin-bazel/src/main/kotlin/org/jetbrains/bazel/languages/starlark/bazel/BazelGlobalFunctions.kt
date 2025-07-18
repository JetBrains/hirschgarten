package org.jetbrains.bazel.languages.starlark.bazel

enum class Environment {
  ALL,
  BZL,
  BUILD,
  MODULE,
  REPO,
  VENDOR,
}

data class BazelGlobalFunctionParameter(
  val name: String,
  val doc: String?,
  val defaultValue: String?,
  val named: Boolean,
  val positional: Boolean,
  val required: Boolean,
)

data class BazelGlobalFunction(
  val name: String,
  val doc: String?,
  val environment: List<Environment>,
  val params: List<BazelGlobalFunctionParameter>,
)

object BazelGlobalFunctions {
  private fun starlarkWithJustName(name: String) = BazelGlobalFunction(name, null, listOf(Environment.ALL), emptyList())

  val STARLARK_FUNCTIONS =
    listOf(
      starlarkWithJustName("abs"),
      starlarkWithJustName("all"),
      starlarkWithJustName("any"),
      starlarkWithJustName("bool"),
      starlarkWithJustName("dict"),
      starlarkWithJustName("dir"),
      starlarkWithJustName("enumerate"),
      starlarkWithJustName("fail"),
      starlarkWithJustName("float"),
      starlarkWithJustName("getattr"),
      starlarkWithJustName("hasattr"),
      starlarkWithJustName("hash"),
      starlarkWithJustName("int"),
      starlarkWithJustName("len"),
      starlarkWithJustName("list"),
      starlarkWithJustName("max"),
      starlarkWithJustName("min"),
      starlarkWithJustName("print"),
      starlarkWithJustName("range"),
      starlarkWithJustName("repr"),
      starlarkWithJustName("reversed"),
      starlarkWithJustName("sorted"),
      starlarkWithJustName("str"),
      starlarkWithJustName("tuple"),
      starlarkWithJustName("type"),
      starlarkWithJustName("zip"),
    ).associateBy { it.name }

  val EXTENSION_FUNCTIONS =
    mapOf(
      "load" to starlarkWithJustName("load"),
    )

  val WORKSPACE_FUNCTIONS =
    mapOf(
      "bind" to starlarkWithJustName("bind"),
      "workspace" to starlarkWithJustName("workspace"),
    )
}
