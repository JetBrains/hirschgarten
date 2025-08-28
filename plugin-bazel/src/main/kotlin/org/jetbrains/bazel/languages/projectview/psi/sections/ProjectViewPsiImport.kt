package org.jetbrains.bazel.languages.projectview.psi.sections

import com.intellij.lang.ASTNode
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewBaseElement
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewElementVisitor
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType

class ProjectViewPsiImport(node: ASTNode) : ProjectViewBaseElement(node) {
  override fun acceptVisitor(visitor: ProjectViewElementVisitor) {
    visitor.visitImport(this)
  }

  fun getImportPath(): ProjectViewPsiImportItem? = getChildOfType<ProjectViewPsiImportItem>()
}
