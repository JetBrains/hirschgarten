package org.jetbrains.bazel.languages.projectview.psi.sections

import com.intellij.lang.ASTNode
import com.intellij.psi.util.elementType
import org.jetbrains.bazel.languages.projectview.language.ProjectViewSection
import org.jetbrains.bazel.languages.projectview.lexer.ProjectViewTokenType
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewBaseElement
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewElementVisitor
import org.jetbrains.bazel.languages.projectview.references.ProjectViewFlagReference

@Suppress("UnstableApiUsage")
class ProjectViewPsiSectionItem(node: ASTNode) : ProjectViewBaseElement(node) {
  override fun acceptVisitor(visitor: ProjectViewElementVisitor) {
    visitor.visitSectionItem(this)
  }

  private fun isLeaf(): Boolean = firstChild != null && firstChild == lastChild && firstChild.elementType == ProjectViewTokenType.IDENTIFIER

  override fun getOwnReferences(): Collection<ProjectViewFlagReference> {
    val sectionName = (parent as? ProjectViewPsiSection)?.getKeyword()?.text ?: return listOf()
    if (sectionName.contains("flag") && ProjectViewSection.KEYWORD_MAP.contains(sectionName) && isLeaf()) {
      return listOf(ProjectViewFlagReference(this))
    }
    return listOf()
  }
}
