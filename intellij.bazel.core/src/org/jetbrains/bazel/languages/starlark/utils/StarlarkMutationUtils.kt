package org.jetbrains.bazel.languages.starlark.utils

import com.intellij.psi.PsiElement
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.StarlarkUtils.nearestRelevantBeforeOperator
import org.jetbrains.bazel.languages.starlark.StarlarkUtils.selectLeftHandSideOfAssignment
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkReferenceExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkSubscriptionExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.getSimpleNameOrNull
import org.jetbrains.bazel.languages.starlark.psi.expressions.isSimpleNameExpression
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkAssignmentStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkAugAssignmentStatement

@ApiStatus.Internal
object StarlarkMutationUtils {
  data class Mutation(
    val target: PsiElement,
    val problemElement: PsiElement,
    val kind: Kind,
    val methodName: String? = null,
  ) {
    enum class Kind { METHOD_CALL, SUBSCRIPTION_ASSIGNMENT, AUGMENTED_ASSIGNMENT }
  }

  fun mutationOrNull(element: PsiElement): Mutation? =
    getMutatingCall(element)
    ?: getMutatingAssignment(element)
    ?: getMutatingAugAssignment(element)

  fun areReferencesEqual(left: PsiElement, right: PsiElement): Boolean {
    val leftResolved = left.reference?.resolve()
    val rightResolved = right.reference?.resolve()
    if (leftResolved != null && rightResolved != null) return leftResolved == rightResolved

    val leftName = left.getSimpleNameOrNull() ?: return false
    val rightName = right.getSimpleNameOrNull() ?: return false
    return leftName == rightName
  }

  fun isKnownMutatingMethod(name: String): Boolean = name in StarlarkStaticValueKind.KNOWN_MUTATING_METHODS

  private fun getMutatingCall(element: PsiElement): Mutation? {
    val call = element as? StarlarkCallExpression ?: return null
    val calledExpression = call.getCalledExpression() as? StarlarkReferenceExpression ?: return null
    val methodName = calledExpression.name ?: return null
    if (!isKnownMutatingMethod(methodName)) return null

    val target = calledExpression.getQualifierExpression()?.takeIf { it.isSimpleNameExpression() } ?: return null
    return Mutation(target, calledExpression, Mutation.Kind.METHOD_CALL, methodName)
  }

  private fun getMutatingAssignment(element: PsiElement): Mutation? {
    val assignment = element as? StarlarkAssignmentStatement ?: return null
    val lhs = selectLeftHandSideOfAssignment(assignment) ?: return null
    val target = extractSubscriptionReceiver(lhs) ?: return null
    return Mutation(target, lhs, Mutation.Kind.SUBSCRIPTION_ASSIGNMENT)
  }

  private fun getMutatingAugAssignment(element: PsiElement): Mutation? {
    val assignment = element as? StarlarkAugAssignmentStatement ?: return null
    val lhs = selectLeftHandSideOfAssignment(assignment) ?: return null
    val target = lhs.takeIf { it.isSimpleNameExpression() } ?: extractSubscriptionReceiver(lhs) ?: return null
    return Mutation(target, lhs, Mutation.Kind.AUGMENTED_ASSIGNMENT)
  }

  private fun extractSubscriptionReceiver(element: PsiElement?): PsiElement? {
    val subscription = element as? StarlarkSubscriptionExpression ?: return null
    return nearestRelevantBeforeOperator(subscription.firstChild)?.takeIf { it.isSimpleNameExpression() }
  }
}
