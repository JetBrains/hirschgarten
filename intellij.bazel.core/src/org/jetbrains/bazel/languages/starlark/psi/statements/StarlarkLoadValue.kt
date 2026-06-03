package org.jetbrains.bazel.languages.starlark.psi.statements

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElement
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression

@ApiStatus.Internal
interface StarlarkLoadValue : StarlarkElement {
  fun getLoadStatement(): StarlarkLoadStatement? = parent as? StarlarkLoadStatement

  fun getLoadValueExpression(): StarlarkStringLiteralExpression? = lastChild as? StarlarkStringLiteralExpression

  fun getLoadValueExpressionContent(): String? = getLoadValueExpression()?.getStringContents()
}
