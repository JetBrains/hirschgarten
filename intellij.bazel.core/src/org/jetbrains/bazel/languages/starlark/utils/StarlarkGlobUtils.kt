package org.jetbrains.bazel.languages.starlark.utils

import com.intellij.psi.PsiElement
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkBinaryExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkGlobExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkListLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression

/**
 * Heuristically checks if the given "srcs = ..." expression positively matches the source file
 */
@ApiStatus.Internal
fun srcsExpressionMatchPath(expr: PsiElement, relativePath: String): Boolean {
  if (expr is StarlarkCallExpression) {
    return expr.getCalledExpression()?.let { srcsExpressionMatchPath(it, relativePath) } ?: false
  }

  if (expr is StarlarkGlobExpression) {
    return expr.getGlob()?.match(relativePath) == true
  }

  if (expr is StarlarkStringLiteralExpression) {
    val value = expr.getStringContents()
    // TODO: support label refs
    if (value.contains(":") || value.startsWith("@") || value.startsWith("//"))
      return false
    return value == relativePath
  }

  if (expr is StarlarkListLiteralExpression) {
    return expr.getElements().any { srcsExpressionMatchPath(it, relativePath) }
  }

  if (expr is StarlarkBinaryExpression && expr.getOperator() == StarlarkTokenTypes.PLUS) {
    return expr.getOperands().any { srcsExpressionMatchPath(it, relativePath) }
  }

  return false
}
