package org.jetbrains.bazel.python.gutter

import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.PyFile
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.python.run.PythonBazelRunUtils
import org.jetbrains.bazel.runnerAction.BazelRunnerActionDescriptor
import org.jetbrains.bazel.ui.gutters.BazelRunLineMarkerContributor

@ApiStatus.Internal
class BazelPyRunLineMarkerContributor : BazelRunLineMarkerContributor() {
  override fun getGutterAction(element: PsiElement): GutterAction? {
    if (element.containingFile !is PyFile || !PythonBazelRunUtils.isRunLineMarkerElement(element)) return null
    return GutterAction(
      icon = if (PythonBazelRunUtils.isTestFunctionNameIdentifier(element)) executeRunLineMarkerIcon else null,
      runnerActionDescriptor = BazelRunnerActionDescriptor(
        programArguments = PythonBazelRunUtils.getTestRunnerArguments(element),
      ),
    )
  }
}
