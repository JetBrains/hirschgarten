package org.jetbrains.bazel.languages.starlark.psi

import com.intellij.lang.ASTNode
import org.jetbrains.bazel.languages.starlark.psi.base.StarlarkBaseElement

class StarlarkStarArgumentExpression(node: ASTNode) : StarlarkBaseElement(node) {
  override fun acceptVisitor(visitor: StarlarkElementVisitor) = visitor.visitStarArgumentExpression(this)
}
