package org.jetbrains.bazel.languages.starlark.psi.expressions

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import org.jetbrains.bazel.languages.starlark.references.StarlarkFunctionCallReference
import org.jetbrains.bazel.languages.starlark.references.StarlarkLocalVariableElement
import org.jetbrains.bazel.languages.starlark.references.StarlarkLocalVariableReference
import org.jetbrains.bazel.languages.starlark.references.StarlarkQualifiedReferenceExpressionReference

@ApiStatus.Internal
class StarlarkReferenceExpression(node: ASTNode) :
  StarlarkBaseElement(node),
  StarlarkLocalVariableElement,
  StarlarkQualifiableExpression {
  override fun acceptVisitor(visitor: StarlarkElementVisitor) = visitor.visitReferenceExpression(this)

  override fun getReference(): PsiReference? =
    when {
      isThrowaway() -> null
      isQualified() -> StarlarkQualifiedReferenceExpressionReference(this)
      (parent as? StarlarkCallExpression)?.getCalledExpression() == this && !isBeforeDot() -> {
        when (val identifier = getNameIdentifier()) {
          null -> null
          else -> StarlarkFunctionCallReference(this,  identifier.textRangeInParent)
        }
      }
      else -> StarlarkLocalVariableReference(this, false)
    }

  override fun getNameIdentifier(): PsiElement? = findChildByType(StarlarkTokenTypes.IDENTIFIER)
  override fun getName(): String? = getNameIdentifier()?.text

  private fun isBeforeDot(): Boolean = generateSequence(node.treeNext) { it.treeNext }.any { it.elementType == StarlarkTokenTypes.DOT }

  private fun isThrowaway(): Boolean = name == "_"
}
