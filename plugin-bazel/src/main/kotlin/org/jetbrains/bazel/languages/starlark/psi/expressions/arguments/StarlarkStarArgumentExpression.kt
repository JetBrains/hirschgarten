package org.jetbrains.bazel.languages.starlark.psi.expressions.arguments

import com.intellij.lang.ASTNode
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor

class StarlarkStarArgumentExpression(node: ASTNode) :
  StarlarkBaseElement(node),
  StarlarkArgumentElement {
  override fun acceptVisitor(visitor: StarlarkElementVisitor) = visitor.visitStarArgumentExpression(this)
}
