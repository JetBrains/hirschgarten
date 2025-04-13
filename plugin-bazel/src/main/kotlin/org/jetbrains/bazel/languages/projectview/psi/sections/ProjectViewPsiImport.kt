package org.jetbrains.bazel.languages.projectview.psi.sections

import com.intellij.lang.ASTNode
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewElementVisitor
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewPsiTopLevel

class ProjectViewPsiImport(node: ASTNode) : ProjectViewPsiTopLevel(node) {
  override fun acceptVisitor(visitor: ProjectViewElementVisitor) {
    visitor.visitImport(this)
  }
}
