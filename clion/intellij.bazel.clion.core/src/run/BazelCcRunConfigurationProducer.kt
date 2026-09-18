package org.jetbrains.bazel.clion.run

import com.intellij.psi.PsiElement
import com.jetbrains.cidr.cpp.runfile.CppFileEntryPointDetector
import org.jetbrains.bazel.clion.sync.CC_LANGUAGE_CLASS
import org.jetbrains.bazel.ui.gutters.BazelRunConfigurationProducer
import org.jetbrains.bsp.protocol.BuildTarget

internal class BazelCcRunConfigurationProducer : BazelRunConfigurationProducer() {

  override fun getGutterAction(element: PsiElement, target: BuildTarget): GutterAction? {
    if (CC_LANGUAGE_CLASS !in target.kind.languageClasses) {
      return null
    }
    if (CppFileEntryPointDetector.getInstance()?.isMainOrIsInMain(element) != true) {
      return null
    }
    return GutterAction()
  }
}
