package org.jetbrains.bazel.languages.starlark.psi.expressions

import com.intellij.psi.PsiElement
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes
import kotlin.takeIf

@ApiStatus.Internal
interface StarlarkQualifiableExpression : PsiElement {
  fun getName(): String?

  /**
   * Check if the expression is qualified: of the form "a.b".
   */
  fun isQualified(): Boolean = node.findChildByType(StarlarkTokenTypes.DOT) != null

  /**
   * If the expression is qualified (of the form "a.b"), return the part the qualifier applies to ("a") as PsiElement.
   */
  fun getQualifierExpression(): PsiElement? {
    if (!isQualified()) return null
    return node.firstChildNode?.psi
  }
}

/**
 * Check if the element is a simple (nonqualified) expression.
 */
@ApiStatus.Internal
fun PsiElement.isSimpleNameExpression(): Boolean =
  this is StarlarkQualifiableExpression && !isQualified()

/**
 * If the element is a simple (nonqualified) expression, return its name. Otherwise, return null.
 */
@ApiStatus.Internal
fun PsiElement.getSimpleNameOrNull(): String? =
  (this as? StarlarkQualifiableExpression)?.takeIf { !it.isQualified() }?.getName()
