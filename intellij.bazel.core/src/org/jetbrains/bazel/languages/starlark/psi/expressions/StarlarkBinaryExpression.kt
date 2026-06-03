package org.jetbrains.bazel.languages.starlark.psi.expressions

import com.intellij.lang.ASTNode
import org.jetbrains.annotations.ApiStatus
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.elementType
import org.jetbrains.bazel.languages.starlark.elements.StarlarkElementTypes
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenSets
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor

@ApiStatus.Internal
class StarlarkBinaryExpression(node: ASTNode) : StarlarkBaseElement(node) {
  override fun acceptVisitor(visitor: StarlarkElementVisitor) = visitor.visitBinaryExpression(this)

  fun getOperands(): List<PsiElement> = findChildrenByType(StarlarkElementTypes.EXPRESSIONS)

  fun getLeftOperand(): PsiElement? = getOperands().firstOrNull()

  fun getRightOperand(): PsiElement? = getOperands().getOrNull(1)

  /**
   * The IElementType of the operator token that sits between the two operands (e.g. `+`, `-`).
   * For compound operations ("not in" and "is not") the first keyword is returned
   */
  fun getOperator(): IElementType? = findChildByType<PsiElement>(StarlarkTokenSets.BINARY_OPERATIONS)?.elementType
}
