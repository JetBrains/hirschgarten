package org.jetbrains.bazel.run.synthetic

import com.intellij.lang.LanguageExtension
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.BuildTarget

interface SyntheticRunTargetTemplateGenerator {
  fun isSupported(target: BuildTarget): Boolean

  // TODO: remove this completely after action system refactor
  fun getRunnerActionName(original: String, target: BuildTarget, element: PsiElement): String
  fun getSyntheticTargetLabel(original: BuildTarget, element: PsiElement): Label
  fun getSyntheticParams(target: BuildTarget, element: PsiElement): Params
  fun createSyntheticTemplate(target: BuildTarget, params: Params): SyntheticRunTargetTemplate

  companion object {
    val ep: LanguageExtension<SyntheticRunTargetTemplateGenerator> = LanguageExtension("org.jetbrains.bazel.syntheticRunTargetTemplateGenerator")
  }

  data class Params(val data: String?)
}
