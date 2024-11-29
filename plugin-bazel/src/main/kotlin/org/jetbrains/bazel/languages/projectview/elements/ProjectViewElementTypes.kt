package org.jetbrains.bazel.languages.projectview.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewDirectoriesSection

class ProjectViewElementTypes {
  val DIRECTORIES_SECTION = ProjectViewElementType("DIRECTORIES_SECTION")
  fun createElement(node: ASTNode): PsiElement =
    when (val type = node.elementType) {
      DIRECTORIES_SECTION -> ProjectViewDirectoriesSection(node)
      else -> error("Unknown element type: $type")
    }
}
