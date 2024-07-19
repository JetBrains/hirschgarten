package org.jetbrains.bazel.languages.starlark.psi.statements

import com.intellij.lang.ASTNode
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTargetExpression

class StarlarkAssignmentStatement(node: ASTNode) : StarlarkBaseElement(node) {
  override fun acceptVisitor(visitor: StarlarkElementVisitor) =
      visitor.visitAssignmentStatement(this)

  fun getTargetExpression(): StarlarkTargetExpression? =
      findChildByClass(StarlarkTargetExpression::class.java)

  override fun getName(): String? = getTargetExpression()?.name ?: super.getName()
}
