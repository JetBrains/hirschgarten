package org.jetbrains.bazel.python.gutter

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.PyIfStatement
import com.jetbrains.python.psi.PyUtil
import org.jetbrains.bazel.ui.gutters.BazelRunLineMarkerContributor

class BazelPyRunLineMarkerContributor : BazelRunLineMarkerContributor() {
  override fun PsiElement.shouldAddMarker(): Boolean = this.containingFile is PyFile && isIfNameMain()

  // It checks if a Psi element is a top level < if __name__ == "__main__" > python statement
  fun PsiElement.isIfNameMain(): Boolean {
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
