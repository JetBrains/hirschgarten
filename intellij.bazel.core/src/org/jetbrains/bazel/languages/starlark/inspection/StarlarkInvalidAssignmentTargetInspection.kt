package org.jetbrains.bazel.languages.starlark.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.util.childrenOfType
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bazel.languages.starlark.StarlarkFileType
import org.jetbrains.bazel.languages.starlark.StarlarkUtils.nearestRelevantBeforeOperator
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkListLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkParenthesizedExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkReferenceExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStarExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkSubscriptionExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTupleExpression
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkAssignmentStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkAugAssignmentStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkForStatement

@ApiStatus.Internal
class StarlarkInvalidAssignmentTargetInspection : LocalInspectionTool() {
  override fun isAvailableForFile(file: PsiFile): Boolean = file.fileType is StarlarkFileType

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = InvalidAssignmentTargetVisitor(holder)

  private class InvalidAssignmentTargetVisitor(private val holder: ProblemsHolder) : StarlarkElementVisitor() {
    override fun visitAssignmentStatement(node: StarlarkAssignmentStatement) {
      node.leftHandSide()?.let(::searchForInvalidTargetAndReport)
    }

    override fun visitAugAssignmentStatement(node: StarlarkAugAssignmentStatement) {
      val lhs = node.leftHandSide() ?: return
      val target = unwrapParentheses(lhs)
      if (target is StarlarkTupleExpression || target is StarlarkListLiteralExpression) {
        holder.registerProblem(lhs, StarlarkBundle.message("inspection.description.assignment.target.augmented.sequence"))
        return
      }
      searchForInvalidTargetAndReport(lhs)
    }

    override fun visitForStatement(node: StarlarkForStatement) {
      node.leftHandSide()?.let(::searchForInvalidTargetAndReport)
    }

    private fun searchForInvalidTargetAndReport(target: PsiElement) {
      val invalid = firstInvalidTarget(target) ?: return
      holder.registerProblem(
        invalid,
        StarlarkBundle.message("inspection.description.assignment.target.invalid", invalid.text),
      )
    }

    private fun firstInvalidTarget(target: PsiElement): PsiElement? =
      when (target) {
        is StarlarkSubscriptionExpression,
        is StarlarkReferenceExpression -> null
        is StarlarkParenthesizedExpression -> {
          val inner = target.childrenOfType<StarlarkBaseElement>().singleOrNull()
          if (inner != null) firstInvalidTarget(inner) else target
        }

        is StarlarkTupleExpression,
        is StarlarkListLiteralExpression -> {
          var elem = nearestRelevantBeforeOperator(target.firstChild)
          while (elem != null) {
            firstInvalidElement(elem)?.let { return it }
            elem = nearestRelevantBeforeOperator(elem.nextSibling)
          }
          return null
        }

        else -> target
      }

    private fun unwrapParentheses(element: PsiElement): PsiElement {
      var current = element
      while (current is StarlarkParenthesizedExpression) {
        current = current.childrenOfType<StarlarkBaseElement>().singleOrNull() ?: return current
      }
      return current
    }

    /**
     * A starred target is valid only as an element of a tuple or a list, so only its operand is checked here.
     * StarlarkUnsupportedPythonSyntaxInspection reports the '*'.
     */
    private fun firstInvalidElement(element: PsiElement): PsiElement? =
      if (element is StarlarkStarExpression) element.childrenOfType<StarlarkBaseElement>().firstOrNull()?.let(::firstInvalidTarget)
      else firstInvalidTarget(element)
  }
}
