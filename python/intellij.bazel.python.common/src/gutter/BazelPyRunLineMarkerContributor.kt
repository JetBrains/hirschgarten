package org.jetbrains.bazel.python.gutter

import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.PyFile
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.python.run.PythonBazelRunUtils
import org.jetbrains.bazel.ui.gutters.BazelRunLineMarkerContributor
import javax.swing.Icon

@ApiStatus.Internal
class BazelPyRunLineMarkerContributor : BazelRunLineMarkerContributor() {
  override fun PsiElement.shouldAddMarker(): Boolean =
    containingFile is PyFile && PythonBazelRunUtils.isRunLineMarkerElement(this)

  override fun getRunLineMarkerIcon(element: PsiElement): Icon? =
    if (PythonBazelRunUtils.isTestFunctionNameIdentifier(element)) executeRunLineMarkerIcon else null

  override fun getExtraProgramArguments(element: PsiElement): List<String> =
    PythonBazelRunUtils.getTestRunnerArguments(element)
}
