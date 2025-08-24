package org.jetbrains.bazel.languages.projectview.psi.sections

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiReference
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewBaseElement
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewElementVisitor
import org.jetbrains.bazel.languages.projectview.references.ProjectViewLabelReference

class ProjectViewPsiSectionItem(node: ASTNode) : ProjectViewBaseElement(node) {
  override fun acceptVisitor(visitor: ProjectViewElementVisitor) {
    visitor.visitSectionItem(this)
  }

  private fun getSection(): ProjectViewPsiSection? = parent as? ProjectViewPsiSection

  override fun getReference(): PsiReference? {
    val sectionKeyword = getSection()?.getKeyword()?.text ?: return ProjectViewLabelReference(this)
    if (sectionKeyword == "targets") {
      return ProjectViewLabelReference(this)
    }
    return null
  }
}
