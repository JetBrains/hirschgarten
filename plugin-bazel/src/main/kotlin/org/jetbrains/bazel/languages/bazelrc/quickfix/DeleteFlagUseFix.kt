package org.jetbrains.bazel.languages.bazelrc.quickfix

import com.intellij.codeInspection.util.IntentionFamilyName
import com.intellij.modcommand.ActionContext
import com.intellij.modcommand.ModPsiUpdater
import com.intellij.modcommand.Presentation
import com.intellij.modcommand.PsiUpdateModCommandAction
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.languages.bazelrc.flags.Flag
import org.jetbrains.bazel.languages.bazelrc.psi.BazelrcFlag

class DeleteFlagUseFix(element: PsiElement, val flag: Flag) : PsiUpdateModCommandAction<PsiElement>(element) {
  override fun invoke(
    context: ActionContext,
    element: PsiElement,
    updater: ModPsiUpdater,
  ) {
    element.parentOfType<BazelrcFlag>()?.delete()
  }

  override fun getPresentation(context: ActionContext, element: PsiElement): Presentation? =
    Presentation.of(BazelPluginBundle.message("quickfix.bazelrc.delete.noop.flag.presentation"))

  override fun getFamilyName(): @IntentionFamilyName String = BazelPluginBundle.message("quickfix.bazelrc.delete.noop.flag.declaration")
}
