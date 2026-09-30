package org.jetbrains.bazel.languages.starlark.rename

import com.intellij.openapi.util.TextRange
import com.intellij.psi.AbstractElementManipulator
import com.intellij.psi.impl.source.tree.LeafElement
import org.jetbrains.bazel.languages.starlark.injection.StarlarkStringLiteralEscaper
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression

/**
 * Allows the platform to change the contents of a Starlark string literal, e.g. when renaming a referenced target or
 * when a fragment of a language injected into the literal is edited. New content is escaped as needed for the kind of
 * string literal. A raw string literal stays raw as long as the new content can be represented in it; otherwise (e.g.
 * if the content contains a quote), it is turned into a regular string literal with the same value.
 */
internal class StarlarkStringLiteralManipulator : AbstractElementManipulator<StarlarkStringLiteralExpression>() {
  override fun handleContentChange(
    element: StarlarkStringLiteralExpression,
    range: TextRange,
    newContent: String,
  ): StarlarkStringLiteralExpression {
    val stringLeafNode = element.node.firstChildNode as? LeafElement ?: return element
    val contentRange = getRangeInElement(element)
    if (!contentRange.contains(range)) return element
    val oldText = element.text
    val quote = element.getQuote()
    val newText =
      if (!element.isRaw()) {
        oldText.take(range.startOffset) + StarlarkStringLiteralEscaper.escape(newContent, quote) + oldText.substring(range.endOffset)
      } else {
        // Raw strings have no escape sequences, so the contents are the value.
        val newRawContent =
          oldText.substring(contentRange.startOffset, range.startOffset) + newContent +
            oldText.substring(range.endOffset, contentRange.endOffset)
        if (StarlarkStringLiteralEscaper.isRepresentableAsRaw(newRawContent, quote)) {
          oldText.take(range.startOffset) + newContent + oldText.substring(range.endOffset)
        } else {
          val prefix = oldText.substring(0, contentRange.startOffset - quote.quote.length).replace("r", "", ignoreCase = true)
          prefix + quote.wrap(StarlarkStringLiteralEscaper.escape(newRawContent, quote))
        }
      }
    stringLeafNode.replaceWithText(newText)
    return element
  }

  override fun getRangeInElement(element: StarlarkStringLiteralExpression): TextRange = element.getStringContentsOffset()
}
