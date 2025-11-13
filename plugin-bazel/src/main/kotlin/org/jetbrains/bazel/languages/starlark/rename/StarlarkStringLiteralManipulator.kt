package org.jetbrains.bazel.languages.starlark.rename

import com.intellij.openapi.util.TextRange
import com.intellij.psi.AbstractElementManipulator
import com.intellij.psi.impl.source.tree.LeafElement
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression

class StarlarkStringLiteralManipulator : AbstractElementManipulator<StarlarkStringLiteralExpression>() {
  override fun handleContentChange(
    element: StarlarkStringLiteralExpression,
    range: TextRange,
    newContent: String,
  ): StarlarkStringLiteralExpression {
    val stringLeafNode = element.node.firstChildNode as? LeafElement ?: return element
    if (!getRangeInElement(element).contains(range)) return element
    val oldText = element.text
    val newText = oldText.take(range.startOffset) + newContent + oldText.substring(range.endOffset)
    stringLeafNode.replaceWithText(newText)
    return element
  }

  override fun getRangeInElement(element: StarlarkStringLiteralExpression): TextRange = element.getStringContentsOffset()
}
