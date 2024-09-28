package org.jetbrains.bazel.languages.starlark.folding

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkListLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkParenthesizedExpression

class StarlarkFoldingBuilder : FoldingBuilderEx() {
  override fun buildFoldRegions(
    root: PsiElement,
    document: Document,
    quick: Boolean,
  ): Array<FoldingDescriptor> {
    val descriptors = mutableListOf<FoldingDescriptor>()

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
    return descriptors.toTypedArray()
  }

  override fun getPlaceholderText(node: ASTNode): String? =
    when (val psiElement = node.psi) {
      is StarlarkParenthesizedExpression -> "(...)"
      is StarlarkListLiteralExpression -> "[...]"
      is StarlarkCallExpression -> getCallExpressionPlaceholder(psiElement)
      else -> "{...}"
    }

  private fun getCallExpressionPlaceholder(callExpression: StarlarkCallExpression): String {
    val functionName = callExpression.getNameNode()?.text ?: "unknown_rule"
    val targetName = callExpression.getTargetName() ?: ""
    return "$functionName($targetName)"
  }

  override fun isCollapsedByDefault(node: ASTNode): Boolean = false
}
