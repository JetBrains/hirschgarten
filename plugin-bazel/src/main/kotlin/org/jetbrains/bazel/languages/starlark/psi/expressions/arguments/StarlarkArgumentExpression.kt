package org.jetbrains.bazel.languages.starlark.psi.expressions.arguments

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiReference
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import org.jetbrains.bazel.languages.starlark.references.StarlarkArgumentReference

class StarlarkArgumentExpression(node: ASTNode) :
  StarlarkBaseElement(node),
  StarlarkArgumentElement {
  override fun acceptVisitor(visitor: StarlarkElementVisitor) = visitor.visitArgumentExpression(this)

  override fun getReference(): PsiReference = StarlarkArgumentReference(this, textRange)
}
