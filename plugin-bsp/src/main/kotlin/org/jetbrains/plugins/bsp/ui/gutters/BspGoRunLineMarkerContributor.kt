package org.jetbrains.plugins.bsp.ui.gutters

import com.goide.GoConstants
import com.goide.GoTypes
import com.goide.execution.GoRunUtil
import com.goide.execution.testing.GoTestFinder
import com.goide.psi.GoFile
import com.goide.psi.GoFunctionDeclaration
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType

public class BspGoRunLineMarkerContributor : BspRunLineMarkerContributor() {
  override fun PsiElement.shouldAddMarker(): Boolean =
    this.elementType == GoTypes.IDENTIFIER &&
      this.parent is GoFunctionDeclaration &&
      this.parent.parent is GoFile &&
      (isMainFunction() || GoTestFinder.isTestFile(this.containingFile))

  private fun PsiElement.isMainFunction(): Boolean =
    GoRunUtil.isMainGoFile(this.containingFile) &&
      GoConstants.MAIN == (this.parent as GoFunctionDeclaration).name
}
