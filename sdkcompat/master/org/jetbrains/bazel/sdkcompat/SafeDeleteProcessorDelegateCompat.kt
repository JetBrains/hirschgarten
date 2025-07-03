package org.jetbrains.bazel.sdkcompat

import com.intellij.openapi.util.NlsContexts
import com.intellij.psi.PsiElement
import com.intellij.refactoring.safeDelete.SafeDeleteProcessorDelegate
import com.intellij.usageView.UsageInfo
import com.intellij.util.containers.MultiMap

abstract class SafeDeleteProcessorDelegateCompat : SafeDeleteProcessorDelegate {
  fun findConflictsCompat(
    element: PsiElement,
    allElementsToDelete: Array<out PsiElement?>,
  ): Collection<String> = listOf()

  override fun findConflicts(
    element: PsiElement,
    allElementsToDelete: Array<out PsiElement?>,
    usages: Array<out UsageInfo?>,
    conflicts: MultiMap<PsiElement?, @NlsContexts.DialogMessage String?>,
  ) {
    findConflictsCompat(element, allElementsToDelete)
      .forEach { conflicts.putValue(element, it) }
  }
}
