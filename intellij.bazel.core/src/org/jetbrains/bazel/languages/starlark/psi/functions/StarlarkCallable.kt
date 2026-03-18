package org.jetbrains.bazel.languages.starlark.psi.functions

internal interface StarlarkCallable {
  fun getNamedParameters(): List<StarlarkParameter>

  fun getParameters(): List<StarlarkParameter>
}
