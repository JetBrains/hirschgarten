package org.jetbrains.bazel.languages.starlark.utils

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.StarlarkUtils.nearestRelevantBeforeOperator
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkDictLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkFalseLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkFloatLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkIntegerLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkListLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkNoneLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkParenthesizedExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTargetExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTrueLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTupleExpression
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkAssignmentStatement

@ApiStatus.Internal
object StarlarkAssignmentUtils {
  fun assignedValueForTarget(target: StarlarkTargetExpression): PsiElement? {
    val assignment = target.containingAssignment() ?: return null
    return assignment.assignedValueFor(target)
  }

  private fun PsiElement.containingAssignment(): StarlarkAssignmentStatement? =
    this as? StarlarkAssignmentStatement
    ?: PsiTreeUtil.getParentOfType(this, StarlarkAssignmentStatement::class.java, false)

  private fun StarlarkAssignmentStatement.assignedValueFor(target: StarlarkTargetExpression): PsiElement? {
    val lhs = leftHandSide() ?: return null
    val rhs = rightHandSide() ?: return null
    return matchAssignedValue(lhs, rhs, target)
  }

  private fun matchAssignedValue(lhs: PsiElement, rhs: PsiElement, target: StarlarkTargetExpression): PsiElement? {
    if (lhs == target) return rhs

    val lhsElements = destructuringTargets(lhs) ?: return null
    val rhsElements = destructuringElements(rhs) ?: return null
    if (lhsElements.size != rhsElements.size) return null

    for ((lhsElement, rhsElement) in lhsElements.zip(rhsElements)) {
      matchAssignedValue(lhsElement, rhsElement, target)?.let { return it }
    }

    return null
  }

  fun destructuringTargets(element: PsiElement): List<PsiElement>? =
    when (element) {
      is StarlarkParenthesizedExpression -> (element.getTuple() ?: element.expressionElements().singleOrNull())?.let(::destructuringTargets)

      is StarlarkTupleExpression,
      is StarlarkListLiteralExpression -> element.expressionElements()

      else -> null
    }

  fun destructuringElements(element: PsiElement): List<PsiElement>? =
    when (element) {
      is StarlarkParenthesizedExpression -> (element.getTuple() ?: element.expressionElements().singleOrNull())?.let(::destructuringElements)

      is StarlarkTupleExpression,
      is StarlarkListLiteralExpression -> element.expressionElements()

      is StarlarkDictLiteralExpression -> element.getKeys()

      is StarlarkFalseLiteralExpression,
      is StarlarkFloatLiteralExpression,
      is StarlarkIntegerLiteralExpression,
      is StarlarkNoneLiteralExpression,
      is StarlarkStringLiteralExpression,
      is StarlarkTrueLiteralExpression -> listOf(element)

      else -> null
    }

  private fun PsiElement.expressionElements(): List<PsiElement> {
    val result = mutableListOf<PsiElement>()
    var child = nearestRelevantBeforeOperator(firstChild)
    while (child != null) {
      result.add(child)
      child = nearestRelevantBeforeOperator(child.nextSibling)
    }
    return result
  }
}
