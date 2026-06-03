package org.jetbrains.bazel.languages.starlark.psi.functions

import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
interface StarlarkCallable {
  fun getNamedParameters(): List<StarlarkParameter>

  fun getParameters(): List<StarlarkParameter>
}
