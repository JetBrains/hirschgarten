package org.jetbrains.bazel.languages.starlark.rename

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import com.intellij.util.containers.MultiMap
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.StarlarkNamedElement
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression

internal class StarlarkRenamePsiElementProcessor : RenamePsiElementProcessor() {

  override fun canProcessElement(element: PsiElement): Boolean = element is StarlarkElement

  /**
   * Effectively, limits the search scope to starlark files.
   * Without this we receive a search scope that is enlarged with [com.intellij.psi.search.UseScopeEnlarger],
   * which leads to unexpected renames in e.g., Markdown files.
   *
   * Analogous limitation must be done in [org.jetbrains.bazel.languages.starlark.findusages.StarlarkFindUsagesHandler] for consistency.
   */
  override fun findReferences(
    element: PsiElement,
    searchScope: SearchScope,
    searchInCommentsAndStrings: Boolean,
  ): Collection<PsiReference> = ReferencesSearch.search(element, searchScope.intersectWith(element.useScope)).findAll()

  override fun findExistingNameConflicts(
    element: PsiElement,
    newName: String,
    conflicts: MultiMap<PsiElement, String>,
  ) {
    when (element) {
      is StarlarkCallExpression -> findRuleTargetConflicts(element, newName, conflicts)
      is StarlarkNamedElement -> findNamedElementConflicts(element, newName, conflicts)
    }
  }

  private fun findRuleTargetConflicts(callExpr: StarlarkCallExpression, newName: String, conflicts: MultiMap<PsiElement, String>) {
    val buildFile = callExpr.containingFile as? StarlarkFile ?: return
    val existingTarget = buildFile.findTargetRule(newName) ?: return
    if (existingTarget != callExpr) {
      conflicts.putValue(existingTarget, StarlarkBundle.message("rename.target.conflict.already.exists", newName, buildFile.name))
    }
  }

  private fun findNamedElementConflicts(element: StarlarkNamedElement, newName: String, conflicts: MultiMap<PsiElement, String>) {
    val file = element.containingFile ?: return
    val useScope = element.useScope
    val scopeElements = (useScope as? LocalSearchScope)?.scope ?: arrayOf(file)
    val scopeName = (scopeElements.singleOrNull() as? StarlarkNamedElement)?.name ?: file.name
    scopeElements
      .asSequence()
      .flatMap { PsiTreeUtil.collectElementsOfType(it, StarlarkNamedElement::class.java) }
      .filter { it != element && it.name == newName }
      .filter { candidate ->
        // Only flag a conflict if the candidate is declared in the same or enclosing scope,
        // not in a nested subscope (comprehension, lambda) that doesn't actually conflict.
        val candidateScope = candidate.useScope
        candidateScope !is LocalSearchScope || candidateScope.containsRange(file, element.textRange)
      }
      .forEach { conflicting ->
        conflicts.putValue(conflicting, StarlarkBundle.message("rename.name.conflict.already.exists", newName, scopeName))
      }
  }
}
