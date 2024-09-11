package org.jetbrains.bazel.languages.starlark.psi.functions

import com.intellij.lang.ASTNode
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor

class StarlarkVariadicParameter(node: ASTNode) : StarlarkParameter(node) {
  override fun acceptVisitor(visitor: StarlarkElementVisitor) = visitor.visitVariadicParameter(this)
}
