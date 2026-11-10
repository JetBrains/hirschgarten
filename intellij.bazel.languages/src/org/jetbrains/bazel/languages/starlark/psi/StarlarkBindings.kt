package org.jetbrains.bazel.languages.starlark.psi

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.elementType
import com.intellij.psi.util.parents
import com.intellij.psi.util.siblings
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCompExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkListLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkParenthesizedExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTargetExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTupleExpression
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkAssignmentStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkForStatement

/**
 * The names that this target binds.
 * A target is the left side of an assignment, or the part between `for` and `in` of a `for` statement or a comprehension.
 * Parentheses, tuples and lists in a target destructure the value.
 */
@ApiStatus.Internal
fun PsiElement.boundNames(): List<StarlarkTargetExpression> =
  when (this) {
    is StarlarkTargetExpression -> listOf(this)
    is StarlarkParenthesizedExpression, is StarlarkTupleExpression, is StarlarkListLiteralExpression -> children.flatMap { it.boundNames() }
    else -> emptyList()
  }

/** The assignment, `for` statement or comprehension that binds this name, or null when this element binds no name. */
@ApiStatus.Internal
fun PsiElement.bindingSite(): StarlarkElement? {
  if (this !is StarlarkTargetExpression) return null
  val target = parents(withSelf = true).first { it.parent?.isDestructuring() != true }
  return when (val site = target.parent) {
    is StarlarkAssignmentStatement -> site.takeIf { it.leftHandSide() == target }
    is StarlarkForStatement, is StarlarkCompExpression -> (site as StarlarkElement).takeIf { target.isLoopTarget() }
    else -> null
  }
}

@ApiStatus.Internal
fun PsiElement.isBinding(): Boolean = bindingSite() != null

/** True when this element follows `for`, so it is the target of a `for` statement or a comprehension. */
internal fun PsiElement.isLoopTarget(): Boolean =
  siblings(forward = false, withSelf = false).firstOrNull { it !is PsiWhiteSpace && it !is PsiComment }.elementType ===
    StarlarkTokenTypes.FOR_KEYWORD

private fun PsiElement.isDestructuring(): Boolean =
  this is StarlarkParenthesizedExpression || this is StarlarkTupleExpression || this is StarlarkListLiteralExpression
