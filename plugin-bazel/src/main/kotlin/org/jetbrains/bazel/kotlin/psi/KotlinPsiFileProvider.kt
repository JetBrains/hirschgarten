package org.jetbrains.bazel.kotlin.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.bazel.languages.projectview.findusages.PsiFileProvider
import org.jetbrains.kotlin.psi.KtClassLikeDeclaration

/** Replaces top-level kotlin classes with their corresponding PsiFile  */
class KotlinPsiFileProvider : PsiFileProvider {
  override fun asFileSearch(elementToSearch: PsiElement): PsiFile? {
    var elementToSearch = elementToSearch
    if (elementToSearch is KtClassLikeDeclaration) {
      elementToSearch = elementToSearch.parent
    }
    return elementToSearch as? PsiFile
  }
}
