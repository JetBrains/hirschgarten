package org.jetbrains.bazel.languages.starlark.psi.expressions

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiReference
import org.jetbrains.bazel.languages.starlark.elements.StarlarkElementTypes
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import org.jetbrains.bazel.languages.starlark.references.StarlarkFunctionCallReference
import org.jetbrains.kotlin.idea.base.psi.relativeTo

class StarlarkCallExpression(node: ASTNode) : StarlarkBaseElement(node) {
  override fun acceptVisitor(visitor: StarlarkElementVisitor) = visitor.visitCallExpression(this)

  override fun getReference(): PsiReference? = getNameNode()?.let {
    val range = it.textRange.relativeTo(this)
    StarlarkFunctionCallReference(this, range)
  }

  override fun getName(): String? = getNameNode()?.text

  fun getNameNode(): ASTNode? = node.findChildByType(StarlarkElementTypes.REFERENCE_EXPRESSION)
}
