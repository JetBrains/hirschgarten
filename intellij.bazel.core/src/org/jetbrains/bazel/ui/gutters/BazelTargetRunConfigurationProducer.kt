package org.jetbrains.bazel.ui.gutters

import com.intellij.psi.PsiElement
import org.jetbrains.bsp.protocol.BuildTarget

internal class BazelRunConfigurationFromTargetProducer : BazelRunConfigurationProducer() {
  override fun getGutterAction(
    element: PsiElement,
    target: BuildTarget,
  ): GutterAction? {
    return if (element is PsiBazelTarget) {
      GutterAction()
    }
    else {
      null
    }
  }
}
