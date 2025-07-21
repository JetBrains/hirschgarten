package org.jetbrains.bazel.languages.starlark.psi.expressions

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiReference
import org.jetbrains.bazel.languages.starlark.elements.StarlarkElementType
import org.jetbrains.bazel.languages.starlark.elements.StarlarkElementTypes
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import org.jetbrains.bazel.languages.starlark.references.StarlarkLocalVariableElement
import org.jetbrains.bazel.languages.starlark.references.StarlarkLocalVariableReference

class StarlarkReferenceExpression(node: ASTNode) :
  StarlarkBaseElement(node),
  StarlarkLocalVariableElement {
  override fun acceptVisitor(visitor: StarlarkElementVisitor) = visitor.visitReferenceExpression(this)

  override fun getReference(): PsiReference? =
    when {
      isThrowaway() -> null
      containsDot() -> null
      hasParentOfType(StarlarkElementTypes.CALL_EXPRESSION) && !isBeforeDot() -> null
      else -> StarlarkLocalVariableReference(this, false)
    }

  override fun getName(): String? = getNameNode()?.text

  override fun getNameNode(): ASTNode? = node.findChildByType(StarlarkTokenTypes.IDENTIFIER)

  private fun hasParentOfType(type: StarlarkElementType): Boolean = node.treeParent?.elementType == type

  private fun isBeforeDot(): Boolean = node.treeNext?.text == "."

  private fun isThrowaway(): Boolean = name == "_"

  private fun containsDot(): Boolean = node.text.contains('.')
}
