package org.jetbrains.bazel.languages.projectview.findusages

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

/**
 * Checks if the PsiElement represents a top-level PsiFile (e.g. a top-level java PsiClass can be
 * interchanged with the corresponding PsiFile, when searching for usages).
 */
interface PsiFileProvider {
  fun asFileSearch(elementToSearch: PsiElement): PsiFile?

  companion object {
    val EP_NAME = ExtensionPointName.create<PsiFileProvider>("org.jetbrains.bazel.psiFileProvider")
  }
}
