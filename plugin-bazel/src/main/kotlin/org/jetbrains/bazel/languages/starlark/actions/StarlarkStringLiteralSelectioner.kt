package org.jetbrains.bazel.languages.starlark.actions

import com.intellij.codeInsight.editorActions.ExtendWordSelectionHandlerBase
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.startOffset
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression

class StarlarkStringLiteralSelectioner : ExtendWordSelectionHandlerBase() {
  override fun canSelect(element: PsiElement): Boolean =
    element is StarlarkStringLiteralExpression || element.parent is StarlarkStringLiteralExpression

  override fun select(
    element: PsiElement,
    editorText: CharSequence,
    cursorOffset: Int,
    editor: Editor,
  ): List<TextRange>? {
    val stringElement = element as? StarlarkStringLiteralExpression ?: element.parent as? StarlarkStringLiteralExpression ?: return null
    return listOf(stringElement.getStringContentsOffset().shiftRight(stringElement.startOffset))
  }
}
