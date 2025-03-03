package org.jetbrains.bazel.languages.bazelrc.quickfix

import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.codeInspection.util.IntentionFamilyName
import com.intellij.codeInspection.util.IntentionName
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.bazel.languages.bazelrc.flags.Flag
import org.jetbrains.bazel.languages.bazelrc.psi.BazelrcElementGenerator

class RenameFlagNameFix(element: PsiElement, val flag: Flag) : LocalQuickFixAndIntentionActionOnPsiElement(element) {
  override fun invoke(
    project: Project,
    file: PsiFile,
    editor: Editor?,
    startElement: PsiElement,
    endElement: PsiElement,
  ) {
    val target =
      when {
        startElement.text.startsWith("--no") -> "no${flag.option.name}"
        else -> flag.option.name
      }

    startElement.replace(BazelrcElementGenerator(project).createFlagName(target))
  }

  override fun getText(): @IntentionName String = """Replace with "${flag.option.name}" variant"""

  override fun getFamilyName(): @IntentionFamilyName String = "Replace old flag names with new ones."
}
