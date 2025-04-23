package org.jetbrains.bazel.flow.modify.ideStarter

import com.intellij.codeInsight.daemon.impl.quickfix.OrderEntryFix
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ui.playback.PlaybackContext
import com.intellij.openapi.ui.playback.commands.PlaybackCommandCoroutineAdapter
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiReference
import com.intellij.psi.util.parents

class ApplyOrderEntryQuickFixCommand(text: String, line: Int) : PlaybackCommandCoroutineAdapter(text, line) {
  companion object {
    const val PREFIX = CMD_PREFIX + "applyOrderEntryQuickFix"
  }

  override suspend fun doExecute(context: PlaybackContext) {
    val project = context.project
    val editor = readAction { checkNotNull(FileEditorManager.getInstance(project).selectedTextEditor) }
    val psiFile = readAction { checkNotNull(PsiDocumentManager.getInstance(project).getPsiFile(editor.document)) }
    val fix =
      readAction {
        val psiElement = checkNotNull(psiFile.findElementAt(editor.caretModel.offset)) { "Can't get the PSI element" }
        val psiReference =
          checkNotNull(psiElement.parents(withSelf = true).filterIsInstance<PsiReference>().firstOrNull()) {
            "PSI element ${psiElement.text} contains no reference"
          }
        val fixes = mutableListOf<IntentionAction>()
        OrderEntryFix.registerFixes(psiReference, fixes)
        val hint = extractCommandArgument(PREFIX)
        val fixesMatchingHint = fixes.filter { it.text.startsWith(hint) }
        val fix =
          checkNotNull(fixesMatchingHint.singleOrNull()) {
            "Expected 1 quick fix matching hint \"$hint\" out of ${fixes.map { it.text }}, got ${fixesMatchingHint.size}"
          }
        fix
      }
    writeAction {
      fix.invoke(project, editor, psiFile)
    }
  }
}
