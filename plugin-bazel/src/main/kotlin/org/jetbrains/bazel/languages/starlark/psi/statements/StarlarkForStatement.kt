package org.jetbrains.bazel.languages.starlark.psi.statements

import com.intellij.lang.ASTNode
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTargetExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTupleExpression

class StarlarkForStatement(node: ASTNode) : StarlarkBaseElement(node) {
  override fun acceptVisitor(visitor: StarlarkElementVisitor) = visitor.visitForStatement(this)

  fun getLoopVariables(): List<StarlarkTargetExpression> =
    getLoopVariable()?.let(::listOf) ?: getLoopTuple()?.getTargetExpressions() ?: emptyList()

  private fun getLoopVariable(): StarlarkTargetExpression? = findChildByClass(StarlarkTargetExpression::class.java)

  private fun getLoopTuple(): StarlarkTupleExpression? = findChildByClass(StarlarkTupleExpression::class.java)
}
