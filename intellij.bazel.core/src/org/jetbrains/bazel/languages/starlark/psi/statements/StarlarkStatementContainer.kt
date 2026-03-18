package org.jetbrains.bazel.languages.starlark.psi.statements

internal interface StarlarkStatementContainer {
  fun getStatementLists(): List<StarlarkStatementList>
}
