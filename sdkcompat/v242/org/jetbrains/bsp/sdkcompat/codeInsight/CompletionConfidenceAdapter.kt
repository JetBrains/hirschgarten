package org.jetbrains.bsp.sdkcompat.codeInsight

import com.intellij.codeInsight.completion.CompletionConfidence
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.ThreeState
import org.jetbrains.kotlin.idea.codeinsight.utils.findExistingEditor

// v243: editor parameter was added
abstract class CompletionConfidenceAdapter : CompletionConfidence() {
  override fun shouldSkipAutopopup(
    element: PsiElement,
    file: PsiFile,
    offset: Int,
  ): ThreeState = shouldSkipAutopopupCompat(file.findExistingEditor()!!, element, file, offset)

  abstract fun shouldSkipAutopopupCompat(
    editor: Editor,
    element: PsiElement,
    file: PsiFile,
    offset: Int,
  ): ThreeState
}
