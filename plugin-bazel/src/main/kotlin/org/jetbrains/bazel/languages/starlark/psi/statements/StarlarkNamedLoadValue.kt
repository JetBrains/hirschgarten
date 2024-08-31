package org.jetbrains.bazel.languages.starlark.psi.statements

import com.intellij.lang.ASTNode
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import org.jetbrains.bazel.languages.starlark.psi.StarlarkNamedElement

class StarlarkNamedLoadValue(node: ASTNode) :
  StarlarkNamedElement(node),
  StarlarkLoadValue {
  override fun acceptVisitor(visitor: StarlarkElementVisitor) = visitor.visitNamedLoadValue(this)
}
