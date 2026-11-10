package org.jetbrains.bazel.languages.starlark.psi.expressions

import com.intellij.lang.ASTNode
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor

@ApiStatus.Internal
class StarlarkGeneratorExpression(node: ASTNode) : StarlarkCompExpression(node) {
  override fun acceptVisitor(visitor: StarlarkElementVisitor) = visitor.visitGeneratorExpression(this)
}
