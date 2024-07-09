package org.jetbrains.bazel.languages.starlark.psi.functions

import com.intellij.lang.ASTNode
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import org.jetbrains.bazel.languages.starlark.psi.StarlarkNamedElement

class StarlarkNamedParameter(node: ASTNode) : StarlarkNamedElement(node) {
  override fun acceptVisitor(visitor: StarlarkElementVisitor) = visitor.visitNamedParameter(this)
}
