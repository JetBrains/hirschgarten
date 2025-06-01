package org.jetbrains.bazel.languages.starlark.psi.expressions

import com.intellij.lang.ASTNode
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement

abstract class StarlarkCompExpression(node: ASTNode) : StarlarkBaseElement(node) {
  fun getCompVariables(): List<StarlarkTargetExpression> =
    getCompVariableTuples().flatMap { it.getTargetExpressions() } +
      findChildrenByClass(StarlarkTargetExpression::class.java).toList()

  private fun getCompVariableTuples() =
    findChildrenByClass(StarlarkParenthesizedExpression::class.java).mapNotNull { it.getTuple() } +
      findChildrenByClass(StarlarkTupleExpression::class.java).toList()
}
