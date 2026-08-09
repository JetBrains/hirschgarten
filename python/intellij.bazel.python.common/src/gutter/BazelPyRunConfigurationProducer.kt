package org.jetbrains.bazel.python.gutter

import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.PyFile
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.python.run.PythonBazelRunUtils
import org.jetbrains.bazel.ui.gutters.BazelRunConfigurationProducer
import org.jetbrains.bsp.protocol.BuildTarget

@ApiStatus.Internal
class BazelPyRunConfigurationProducer : BazelRunConfigurationProducer() {
  override fun getGutterAction(element: PsiElement, target: BuildTarget): GutterAction? {
    if (element.containingFile !is PyFile || !PythonBazelRunUtils.isRunLineMarkerElement(element)) return null
    return GutterAction(
      programArguments = PythonBazelRunUtils.getTestRunnerArguments(element),
    )
  }
}
