package org.jetbrains.bazel.languages.starlark.rename

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiReference
import com.intellij.psi.search.SearchScope
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import com.intellij.psi.search.searches.ReferencesSearch

class StarlarkFileRenameProcessor: RenamePsiElementProcessor() {
  override fun canProcessElement(element: PsiElement): Boolean {
    return element is PsiFile
  }

  override fun findReferences(
    element: PsiElement,
    searchScope: SearchScope,
    searchInCommentsAndStrings: Boolean
  ): Collection<PsiReference> {
    return ReferencesSearch.search(element, searchScope).findAll()
  }
}
