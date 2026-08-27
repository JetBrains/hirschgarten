package org.jetbrains.bazel.languages.starlark.folding

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.CustomFoldingBuilder
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.bazel.languages.starlark.elements.StarlarkElementTypes
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkListLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkParenthesizedExpression

/**
 * Folds Starlark expressions.
 *
 * The base class adds the custom folding regions, for example a `#region` and
 * `#endregion` comment pair.
 */
internal class StarlarkFoldingBuilder : CustomFoldingBuilder() {
  override fun buildLanguageFoldRegions(
    descriptors: MutableList<FoldingDescriptor>,
    root: PsiElement,
    document: Document,
    quick: Boolean,
  ) {
    val blockElements =
      listOf(
        PsiTreeUtil.findChildrenOfType(root, StarlarkParenthesizedExpression::class.java),
        PsiTreeUtil.findChildrenOfType(root, StarlarkListLiteralExpression::class.java),
        PsiTreeUtil.findChildrenOfType(root, StarlarkCallExpression::class.java),
      ).flatten()

    for (block in blockElements) {
      val startOffset = block.textRange.startOffset
      val endOffset = block.textRange.endOffset
      if (startOffset < endOffset) {
        descriptors.add(FoldingDescriptor(block.node, TextRange(startOffset, endOffset)))
      }
    }
  }

  override fun getLanguagePlaceholderText(node: ASTNode, range: TextRange): String =
    when (val psiElement = node.psi) {
      is StarlarkParenthesizedExpression -> "(...)"
      is StarlarkListLiteralExpression -> "[...]"
      is StarlarkCallExpression -> getCallExpressionPlaceholder(psiElement)
      else -> "{...}"
    }

  private fun getCallExpressionPlaceholder(callExpression: StarlarkCallExpression): String {
    val functionName = callExpression.getCalledFunctionName() ?: "unknown_rule"
    val targetName = callExpression.getNameAttributeValue() ?: ""
    return "$functionName($targetName)"
  }

  override fun isRegionCollapsedByDefault(node: ASTNode): Boolean = false

  /** Only a `#` comment can hold a custom folding marker. */
  override fun isCustomFoldingCandidate(node: ASTNode): Boolean = node.elementType == StarlarkTokenTypes.COMMENT

  /**
   * Limits a custom folding region to one file or to one statement list.
   *
   * A start marker and an end marker in two different function bodies do not make a region.
   */
  override fun isCustomFoldingRoot(node: ASTNode): Boolean =
    node.psi is StarlarkFile || node.elementType == StarlarkElementTypes.STATEMENT_LIST
}
