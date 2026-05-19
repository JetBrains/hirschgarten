package org.jetbrains.bazel.languages.starlark.rename

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import com.intellij.util.containers.MultiMap
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.isRuleTarget

internal class BazelTargetRenamePsiElementProcessor : RenamePsiElementProcessor() {

  override fun canProcessElement(element: PsiElement): Boolean = element.isRuleTarget()

  /**
   * Effectively, limits the search scope to starlark files.
   * Without this we receive a search scope that is enlarged with [com.intellij.psi.search.UseScopeEnlarger],
   * which leads to unexpected renames in e.g., Markdown files.
   *
   * Analogous limitation must be done in [org.jetbrains.bazel.languages.starlark.findusages.BazelTargetFindUsagesHandler] for consistency.
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
    val callExpr = element as? StarlarkCallExpression ?: return
    val buildFile = callExpr.containingFile as? StarlarkFile ?: return
    val existingTarget = buildFile.findRuleTarget(newName) ?: return
    if (existingTarget != callExpr) {
      conflicts.putValue(existingTarget, StarlarkBundle.message("rename.target.conflict.already.exists", newName, buildFile.name))
    }
  }
}
