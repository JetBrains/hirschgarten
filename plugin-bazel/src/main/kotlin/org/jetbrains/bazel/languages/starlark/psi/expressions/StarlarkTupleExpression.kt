package org.jetbrains.bazel.languages.starlark.psi.expressions

import com.intellij.lang.ASTNode
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor

class StarlarkTupleExpression(node: ASTNode) : StarlarkBaseElement(node) {
  override fun acceptVisitor(visitor: StarlarkElementVisitor) = visitor.visitTupleExpression(this)

  fun getTargetExpressions(): List<StarlarkTargetExpression> =
    findChildrenByClass(StarlarkTargetExpression::class.java).toList() +
      getNestedTuples().flatMap { it.getTargetExpressions() }

  private fun getNestedTuples(): List<StarlarkTupleExpression> =
    findChildrenByClass(StarlarkParenthesizedExpression::class.java).mapNotNull { it.getTuple() } +
      findChildrenByClass(StarlarkTupleExpression::class.java).toList()
}
