package org.jetbrains.bazel.languages.projectview.psi.sections

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiReference
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewBaseElement
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewElementVisitor
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType

class ProjectViewPsiSectionItem(node: ASTNode) : ProjectViewBaseElement(node) {
  override fun acceptVisitor(visitor: ProjectViewElementVisitor) {
    visitor.visitSectionItem(this)
  }

  override fun getReference(): PsiReference? {
    return if (getParentOfType<ProjectViewPsiSection>(true)?.getKeyword()?.text == "targets") {
      BazelLabelReference(this)
    } else {
      null
    }
  }
}
