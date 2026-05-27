package org.jetbrains.bazel.languages.starlark.psi.statements

import org.jetbrains.bazel.languages.starlark.psi.StarlarkElement
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression

internal interface StarlarkLoadValue : StarlarkElement {
  fun getLoadStatement(): StarlarkLoadStatement? = parent as? StarlarkLoadStatement

  fun getLoadValueExpression(): StarlarkStringLiteralExpression? = lastChild as? StarlarkStringLiteralExpression

  fun getLoadValueExpressionContent(): String? = getLoadValueExpression()?.getStringContents()
}
