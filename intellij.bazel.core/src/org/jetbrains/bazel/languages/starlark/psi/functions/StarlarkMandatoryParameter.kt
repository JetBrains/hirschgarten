package org.jetbrains.bazel.languages.starlark.psi.functions

import com.intellij.lang.ASTNode
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor

@ApiStatus.Internal
class StarlarkMandatoryParameter(node: ASTNode) : StarlarkParameter(node) {
  override fun acceptVisitor(visitor: StarlarkElementVisitor) = visitor.visitMandatoryParameter(this)
}
