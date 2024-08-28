package org.jetbrains.bazel.languages.starlark.psi.statements

import com.intellij.lang.ASTNode
import com.intellij.util.Processor
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTargetExpression

class StarlarkAssignmentStatement(node: ASTNode) : StarlarkBaseElement(node) {
  override fun acceptVisitor(visitor: StarlarkElementVisitor) = visitor.visitAssignmentStatement(this)

  override fun getName(): String? = getTargetExpression()?.name ?: super.getName()

  fun check(processor: Processor<StarlarkElement>): Boolean = getTargetExpression()?.let { processor.process(it) } ?: true

  private fun getTargetExpression(): StarlarkTargetExpression? = findChildByClass(StarlarkTargetExpression::class.java)
}
