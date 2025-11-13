package org.jetbrains.bazel.run.synthetic

import com.intellij.lang.LanguageExtension
import com.intellij.openapi.components.BaseState
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.BuildTarget

interface SyntheticRunTargetTemplateGenerator {
  fun isSupported(target: BuildTarget): Boolean
  fun getRunnerActionName(original: String, target: BuildTarget, element: PsiElement): String
  fun getSyntheticTargetLabel(original: BuildTarget, element: PsiElement): Label
  fun getSyntheticParams(target: BuildTarget, element: PsiElement): SyntheticRunTargetParams
  fun createSyntheticTemplate(target: BuildTarget, params: SyntheticRunTargetParams): SyntheticRunTargetTemplate

  companion object {
    val ep: LanguageExtension<SyntheticRunTargetTemplateGenerator> = LanguageExtension("org.jetbrains.bazel.syntheticRunTargetTemplateGenerator")
  }
}

class SyntheticRunTargetParams : BaseState() {
  var data: String? by string()

  companion object {
    fun ofString(value: String) = SyntheticRunTargetParams().apply { data = value }
  }
}
