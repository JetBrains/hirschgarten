package org.jetbrains.bazel.languages.starlark.psi.statements

import org.jetbrains.bazel.languages.starlark.psi.StarlarkElement

interface StarlarkLoadValue : StarlarkElement {
  fun getLoadStatement(): StarlarkLoadStatement? = parent as? StarlarkLoadStatement
}
