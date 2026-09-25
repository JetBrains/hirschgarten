package org.jetbrains.bazel.languages.starlark.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.elementType
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bazel.languages.starlark.StarlarkFileType
import org.jetbrains.bazel.languages.starlark.StarlarkUtils
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkBinaryExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkDoubleStarExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkGeneratorExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStarExpression

@ApiStatus.Internal
class StarlarkUnsupportedPythonSyntaxInspection : LocalInspectionTool() {
  override fun isAvailableForFile(file: PsiFile): Boolean = file.fileType is StarlarkFileType

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = UnsupportedSyntaxVisitor(holder)

  private class UnsupportedSyntaxVisitor(private val holder: ProblemsHolder) : StarlarkElementVisitor() {
    override fun visitElement(element: PsiElement) {
      if (element.firstChild == null && UNSUPPORTED_KEYWORDS.contains(element.elementType))
        holder.registerProblem(
          element,
          StarlarkBundle.message("inspection.description.unsupported.keyword", element.text),
        )
    }

    override fun visitGeneratorExpression(node: StarlarkGeneratorExpression) {
      holder.registerProblem(
        node,
        StarlarkBundle.message("inspection.description.unsupported.generator.expression"),
      )
    }

    override fun visitStarExpression(node: StarlarkStarExpression) {
      holder.registerProblem(
        node,
        StarlarkBundle.message("inspection.description.unsupported.unpacking.expression"),
      )
    }

    override fun visitDoubleStarExpression(node: StarlarkDoubleStarExpression) {
      holder.registerProblem(
        node,
        StarlarkBundle.message("inspection.description.unsupported.unpacking.expression"),
      )
    }

    override fun visitBinaryExpression(node: StarlarkBinaryExpression) {
      val operator = node.getOperatorElement() ?: return
      when {
        operator.elementType == StarlarkTokenTypes.EXP ->
          holder.registerProblem(
            operator,
            StarlarkBundle.message("inspection.description.unsupported.binary.operator", operator.text),
          )
        COMPARISON_OPERATORS.contains(operator.elementType) && node.isPartOfChainedComparison() ->
          holder.registerProblem(
            node,
            node.operatorProblemRange(operator),
            StarlarkBundle.message("inspection.description.unsupported.chained.comparison"),
          )
      }
    }

    private fun StarlarkBinaryExpression.isPartOfChainedComparison(): Boolean =
      getLeftOperand().isComparisonExpression() || getRightOperand().isComparisonExpression()

    private fun PsiElement?.isComparisonExpression(): Boolean {
      val binaryExpression = this as? StarlarkBinaryExpression ?: return false
      return binaryExpression.getOperator()?.let(COMPARISON_OPERATORS::contains) == true
    }

    private fun StarlarkBinaryExpression.operatorProblemRange(operator: PsiElement): TextRange {
      val endElement = when (operator.elementType) {
        StarlarkTokenTypes.NOT_KEYWORD -> operator.nextRelevantSibling()?.takeIf { it.elementType == StarlarkTokenTypes.IN_KEYWORD } ?: operator
        else -> operator
      }
      return TextRange(operator.textRange.startOffset - textRange.startOffset, endElement.textRange.endOffset - textRange.startOffset)
    }

    private fun PsiElement.nextRelevantSibling(): PsiElement? {
      var current = nextSibling
      while (current != null) {
        if (current.isRelevant()) return current
        current = current.nextSibling
      }
      return null
    }

    private fun PsiElement.isRelevant(): Boolean =
      this !is PsiErrorElement && this !is PsiWhiteSpace && elementType != null &&
      !StarlarkUtils.STARLARK_WHITESPACES_AND_COMMENTS.contains(elementType)
  }

  companion object {
    private val UNSUPPORTED_KEYWORDS = TokenSet.create(
      StarlarkTokenTypes.AS_KEYWORD,
      StarlarkTokenTypes.ASSERT_KEYWORD,
      StarlarkTokenTypes.ASYNC_KEYWORD,
      StarlarkTokenTypes.AWAIT_KEYWORD,
      StarlarkTokenTypes.CLASS_KEYWORD,
      StarlarkTokenTypes.DEL_KEYWORD,
      StarlarkTokenTypes.EXCEPT_KEYWORD,
      StarlarkTokenTypes.FINALLY_KEYWORD,
      StarlarkTokenTypes.FROM_KEYWORD,
      StarlarkTokenTypes.GLOBAL_KEYWORD,
      StarlarkTokenTypes.IMPORT_KEYWORD,
      StarlarkTokenTypes.IS_KEYWORD,
      StarlarkTokenTypes.NONLOCAL_KEYWORD,
      StarlarkTokenTypes.RAISE_KEYWORD,
      StarlarkTokenTypes.TRY_KEYWORD,
      StarlarkTokenTypes.WHILE_KEYWORD,
      StarlarkTokenTypes.WITH_KEYWORD,
      StarlarkTokenTypes.YIELD_KEYWORD,
    )

    private val COMPARISON_OPERATORS = TokenSet.create(
      StarlarkTokenTypes.LT,
      StarlarkTokenTypes.LE,
      StarlarkTokenTypes.GT,
      StarlarkTokenTypes.GE,
      StarlarkTokenTypes.EQEQ,
      StarlarkTokenTypes.NE,
      StarlarkTokenTypes.IN_KEYWORD,
      StarlarkTokenTypes.NOT_KEYWORD,
    )
  }
}
