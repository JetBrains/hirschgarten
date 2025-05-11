package org.jetbrains.bazel.languages.projectview.psi.sections

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewBaseElement
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewElementVisitor

class ProjectViewPsiSection(node: ASTNode) : ProjectViewBaseElement(node) {
  fun getKeyword(): String = firstChild.text

  fun getItems(): List<PsiElement> = children.toList().filter { it is ProjectViewPsiSectionItem }

  override fun acceptVisitor(visitor: ProjectViewElementVisitor) {
    visitor.visitSection(this)
  }
}
