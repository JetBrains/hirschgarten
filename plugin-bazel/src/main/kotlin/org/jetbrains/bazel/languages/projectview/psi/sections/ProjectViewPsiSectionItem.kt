package org.jetbrains.bazel.languages.projectview.psi.sections

import com.intellij.lang.ASTNode
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewBaseElement
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewElementVisitor
import org.jetbrains.bazel.languages.projectview.references.ProjectViewFlagReference

@Suppress("UnstableApiUsage")
class ProjectViewPsiSectionItem(node: ASTNode) : ProjectViewBaseElement(node) {
  override fun acceptVisitor(visitor: ProjectViewElementVisitor) {
    visitor.visitSectionItem(this)
  }

  override fun getOwnReferences(): Collection<ProjectViewFlagReference> {
    val sectionName = (parent as? ProjectViewPsiSection)?.getKeyword() ?: return listOf()
    if (sectionName.text.contains("flag")) {
      return listOf(ProjectViewFlagReference(this))
    }
    return listOf()
  }
}
