package org.jetbrains.bazel.languages.starlark.psi.statements

import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
interface StarlarkStatementContainer {
  fun getStatementLists(): List<StarlarkStatementList>
}
