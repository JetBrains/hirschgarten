package org.jetbrains.bazel.languages.starlark.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.TokenSet
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bazel.languages.starlark.StarlarkFileType
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenSets
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkListLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkParenthesizedExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkReferenceExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkSliceExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkSubscriptionExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTargetExpression
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
      selectLeftHandSideOfAssignment(node)?.let(::searchForInvalidTargetAndReport)
    }

    override fun visitAugAssignmentStatement(node: StarlarkAugAssignmentStatement) {
      val lhs = selectLeftHandSideOfAssignment(node) ?: return
      if (lhs is StarlarkTupleExpression || lhs is StarlarkListLiteralExpression) {
        holder.registerProblem(lhs, StarlarkBundle.message("inspection.description.assignment.target.augmented.sequence"))
        return
      }
      searchForInvalidTargetAndReport(lhs)
    }

    override fun visitForStatement(node: StarlarkForStatement) {
      val loopVars = node.getLoopVariables()
      if (loopVars.isNotEmpty()) loopVars.forEach(::searchForInvalidTargetAndReport)
      else selectLeftHandSideOfAssignment(node)?.let(::searchForInvalidTargetAndReport)
    }

    private fun selectLeftHandSideOfAssignment(node: PsiElement): PsiElement? = nearestRelevantBeforeOperator(node.firstChild)

    private fun searchForInvalidTargetAndReport(target: PsiElement) {
      val invalid = firstInvalidTarget(target) ?: return
      holder.registerProblem(
        invalid,
        StarlarkBundle.message("inspection.description.assignment.target.invalid", invalid.text),
      )
    }

    private fun firstInvalidTarget(target: PsiElement): PsiElement? =
      when (target) {
        is StarlarkTargetExpression,
        is StarlarkSubscriptionExpression,
        is StarlarkReferenceExpression,
        is StarlarkSliceExpression -> null

        is StarlarkParenthesizedExpression -> {
          val tuple = target.getTuple()
          if (tuple != null) firstInvalidTarget(tuple) else target
        }

        is StarlarkTupleExpression,
        is StarlarkListLiteralExpression -> {
          var elem = nearestRelevantBeforeOperator(target.firstChild)
          while (elem != null) {
            firstInvalidTarget(elem)?.let{ return it }
            elem = nearestRelevantBeforeOperator(elem.nextSibling)
          }
          return null
        }

        else -> target
      }

    private fun nearestRelevantBeforeOperator(node: PsiElement?): PsiElement? {
      var current = node
      while (current != null) {
        val elementType = current.node?.elementType
        if (ASSIGNMENT_OPERATORS.contains(elementType)) return null
        if (current is StarlarkBaseElement && !WHITESPACES_AND_COMMENTS.contains(elementType)) return current
        current = current.nextSibling
      }
      return null
    }

    companion object {
      private val ASSIGNMENT_OPERATORS = TokenSet.create(
        StarlarkTokenTypes.EQ,
        *StarlarkTokenSets.COMPOUND_ASSIGN_OPERATIONS.types,
        StarlarkTokenTypes.IN_KEYWORD,
      )

      private val WHITESPACES_AND_COMMENTS = TokenSet.orSet(StarlarkTokenSets.WHITESPACE, StarlarkTokenSets.COMMENT)
    }
  }
}
