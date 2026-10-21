package org.jetbrains.bazel.clion.run

import com.intellij.psi.PsiElement
import com.jetbrains.cidr.cpp.runfile.CppFileEntryPointDetector
import org.jetbrains.bazel.clion.sync.includesCc
import org.jetbrains.bazel.ui.gutters.BazelRunConfigurationProducer
import org.jetbrains.bsp.protocol.BuildTarget

internal class BazelCcMainRunConfigurationProducer : BazelRunConfigurationProducer() {
  override fun getGutterAction(element: PsiElement, target: BuildTarget): GutterAction? {
    if (!target.kind.includesCc()) return null
    if (CppFileEntryPointDetector.getInstance()?.isMainOrIsInMain(element) != true) {
      return null
    }
    return GutterAction()
  }
}
