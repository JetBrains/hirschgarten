package org.jetbrains.bazel.run.synthetic

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.psi.PsiElement
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.BuildTargetInfo

@ApiStatus.Internal
interface SyntheticRunTargetTemplateGenerator {
  fun isSupported(target: BuildTargetInfo): Boolean

  fun getSyntheticTargetLabel(original: BuildTargetInfo, element: PsiElement): Label
  fun getSyntheticParams(target: BuildTargetInfo, element: PsiElement): Params
  fun createSyntheticTemplate(target: BuildTargetInfo, params: Params): SyntheticRunTargetTemplate

  companion object {
    val ep: LanguageExtension<SyntheticRunTargetTemplateGenerator> = LanguageExtension("org.jetbrains.bazel.syntheticRunTargetTemplateGenerator")

    /**
     * Just one generator, because showing several gutters for the same target doesn't make sense
     */
    fun getTemplateGenerator(target: BuildTargetInfo, language: Language): SyntheticRunTargetTemplateGenerator? =
      ep.allForLanguage(language)
        .firstOrNull { it.isSupported(target) }
  }

  data class Params(val data: String?)
}
