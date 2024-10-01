package org.jetbrains.bazel.languages.starlark.findusages

import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.search.searches.ReferencesSearch.SearchParameters
import com.intellij.util.Processor
import org.jetbrains.bazel.languages.starlark.StarlarkFileType
import org.jetbrains.bazel.languages.starlark.psi.StarlarkNamedElement

class StarlarkStringUsageSearcher : QueryExecutorBase<PsiReference, SearchParameters>() {
  override fun processQuery(params: SearchParameters, processor: Processor<in PsiReference>) {
    val searchee = params.elementToSearch as? StarlarkNamedElement ?: return
    val name = searchee.name ?: return
    val userScope = params.scopeDeterminedByUser as? GlobalSearchScope ?: return
    val searchScope = GlobalSearchScope.getScopeRestrictedByFileTypes(userScope, StarlarkFileType)
    params.optimizer.searchWord(name, searchScope, UsageSearchContext.IN_STRINGS, true, searchee)
  }
}
