package org.jetbrains.bazel.languages.starlark.psi.expressions

import com.intellij.lang.ASTNode
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor

class StarlarkDictCompExpression(node: ASTNode) : StarlarkCompExpression(node) {
  override fun acceptVisitor(visitor: StarlarkElementVisitor) = visitor.visitDictCompExpression(this)
}
