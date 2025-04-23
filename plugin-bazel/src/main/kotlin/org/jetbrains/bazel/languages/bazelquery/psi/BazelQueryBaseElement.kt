package org.jetbrains.bazel.languages.bazelquery.psi

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElementVisitor

abstract class BazelQueryBaseElement(node: ASTNode) :
  ASTWrapperPsiElement(node),
  BazelQueryElement {
  override fun accept(visitor: PsiElementVisitor) {
    if (visitor is BazelQueryElementVisitor) {
      acceptVisitor(visitor)
    } else {
      super.accept(visitor)
    }
  }

  protected abstract fun acceptVisitor(visitor: BazelQueryElementVisitor)
}
