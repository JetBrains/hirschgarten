package org.jetbrains.bazel.languages.projectview.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewList
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewListKey
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewListValue
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewScalar
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewScalarKey
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewScalarValue

object ProjectViewElementTypes {
  val LIST = ProjectViewElementType("LIST")
  val SCALAR = ProjectViewElementType("SCALAR")

  val SCALAR_KEY = ProjectViewElementType("SCALAR_KEY")
  val SCALAR_VALUE = ProjectViewElementType("SCALAR VALUE")

  val LIST_KEY = ProjectViewElementType("LIST_KEY")
  val LIST_VALUE = ProjectViewElementType("LIST_VALUE")

  fun createElement(node: ASTNode): PsiElement =
    when (val type = node.elementType) {
      LIST -> ProjectViewList(node)
      SCALAR -> ProjectViewScalar(node)

      SCALAR_KEY -> ProjectViewScalarKey(node)
      SCALAR_VALUE -> ProjectViewScalarValue(node)

      LIST_KEY -> ProjectViewListKey(node)
      LIST_VALUE -> ProjectViewListValue(node)

      else -> error("Unknown element type: $type")
    }
}
