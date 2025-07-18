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
