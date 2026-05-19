package org.jetbrains.bazel.languages.starlark.findusages

import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReference
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScopeUtil
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.descendantsOfType
import com.intellij.util.Processor
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.languages.starlark.StarlarkFileType
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.isRuleTarget

/**
 * Finds all references to a Bazel target name (e.g., `":lib"`, `"//core:lib"`) across Starlark files.
 */
internal class BazelTargetReferenceSearcher : QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters>(true) {

  override fun processQuery(queryParameters: ReferencesSearch.SearchParameters, consumer: Processor<in PsiReference>) {
    val project = queryParameters.project
    if (!project.isBazelProject) return
    val element = queryParameters.elementToSearch
    if (!element.isRuleTarget()) return
    val targetName = element.getNameAttributeValue() ?: return
    if (targetName.isEmpty()) return
    val psiManager = PsiManager.getInstance(project)
    val scope = GlobalSearchScopeUtil.toGlobalSearchScope(queryParameters.effectiveSearchScope, project)
    for (virtualFile in FileTypeIndex.getFiles(StarlarkFileType, scope)) {
      val starlarkFile = psiManager.findFile(virtualFile) as? StarlarkFile ?: continue
      for (literal in starlarkFile.descendantsOfType<StarlarkStringLiteralExpression>()) {
        if (literal.isTargetNameAttributeValue()) continue
        if (!psiManager.pointsToTarget(candidate = literal, target = element)) continue
        val reference = literal.reference ?: continue
        if (!consumer.process(reference)) return
      }
    }
  }

  private fun PsiManager.pointsToTarget(candidate: StarlarkStringLiteralExpression, target: StarlarkCallExpression): Boolean {
    val targetName = target.getNameAttributeValue() ?: return false
    // quick exit to avoid reference resolution for unrelated strings
    if (!candidate.getStringContents().contains(targetName)) return false
    val resolvedRef = candidate.reference?.resolve() ?: return false
    return areElementsEquivalent(resolvedRef, target)
  }
}
