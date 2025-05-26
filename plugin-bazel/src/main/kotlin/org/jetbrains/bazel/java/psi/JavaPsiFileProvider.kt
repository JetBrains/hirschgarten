package org.jetbrains.bazel.java.psi

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import org.jetbrains.bazel.languages.projectview.findusages.PsiFileProvider

/** Replaces top-level java classes with their corresponding PsiFile  */
class JavaPsiFileProvider : PsiFileProvider {
  override fun asFileSearch(elementToSearch: com.intellij.psi.PsiElement): PsiFile? {
    var elementToSearch = elementToSearch
    if (elementToSearch is PsiClass) {
      elementToSearch = elementToSearch.parent
    }
    return elementToSearch as? PsiFile
  }
}
