package org.jetbrains.bazel.languages.starlark.refactoring

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.refactoring.RefactoringSettings
import com.intellij.refactoring.safeDelete.NonCodeUsageSearchInfo
import com.intellij.refactoring.safeDelete.SafeDeleteProcessor
import com.intellij.usageView.UsageInfo
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkFunctionDeclaration
import org.jetbrains.bazel.sdkcompat.SafeDeleteProcessorDelegateCompat

class StarlarkFunctionSafeDeleteProcessor : SafeDeleteProcessorDelegateCompat() {
  override fun handlesElement(element: PsiElement?): Boolean = element is StarlarkFunctionDeclaration

  override fun findUsages(
    element: PsiElement,
    allElementsToDelete: Array<out PsiElement?>,
    result: MutableList<in UsageInfo>,
  ): NonCodeUsageSearchInfo {
    SafeDeleteProcessor.findGenericElementUsages(element, result, allElementsToDelete,
                                                 GlobalSearchScope.projectScope(element.project))
    return NonCodeUsageSearchInfo(SafeDeleteProcessor.getDefaultInsideDeletedCondition(allElementsToDelete), element)
  }

  override fun getElementsToSearch(
    element: PsiElement,
    allElementsToDelete: Collection<PsiElement?>,
  ): Collection<PsiElement?> = listOf(element)

  override fun getAdditionalElementsToDelete(
    element: PsiElement,
    allElementsToDelete: Collection<PsiElement?>,
    askUser: Boolean,
  ): Collection<PsiElement?> = emptyList()

  override fun preprocessUsages(
    project: Project,
    usages: Array<out UsageInfo?>,
  ): Array<out UsageInfo?> = usages

  override fun prepareForDeletion(element: PsiElement) {

  }

  override fun isToSearchInComments(element: PsiElement?): Boolean =
    RefactoringSettings.getInstance().SAFE_DELETE_SEARCH_IN_COMMENTS

  override fun setToSearchInComments(element: PsiElement?, enabled: Boolean) {
    RefactoringSettings.getInstance().SAFE_DELETE_SEARCH_IN_COMMENTS = enabled
  }

  override fun isToSearchForTextOccurrences(element: PsiElement?): Boolean =
    RefactoringSettings.getInstance().SAFE_DELETE_SEARCH_IN_NON_JAVA

  override fun setToSearchForTextOccurrences(element: PsiElement?, enabled: Boolean) {
    RefactoringSettings.getInstance().SAFE_DELETE_SEARCH_IN_NON_JAVA = enabled
  }
}

