package org.jetbrains.bazel.languages.projectview.psi.sections

import com.intellij.lang.ASTNode
import com.intellij.psi.util.elementType
import org.jetbrains.bazel.languages.projectview.language.ProjectViewSection
import org.jetbrains.bazel.languages.projectview.lexer.ProjectViewTokenType
import com.intellij.psi.PsiReference
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewBaseElement
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewElementVisitor
import org.jetbrains.bazel.languages.projectview.references.ProjectViewFlagReference
import org.jetbrains.bazel.languages.projectview.references.ProjectViewLabelReference

@Suppress("UnstableApiUsage")
class ProjectViewPsiSectionItem(node: ASTNode) : ProjectViewBaseElement(node) {
  override fun acceptVisitor(visitor: ProjectViewElementVisitor) {
    visitor.visitSectionItem(this)
  }

  fun getSection(): ProjectViewPsiSection? = parent as? ProjectViewPsiSection

  override fun getOwnReferences(): Collection<ProjectViewFlagReference> {
    //if (!(firstChild != null && firstChild == lastChild && firstChild.elementType == ProjectViewTokenType.IDENTIFIER)) {
    //  return emptyList()
    //}
    //val sectionName = getSection()?.getKeyword()?.text ?: return emptyList()
    //if (sectionName.contains("flag") && ProjectViewSection.KEYWORD_MAP.contains(sectionName)) {
    //  return listOf(ProjectViewFlagReference(this))
    //}
    return emptyList()
  }

  override fun getReference(): PsiReference? {
    val sectionKeyword = getSection()?.getKeyword()?.text ?: return ProjectViewLabelReference(this)
    if (sectionKeyword == "targets") {
      return ProjectViewLabelReference(this)
    }
    return null
  }
}
