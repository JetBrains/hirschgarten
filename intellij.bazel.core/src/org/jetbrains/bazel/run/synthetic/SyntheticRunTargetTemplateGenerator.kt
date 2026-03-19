package org.jetbrains.bazel.run.synthetic

import com.intellij.lang.LanguageExtension
import com.intellij.psi.PsiElement
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.ExecutableTarget

@ApiStatus.Internal
interface SyntheticRunTargetTemplateGenerator {
  fun isSupported(target: ExecutableTarget): Boolean

  // TODO: remove this completely after action system refactor
  fun getRunnerActionName(original: String, target: ExecutableTarget, element: PsiElement): String
  fun getSyntheticTargetLabel(original: ExecutableTarget, element: PsiElement): Label
  fun getSyntheticParams(target: ExecutableTarget, element: PsiElement): Params
  fun createSyntheticTemplate(target: ExecutableTarget, params: Params): SyntheticRunTargetTemplate

  companion object {
    val ep: LanguageExtension<SyntheticRunTargetTemplateGenerator> = LanguageExtension("org.jetbrains.bazel.syntheticRunTargetTemplateGenerator")
  }

  data class Params(val data: String?)
}
