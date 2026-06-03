package org.jetbrains.bazel.languages.starlark.psi.statements

import com.intellij.lang.ASTNode
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor

@ApiStatus.Internal
class StarlarkBreakStatement(node: ASTNode) : StarlarkBaseElement(node) {
  override fun acceptVisitor(visitor: StarlarkElementVisitor) = visitor.visitBreakStatement(this)
}
