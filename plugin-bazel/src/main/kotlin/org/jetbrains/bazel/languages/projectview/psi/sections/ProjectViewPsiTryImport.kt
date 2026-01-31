package org.jetbrains.bazel.languages.projectview.psi.sections

import com.intellij.lang.ASTNode
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewElementVisitor

class ProjectViewPsiTryImport(node: ASTNode) : ProjectViewPsiImportBase(node) {
  override fun acceptVisitor(visitor: ProjectViewElementVisitor) {
    visitor.visitTryImport(this)
  }

  override val isImportRequired: Boolean = false
}
