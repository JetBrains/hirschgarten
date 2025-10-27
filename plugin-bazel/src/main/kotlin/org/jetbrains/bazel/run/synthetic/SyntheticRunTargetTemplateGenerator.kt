package org.jetbrains.bazel.run.synthetic

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.BuildTarget

interface SyntheticRunTargetTemplateGenerator {
  val id: String
  fun isSupported(target: BuildTarget): Boolean
  fun getSyntheticTargetLabel(original: BuildTarget, element: PsiElement): Label
  fun getSyntheticParams(target: BuildTarget, element: PsiElement): String
  fun createSyntheticTemplate(target: BuildTarget, params: String): SyntheticRunTargetTemplate

  companion object {
    val ep: ExtensionPointName<SyntheticRunTargetTemplateGenerator> =
      ExtensionPointName("org.jetbrains.bazel.syntheticRunTargetTemplateGenerator")
  }
}
