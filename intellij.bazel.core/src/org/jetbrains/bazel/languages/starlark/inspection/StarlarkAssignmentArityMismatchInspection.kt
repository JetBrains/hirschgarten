package org.jetbrains.bazel.languages.starlark.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bazel.languages.starlark.StarlarkFileType
import org.jetbrains.bazel.languages.starlark.StarlarkUtils.relevantChildrenAroundEach
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCompExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkDictCompExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkListCompExpression
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkAssignmentStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkForStatement
import org.jetbrains.bazel.languages.starlark.utils.StarlarkAssignmentUtils.destructuringElements
import org.jetbrains.bazel.languages.starlark.utils.StarlarkAssignmentUtils.destructuringTargets

@ApiStatus.Internal
class StarlarkAssignmentArityMismatchInspection : LocalInspectionTool() {
  override fun isAvailableForFile(file: PsiFile): Boolean = file.fileType is StarlarkFileType

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = AssignmentArityMismatchVisitor(holder)

  private class AssignmentArityMismatchVisitor(private val holder: ProblemsHolder) : StarlarkElementVisitor() {
    override fun visitAssignmentStatement(node: StarlarkAssignmentStatement) {
      val left = node.leftHandSide() ?: return
      val right = node.rightHandSide() ?: return
      checkAssignmentArity(left, right)
    }

    override fun visitForStatement(node: StarlarkForStatement) {
      val left = node.leftHandSide() ?: return
      val right = node.rightHandSide() ?: return
      checkIterationTargetArity(left, right)
    }

    override fun visitListCompExpression(node: StarlarkListCompExpression) = checkComprehensionArity(node)

    override fun visitDictCompExpression(node: StarlarkDictCompExpression) = checkComprehensionArity(node)

    private fun checkComprehensionArity(node: StarlarkCompExpression) {
      for ((left, right) in relevantChildrenAroundEach(node, StarlarkTokenTypes.IN_KEYWORD)) {
        checkIterationTargetArity(left, right)
      }
    }

    private fun checkIterationTargetArity(left: PsiElement, iterable: PsiElement) {
      val iteratedValues = destructuringElements(iterable) ?: return

      for (iteratedValue in iteratedValues) {
        checkAssignmentArity(left, iteratedValue)
      }
    }

    private fun checkAssignmentArity(left: PsiElement, right: PsiElement): Boolean {
      val leftElements = destructuringTargets(left) ?: return true
      val rightElements = destructuringElements(right) ?: return true
      val leftArity = leftElements.size
      val rightArity = rightElements.size

      if (leftArity != rightArity) {
        holder.registerProblem(
          left,
          StarlarkBundle.message("inspection.description.assignment.arity.mismatch", leftArity, rightArity),
        )
        return false
      }

      var isValid = true
      for (index in leftElements.indices) {
        if (!checkAssignmentArity(leftElements[index], rightElements[index])) isValid = false
      }

      return isValid
    }
  }
}
