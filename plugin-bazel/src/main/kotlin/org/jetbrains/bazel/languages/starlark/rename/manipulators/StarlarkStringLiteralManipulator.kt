package org.jetbrains.bazel.languages.starlark.rename.manipulators

import com.intellij.openapi.util.TextRange
import com.intellij.psi.AbstractElementManipulator
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementFactory

class StarlarkStringLiteralManipulator : AbstractElementManipulator<StarlarkStringLiteralExpression>() {
  override fun handleContentChange(
    element: StarlarkStringLiteralExpression,
    range: TextRange,
    newContent: String
  ): StarlarkStringLiteralExpression {
    val oldText = element.text
    val newText = oldText.replaceRange(range.startOffset, range.endOffset, newContent)
    val newElement = StarlarkElementFactory.createStringLiteral(element.project, newText)
    return element.replace(newElement) as StarlarkStringLiteralExpression
  }

  override fun getRangeInElement(element: StarlarkStringLiteralExpression): TextRange {
    val text = element.text
    return TextRange(1, text.length - 1)
  }
}
