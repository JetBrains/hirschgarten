package org.jetbrains.bazel.languages.starlark.refactoring

import com.intellij.lang.refactoring.RefactoringSupportProvider
import com.intellij.psi.PsiElement

class StarlarkRefactoringSupportProvider : RefactoringSupportProvider() {
  override fun isSafeDeleteAvailable(element: PsiElement): Boolean = true
}
