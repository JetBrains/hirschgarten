package org.jetbrains.bazel.languages.projectview.psi.sections

import com.intellij.lang.ASTNode
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewElementVisitor
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewPsiItem

class ProjectViewPsiSectionItem(node: ASTNode) : ProjectViewPsiItem(node) {
  override fun acceptVisitor(visitor: ProjectViewElementVisitor) {
    visitor.visitSectionItem(this)
  }
}
