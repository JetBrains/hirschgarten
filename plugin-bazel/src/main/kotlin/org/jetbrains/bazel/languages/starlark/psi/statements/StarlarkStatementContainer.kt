package org.jetbrains.bazel.languages.starlark.psi.statements

interface StarlarkStatementContainer {
  fun getStatementLists(): List<StarlarkStatementList>
}
