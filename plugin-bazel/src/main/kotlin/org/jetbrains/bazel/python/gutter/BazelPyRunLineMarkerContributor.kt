package org.jetbrains.bazel.python.gutter

import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.PyIfStatement
import com.jetbrains.python.psi.PyUtil
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bazel.ui.gutters.BazelRunLineMarkerContributor
import org.jetbrains.bsp.protocol.PythonBuildTarget
import kotlin.io.path.isRegularFile

class BazelPyRunLineMarkerContributor : BazelRunLineMarkerContributor() {
  override fun PsiElement.shouldAddMarker(): Boolean =
    containingFile is PyFile &&
    containingFile.isMainFileInAnyBuildTarget() &&
    isIfNameMain()

  // Check if this file is configured as a main application file for any bazel target.
  // Showing our gutter icon in other cases doesn't make sense, since clicking it would run another file.
  private fun PsiFile.isMainFileInAnyBuildTarget(): Boolean {
    val targetUtils = project.targetUtils
    val fileNioPath = virtualFile.toNioPathOrNull()
    return targetUtils.getTargetsForFile(virtualFile).any {
      val buildTarget = targetUtils.getBuildTargetForLabel(it) ?: return@any false
      val pythonBuildTargetData = buildTarget.data as? PythonBuildTarget ?: return@any false
      if (pythonBuildTargetData.hasMainFileDefined()) {
        pythonBuildTargetData.mainFile == fileNioPath
      } else if (pythonBuildTargetData.mainModule.isNullOrEmpty()) {
        // When both the main file and main module aren't defined, then py_binary and py_test rules
        // expect the file named exactly targetName + ".py".
        // See the "main" attribute documentation: https://bazel.build/reference/be/python#py_binary
        virtualFile.name == "${buildTarget.id.targetName}.py"
      } else {
        // Currently, I don't support finding the main file of a Python module.
        // After researching the public GitHub repositories, I decided that at this moment
        // it isn't worth the effort to support it.
        false
      }
    }
  }

  private fun PythonBuildTarget.hasMainFileDefined(): Boolean = mainFile != null && mainFile!!.isRegularFile()

  // It checks if a Psi element is a top level < if __name__ == "__main__" > python statement
  private fun PsiElement.isIfNameMain(): Boolean {
    // The built-in python gutter contributor places these only on the if keyword itself,
    // so we just add it to the same if keywork to overwrite it
    if (this.node.elementType != PyTokenTypes.IF_KEYWORD) {
      return false
    }
    var element: PsiElement = this
    while (true) {
      val ifStatement = PsiTreeUtil.getParentOfType(element, PyIfStatement::class.java) ?: break
      element = ifStatement
    }
    if (element is PyIfStatement) {
      return PyUtil.isIfNameEqualsMain(element)
    }
    return false
  }
}
