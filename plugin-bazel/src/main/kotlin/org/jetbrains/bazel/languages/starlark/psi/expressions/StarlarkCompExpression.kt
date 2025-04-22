package org.jetbrains.bazel.languages.starlark.psi.expressions

import com.intellij.lang.ASTNode
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement

abstract class StarlarkCompExpression(node: ASTNode) : StarlarkBaseElement(node) {
  fun getCompVariables(): List<StarlarkTargetExpression> =
    getCompVariable()?.let(::listOf) ?: getCompTuple()?.getTargetExpressions() ?: emptyList()

  private fun getCompVariable(): StarlarkTargetExpression? = findChildByClass(StarlarkTargetExpression::class.java)

  private fun getCompTuple(): StarlarkTupleExpression? =
    findChildByClass(StarlarkTupleExpression::class.java)
      ?: findChildByClass(StarlarkParenthesizedExpression::class.java)?.getTuple()
}
