package org.jetbrains.bazel.languages.bazelrc.quickfix

import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.codeInspection.util.IntentionFamilyName
import com.intellij.codeInspection.util.IntentionName
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.parentOfType
import org.jetbrains.bazel.languages.bazelrc.flags.Flag
import org.jetbrains.bazel.languages.bazelrc.psi.BazelrcFlag

class DeleteFlagUseFix(element: PsiElement, val flag: Flag) : LocalQuickFixAndIntentionActionOnPsiElement(element) {
  override fun invoke(
    project: Project,
    file: PsiFile,
    editor: Editor?,
    startElement: PsiElement,
    endElement: PsiElement,
  ) {
    startElement.parentOfType<BazelrcFlag>()?.delete()
  }

  override fun getText(): @IntentionName String = """Delete NO_OP flag use"""

  override fun getFamilyName(): @IntentionFamilyName String = "Delete NO_OP flag declarations"
}
