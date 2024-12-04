package org.jetbrains.bazel.languages.projectview.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.projectview.psi.sections.*

object ProjectViewElementTypes {
  val LIST = ProjectViewElementType("LIST")
  val SCALAR = ProjectViewElementType("SCALAR")

  val SCALAR_KEY = ProjectViewElementType("SCALAR_KEY")
  val SCALAR_VALUE = ProjectViewElementType("SCALAR VALUE")

  fun createElement(node: ASTNode): PsiElement =
    when (val type = node.elementType) {
      LIST -> ProjectViewList(node)
      SCALAR -> ProjectViewScalar(node)

      SCALAR_KEY -> ProjectViewScalarKey(node)
      SCALAR_VALUE -> ProjectViewScalarValue(node)

      else -> error("Unknown element type: $type")
    }
}
