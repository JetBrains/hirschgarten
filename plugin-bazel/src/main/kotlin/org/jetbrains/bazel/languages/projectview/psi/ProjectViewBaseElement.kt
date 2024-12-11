package org.jetbrains.bazel.languages.projectview.psi

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElementVisitor

abstract class ProjectViewBaseElement(node: ASTNode) :
  ASTWrapperPsiElement(node),
  ProjectViewElement {
  override fun accept(visitor: PsiElementVisitor) {
    if (visitor is ProjectViewElementVisitor) {
      acceptVisitor(visitor)
    } else {
      super.accept(visitor)
    }
  }

  protected abstract fun acceptVisitor(visitor: ProjectViewElementVisitor)

  override fun getName(): String? = node.text
}
