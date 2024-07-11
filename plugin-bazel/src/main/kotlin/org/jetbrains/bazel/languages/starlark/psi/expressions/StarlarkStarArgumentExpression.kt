package org.jetbrains.bazel.languages.starlark.psi.expressions

import com.intellij.lang.ASTNode
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor

class StarlarkStarArgumentExpression(node: ASTNode) : StarlarkBaseElement(node) {
  override fun acceptVisitor(visitor: StarlarkElementVisitor) = visitor.visitStarArgumentExpression(this)
}
