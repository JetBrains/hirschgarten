package org.jetbrains.bazel.languages.starlark.psi

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.elementType
import com.intellij.psi.util.parents
import com.intellij.psi.util.siblings
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.parser.generated.StarlarkTokenTypes
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCompExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkListLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkParenthesizedExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkReferenceExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStarExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTupleExpression
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkAssignmentStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkForStatement

/**
 * The names that this target binds.
 * A target is the left side of an assignment, or the part between `for` and `in` of a `for` statement or a comprehension.
 * Parentheses, tuples and lists in a target destructure the value.
 * A starred name, as `b` in `a, *b = c`, binds as in Python. Starlark does not support it, and an inspection reports it.
 */
@ApiStatus.Internal
fun PsiElement.boundNames(): List<StarlarkReferenceExpression> =
  when (this) {
    is StarlarkReferenceExpression -> listOfNotNull(takeIf { it.isBindableName() })
    is StarlarkParenthesizedExpression,
    is StarlarkTupleExpression,
    is StarlarkListLiteralExpression,
    is StarlarkStarExpression -> children.flatMap { it.boundNames() }
    else -> emptyList()
  }

/** The assignment, `for` statement or comprehension that binds this name, or null when this element binds no name. */
@ApiStatus.Internal
fun PsiElement.bindingSite(): StarlarkElement? {
  if (this !is StarlarkReferenceExpression || !isBindableName()) return null
  val target = parents(withSelf = true).first { it.parent?.isTargetPart() != true }
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

/** A plain name can be bound. A qualified name `a.b` and the name `_` bind nothing. */
private fun StarlarkReferenceExpression.isBindableName(): Boolean = !isQualified() && name != "_"

private fun PsiElement.isTargetPart(): Boolean =
  this is StarlarkParenthesizedExpression ||
  this is StarlarkTupleExpression ||
  this is StarlarkListLiteralExpression ||
  this is StarlarkStarExpression
