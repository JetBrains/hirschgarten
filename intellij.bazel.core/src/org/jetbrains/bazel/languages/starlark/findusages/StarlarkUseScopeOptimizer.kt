package org.jetbrains.bazel.languages.starlark.findusages

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.search.DelegatingGlobalSearchScope
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ScopeOptimizer
import com.intellij.psi.search.SearchScope
import org.jetbrains.bazel.languages.starlark.index.StarlarkLoadsIndex
import org.jetbrains.bazel.languages.starlark.psi.StarlarkNamedElement

/**
 * Narrows the use scope to only the defining file and files that actually load the searched Starlark element.
 */
internal class StarlarkUseScopeOptimizer : ScopeOptimizer {

  override fun getRestrictedUseScope(element: PsiElement): SearchScope? {
    val namedElement = element as? StarlarkNamedElement ?: return null
    val baseScope = element.useScope as? GlobalSearchScope ?: return null
    return StarlarkLoadersScope(baseScope, namedElement)
  }
}

/**
 * A search scope that wraps the [baseScope] and limits it to files that actually load the [element].
 */
private class StarlarkLoadersScope(
  baseScope: GlobalSearchScope,
  private val element: StarlarkNamedElement,
) : DelegatingGlobalSearchScope(baseScope) {

  override fun contains(file: VirtualFile): Boolean {
    if (!super.contains(file)) return false
    if (file == element.containingFile?.virtualFile) return true
    return StarlarkLoadsIndex.isLoaded(element, fileScope(element.project, file))
  }
}
