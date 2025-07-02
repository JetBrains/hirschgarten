package org.jetbrains.bazel.languages.starlark.psi.statements

import com.intellij.lang.ASTNode
import com.intellij.util.Processor
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkParenthesizedExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTargetExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTupleExpression

class StarlarkAssignmentStatement(node: ASTNode) : StarlarkBaseElement(node) {
  override fun acceptVisitor(visitor: StarlarkElementVisitor) = visitor.visitAssignmentStatement(this)

  override fun getName(): String? = getTargetExpression()?.name ?: super.getName()

  fun check(processor: Processor<StarlarkElement>): Boolean =
    getTargetExpression()?.let { processor.process(it) }
      ?: getTupleExpression()?.getTargetExpressions()?.all { processor.process(it) }
      ?: getParenthesizedExpression()?.getTuple()?.getTargetExpressions()?.all { processor.process(it) }
      ?: true

  fun isTopLevel(): Boolean = parent is StarlarkFile

  private fun getTargetExpression(): StarlarkTargetExpression? = findChildByClass(StarlarkTargetExpression::class.java)

  private fun getTupleExpression(): StarlarkTupleExpression? = findChildByClass(StarlarkTupleExpression::class.java)

  private fun getParenthesizedExpression(): StarlarkParenthesizedExpression? = findChildByClass(StarlarkParenthesizedExpression::class.java)
}
