package org.jetbrains.bazel.languages.projectview.psi.sections

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.childrenOfType
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.projectview.lexer.ProjectViewTokenType
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewBaseElement
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewElementVisitor

@ApiStatus.Internal
class ProjectViewPsiSection(node: ASTNode) : ProjectViewBaseElement(node) {
  override fun acceptVisitor(visitor: ProjectViewElementVisitor) {
    visitor.visitSection(this)
  }

  fun getKeyword(): PsiElement = firstChild

  fun getItems(): List<ProjectViewPsiSectionItem> = childrenOfType<ProjectViewPsiSectionItem>()

  fun getColon(): PsiElement? = findChildByType(ProjectViewTokenType.COLON)

  companion object {

    fun findByName(psi: PsiElement, name: String): ProjectViewPsiSection? = psi.childrenOfType<ProjectViewPsiSection>()
      .firstOrNull { it.getKeyword().text.trim() == name }
  }
}
