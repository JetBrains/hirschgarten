package org.jetbrains.bazel.languages.starlark.psi.statements

import com.intellij.lang.ASTNode
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor

class StarlarkLoadStatement(node: ASTNode) : StarlarkBaseElement(node) {
  override fun acceptVisitor(visitor: StarlarkElementVisitor) = visitor.visitLoadStatement(this)
}
