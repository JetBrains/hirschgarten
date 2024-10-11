package org.jetbrains.bazel.languages.starlark.completion

import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.ThreeState
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkFloatLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkIntegerLiteralExpression
import org.jetbrains.bsp.sdkcompat.codeInsight.CompletionConfidenceAdapter

class StarlarkCompletionConfidence : CompletionConfidenceAdapter() {
  override fun shouldSkipAutopopupCompat(
    editor: Editor,
    contextElement: PsiElement,
    psiFile: PsiFile,
    offset: Int,
  ): ThreeState =
    when (contextElement.parent) {
      is StarlarkIntegerLiteralExpression, is StarlarkFloatLiteralExpression -> ThreeState.YES
      else -> ThreeState.UNSURE
    }
}
