package org.jetbrains.bazel.languages.starlark.psi

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import org.jetbrains.bazel.languages.starlark.StarlarkFileType
import org.jetbrains.kotlin.idea.base.util.restrictByFileType

abstract class StarlarkBaseElement(node: ASTNode) :
  ASTWrapperPsiElement(node),
  StarlarkElement {
  override fun accept(visitor: PsiElementVisitor) {
    if (visitor is StarlarkElementVisitor) {
      acceptVisitor(visitor)
    } else {
      super.accept(visitor)
    }
  }

  protected abstract fun acceptVisitor(visitor: StarlarkElementVisitor)

  override fun getUseScope(): SearchScope = GlobalSearchScope.projectScope(project).restrictByFileType(StarlarkFileType)
}
