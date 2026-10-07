package org.jetbrains.bazel.languages.starlark.utils

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.StarlarkUtils.nearestRelevantAfter
import org.jetbrains.bazel.languages.starlark.StarlarkUtils.nearestRelevantBeforeOperator
import org.jetbrains.bazel.languages.starlark.StarlarkUtils.selectLeftHandSideOfAssignment
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkListLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkParenthesizedExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTargetExpression
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
    val lhs = selectLeftHandSideOfAssignment(this) ?: return null
    val rhs = nearestRelevantAfter(this, StarlarkTokenTypes.EQ) ?: return null
    return matchAssignedValue(lhs, rhs, target)
  }

  private fun matchAssignedValue(lhs: PsiElement, rhs: PsiElement, target: StarlarkTargetExpression): PsiElement? {
    if (lhs == target) return rhs

    val lhsElements = destructuringElements(lhs) ?: return null
    val rhsElements = destructuringElements(rhs) ?: return null
    if (lhsElements.size != rhsElements.size) return null

    for ((lhsElement, rhsElement) in lhsElements.zip(rhsElements)) {
      matchAssignedValue(lhsElement, rhsElement, target)?.let { return it }
    }

    return null
  }

  private fun destructuringElements(element: PsiElement): List<PsiElement>? =
    when (element) {
      is StarlarkParenthesizedExpression -> element.getTuple()?.let(::destructuringElements)
      is StarlarkTupleExpression -> element.expressionElements()
      is StarlarkListLiteralExpression -> element.expressionElements()
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
