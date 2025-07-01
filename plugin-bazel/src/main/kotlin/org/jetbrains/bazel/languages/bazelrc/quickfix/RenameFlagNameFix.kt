package org.jetbrains.bazel.languages.bazelrc.quickfix

import com.intellij.codeInspection.util.IntentionFamilyName
import com.intellij.modcommand.ActionContext
import com.intellij.modcommand.ModPsiUpdater
import com.intellij.modcommand.Presentation
import com.intellij.modcommand.PsiUpdateModCommandAction
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.languages.bazelrc.flags.Flag
import org.jetbrains.bazel.languages.bazelrc.psi.BazelrcElementGenerator

class RenameFlagNameFix(element: PsiElement, val flag: Flag) : PsiUpdateModCommandAction<PsiElement>(element) {
  override fun invoke(
    context: ActionContext,
    element: PsiElement,
    updater: ModPsiUpdater,
  ) {
    val target =
      when {
        element.text.startsWith("--no") -> "no${flag.option.name}"
        else -> flag.option.name
      }

    element.replace(BazelrcElementGenerator(context.project).createFlagName(target))
  }

  override fun getPresentation(context: ActionContext, element: PsiElement): Presentation? =
    Presentation.of(BazelPluginBundle.message("quickfix.bazelrc.rename.flag.presentation", flag.option.name))

  override fun getFamilyName(): @IntentionFamilyName String = BazelPluginBundle.message("quickfix.bazelrc.rename.flag.description")
}
