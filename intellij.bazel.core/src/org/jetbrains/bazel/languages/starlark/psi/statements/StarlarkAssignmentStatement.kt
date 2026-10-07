package org.jetbrains.bazel.languages.starlark.psi.statements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.util.Processor
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.StarlarkUtils.nearestRelevantBeforeOperator
import org.jetbrains.bazel.languages.starlark.StarlarkUtils.selectLeftHandSideOfAssignment
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkListLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkParenthesizedExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTargetExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTupleExpression

@ApiStatus.Internal
class StarlarkAssignmentStatement(node: ASTNode) : StarlarkBaseElement(node) {
  override fun acceptVisitor(visitor: StarlarkElementVisitor) = visitor.visitAssignmentStatement(this)

  override fun getName(): String? = getTargetExpression()?.name ?: super.getName()

  fun check(processor: Processor<StarlarkElement>): Boolean =
    processTargets(selectLeftHandSideOfAssignment(this), processor)

  fun isTopLevel(): Boolean = parent is StarlarkFile

  private fun processTargets(element: PsiElement?, processor: Processor<StarlarkElement>): Boolean =
    when (element) {
      null -> true
      is StarlarkTargetExpression -> processor.process(element)
      is StarlarkParenthesizedExpression -> processTargets(element.getTuple(), processor)
      is StarlarkTupleExpression -> element.getTargetExpressions().all { processor.process(it) }
      is StarlarkListLiteralExpression -> {
        var child = nearestRelevantBeforeOperator(element.firstChild)
        while (child != null) {
          if (!processTargets(child, processor)) return false
          child = nearestRelevantBeforeOperator(child.nextSibling)
        }
        true
      }
      else -> true
    }

  private fun getTargetExpression(): StarlarkTargetExpression? = findChildByClass(StarlarkTargetExpression::class.java)
}
