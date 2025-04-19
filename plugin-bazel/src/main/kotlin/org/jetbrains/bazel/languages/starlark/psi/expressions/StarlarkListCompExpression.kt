package org.jetbrains.bazel.languages.starlark.psi.expressions

import com.intellij.lang.ASTNode
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor

class StarlarkListCompExpression(node: ASTNode) : StarlarkCompExpression(node) {
  override fun acceptVisitor(visitor: StarlarkElementVisitor) = visitor.visitListCompExpression(this)
}
