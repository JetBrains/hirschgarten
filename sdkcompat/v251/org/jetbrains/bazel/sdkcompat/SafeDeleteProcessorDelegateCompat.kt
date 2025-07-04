package org.jetbrains.bazel.sdkcompat

import com.intellij.psi.PsiElement
import com.intellij.refactoring.safeDelete.SafeDeleteProcessorDelegate

abstract class SafeDeleteProcessorDelegateCompat : SafeDeleteProcessorDelegate {
  fun findConflictsCompat(
    element: PsiElement,
    allElementsToDelete: Array<out PsiElement?>,
  ): Collection<String> = emptyList()

  override fun findConflicts(
    element: PsiElement,
    allElementsToDelete: Array<out PsiElement?>,
  ): Collection<String> = findConflictsCompat(element, allElementsToDelete)
}
