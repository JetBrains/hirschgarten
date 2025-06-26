package org.jetbrains.bazel.languages.projectview.psi.sections

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import org.jetbrains.bazel.languages.projectview.lexer.ProjectViewTokenType
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewBaseElement
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewElementVisitor
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType

class ProjectViewPsiSection(node: ASTNode) : ProjectViewBaseElement(node) {
  override fun acceptVisitor(visitor: ProjectViewElementVisitor) {
    visitor.visitSection(this)
  }

  fun getKeyword(): PsiElement = firstChild

  fun getItems(): Array<ProjectViewPsiSectionItem> = getChildrenOfType<ProjectViewPsiSectionItem>()

  fun getColon(): PsiElement? = getChildrenOfType<PsiElement>().find { it.elementType == ProjectViewTokenType.COLON }
}
