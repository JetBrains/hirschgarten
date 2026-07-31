package org.jetbrains.bazel.languages.starlark.utils

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkLambdaExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkReferenceExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTargetExpression
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkCallable
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkAssignmentStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkNamedLoadValue


@ApiStatus.Internal
object StarlarkCallableResolver {
  fun resolve(call: StarlarkCallExpression): StarlarkCallable? {
    val expression = call.getCalledExpression() ?: return null

    findLambdaExpression(expression)?.let { return it }

    val refExpr = findReferenceExpression(expression) ?: return null
    val resolved = refExpr.reference?.resolve() ?: return null

    return getCallableFromResolvedReference(resolved)
  }

  private fun findLambdaExpression(expression: PsiElement): StarlarkLambdaExpression? =
    PsiTreeUtil.getChildOfType(expression, StarlarkLambdaExpression::class.java)

  private fun findReferenceExpression(expression: PsiElement): StarlarkReferenceExpression? =
    expression as? StarlarkReferenceExpression
    ?: PsiTreeUtil.getChildOfType(expression, StarlarkReferenceExpression::class.java)

  private fun getCallableFromResolvedReference(resolved: PsiElement): StarlarkCallable? =
    when (resolved) {
      is StarlarkCallable -> resolved
      is StarlarkTargetExpression -> resolveLambdaAlias(resolved)
      is StarlarkNamedLoadValue -> resolveLoadedCallable(resolved)
      else -> null
    }

  private fun resolveLambdaAlias(target: StarlarkTargetExpression): StarlarkCallable? {
    val assignment = PsiTreeUtil.getParentOfType(target, StarlarkAssignmentStatement::class.java, false) ?: return null
    return findLambdaExpression(assignment)
  }

  private fun resolveLoadedCallable(loadValue: StarlarkNamedLoadValue): StarlarkCallable? {
    val loaded = PsiTreeUtil.getChildOfType(loadValue, StarlarkStringLiteralExpression::class.java) ?: return null
    return loaded.reference?.resolve() as? StarlarkCallable
  }
}
