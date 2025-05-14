package org.jetbrains.bazel.python.gutter

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.PyIfStatement
import com.jetbrains.python.psi.PyUtil
import org.jetbrains.bazel.ui.gutters.BspRunLineMarkerContributor

class BazelPyRunLineMarkerContributor : BspRunLineMarkerContributor() {
  override fun PsiElement.shouldAddMarker(): Boolean = this.containingFile is PyFile && isIfNameMain()

  // This is from com.jetbrains.python.run.RunnableScriptFilter.isIfNameMain
  // It check if a Psi element is a < if __name__ == "main" > python statement
  fun PsiElement.isIfNameMain(): Boolean {
    var element: PsiElement = this
    while (true) {
      val ifStatement = PsiTreeUtil.getParentOfType(element, PyIfStatement::class.java)
      if (ifStatement == null) {
        break
      }
      element = ifStatement
    }
    if (element is PyIfStatement) {
      return PyUtil.isIfNameEqualsMain(element)
    }
    return false
  }
}
