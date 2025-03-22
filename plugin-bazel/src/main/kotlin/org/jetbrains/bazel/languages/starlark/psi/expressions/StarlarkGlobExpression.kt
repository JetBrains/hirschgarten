package org.jetbrains.bazel.languages.starlark.psi.expressions

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiReference
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import org.jetbrains.bazel.languages.starlark.references.StarlarkGlobReference

class StarlarkGlobExpression(node: ASTNode): StarlarkBaseElement(node) {
  override fun acceptVisitor(visitor: StarlarkElementVisitor) = visitor.visitGlobExpression(this)

  override fun getReference(): PsiReference = StarlarkGlobReference(this)
}
