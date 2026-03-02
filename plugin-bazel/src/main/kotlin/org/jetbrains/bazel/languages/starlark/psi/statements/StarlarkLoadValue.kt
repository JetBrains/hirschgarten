package org.jetbrains.bazel.languages.starlark.psi.statements

import org.jetbrains.bazel.languages.starlark.psi.StarlarkElement

internal interface StarlarkLoadValue : StarlarkElement {
  fun getLoadStatement(): StarlarkLoadStatement? = parent as? StarlarkLoadStatement
}
