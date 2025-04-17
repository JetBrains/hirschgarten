package org.jetbrains.bazel.languages.starlark.psi.functions

interface StarlarkCallable {
  fun getNamedParameters(): List<StarlarkParameter>

  fun getParameters(): List<StarlarkParameter>
}
