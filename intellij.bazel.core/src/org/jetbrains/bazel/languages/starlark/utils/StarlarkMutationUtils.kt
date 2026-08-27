package org.jetbrains.bazel.languages.starlark.utils

import com.intellij.psi.PsiElement
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.StarlarkUtils.nearestRelevantBeforeOperator
import org.jetbrains.bazel.languages.starlark.StarlarkUtils.selectLeftHandSideOfAssignment
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkReferenceExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkSubscriptionExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTargetExpression
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkAssignmentStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkAugAssignmentStatement

@ApiStatus.Internal
object StarlarkMutationUtils {
  data class Mutation(
    val target: PsiElement,
    val problemElement: PsiElement,
  )

  fun mutationOrNull(element: PsiElement): Mutation? =
    getMutatingCall(element)
    ?: getMutatingAssignment(element)
    ?: getMutatingAugAssignment(element)

  fun referenceName(element: PsiElement?): String? =
    when (element) {
      is StarlarkReferenceExpression -> element.takeIf { it.getQualifierExpression() == null }?.name
      is StarlarkTargetExpression -> element.name
      else -> null
    }

  fun areReferencesEqual(left: PsiElement, right: PsiElement): Boolean {
    val leftResolved = left.reference?.resolve()
    val rightResolved = right.reference?.resolve()
    if (leftResolved != null && rightResolved != null) return leftResolved == rightResolved
    return referenceName(left) == referenceName(right)
  }

  fun isKnownMutatingMethod(name: String): Boolean = name in KNOWN_MUTATING_METHODS

  private fun getMutatingCall(element: PsiElement): Mutation? {
    val call = element as? StarlarkCallExpression ?: return null
    val calledExpression = call.getCalledExpression() as? StarlarkReferenceExpression ?: return null
    val methodName = calledExpression.name ?: return null
    if (!isKnownMutatingMethod(methodName)) return null

    val target = calledExpression.getQualifierExpression()?.takeIf { referenceName(it) != null } ?: return null
    return Mutation(target, calledExpression)
  }

  private fun getMutatingAssignment(element: PsiElement): Mutation? {
    val assignment = element as? StarlarkAssignmentStatement ?: return null
    val lhs = selectLeftHandSideOfAssignment(assignment) ?: return null
    val target = extractSubscriptionReceiver(lhs) ?: return null
    return Mutation(target, lhs)
  }

  private fun getMutatingAugAssignment(element: PsiElement): Mutation? {
    val assignment = element as? StarlarkAugAssignmentStatement ?: return null
    val lhs = selectLeftHandSideOfAssignment(assignment) ?: return null
    val target = lhs.takeIf { referenceName(it) != null } ?: extractSubscriptionReceiver(lhs) ?: return null
    return Mutation(target, lhs)
  }

  private fun extractSubscriptionReceiver(element: PsiElement?): PsiElement? {
    val subscription = element as? StarlarkSubscriptionExpression ?: return null
    return nearestRelevantBeforeOperator(subscription.firstChild)?.takeIf { referenceName(it) != null }
  }
  private val KNOWN_MUTATING_METHODS = setOf(
    "add",
    "append",
    "clear",
    "difference_update",
    "discard",
    "extend",
    "insert",
    "intersection_update",
    "pop",
    "popitem",
    "remove",
    "setdefault",
    "symmetric_difference_update",
    "update",
  )
}
