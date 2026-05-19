package org.jetbrains.bazel.languages.starlark.findusages

import com.intellij.find.findUsages.FindUsagesHandler
import com.intellij.find.findUsages.FindUsagesHandlerFactory
import com.intellij.find.findUsages.FindUsagesOptions
import com.intellij.openapi.application.ReadAction
import com.intellij.psi.PsiElement
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.searches.ReferencesSearch.SearchParameters
import org.jetbrains.bazel.languages.starlark.psi.expressions.isRuleTarget

internal class BazelTargetFindUsagesHandlerFactory : FindUsagesHandlerFactory() {

  override fun canFindUsages(element: PsiElement): Boolean = element.isRuleTarget()

  override fun createFindUsagesHandler(
    element: PsiElement,
    forHighlightUsages: Boolean,
  ): FindUsagesHandler = BazelTargetFindUsagesHandler(element)
}

private class BazelTargetFindUsagesHandler(element: PsiElement) : FindUsagesHandler(element) {

  override fun isSearchForTextOccurrencesAvailable(psiElement: PsiElement, isSingleFile: Boolean): Boolean = false

  /**
   * Effectively, limits the search scope to starlark files.
   * Without this we might receive a search scope that is enlarged with [com.intellij.psi.search.UseScopeEnlarger],
   * which leads to unnecessary noise like e.g., Markdown file results.
   *
   * Analogous limitation must be done in [org.jetbrains.bazel.languages.starlark.rename.BazelTargetRenamePsiElementProcessor] for consistency.
   */
  override fun createSearchParameters(
    target: PsiElement,
    searchScope: SearchScope,
    findUsagesOptions: FindUsagesOptions?,
  ): SearchParameters {
    val scope = ReadAction
      .nonBlocking<SearchScope> { searchScope.intersectWith(target.useScope) }
      .executeSynchronously()
    return SearchParameters(target, scope, false, findUsagesOptions?.fastTrack)
  }
}
