package org.jetbrains.bazel.kotlin.psi

import org.jetbrains.bazel.languages.projectview.findusages.PsiFileProvider
import org.jetbrains.kotlin.psi.KtClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

/** Replaces top-level kotlin classes with their corresponding PsiFile  */
class KotlinPsiFileProvider : PsiFileProvider {
  override fun asFileSearch(elementToSearch: PsiElement): PsiFile? {
    var elementToSearch = elementToSearch
    if (elementToSearch is KtClass) {
      elementToSearch = elementToSearch.parent
    }
    return elementToSearch as? PsiFile
  }
}
