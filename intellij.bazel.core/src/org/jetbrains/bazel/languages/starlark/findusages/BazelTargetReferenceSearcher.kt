package org.jetbrains.bazel.languages.starlark.findusages

import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScopeUtil
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.descendantsOfType
import com.intellij.util.Processor
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.label.Package
import org.jetbrains.bazel.languages.starlark.bazel.BazelFileType
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.isRuleTarget
import org.jetbrains.bazel.languages.starlark.repomapping.calculateLabel
import org.jetbrains.bazel.languages.starlark.restrictToStarlarkFiles

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
    val scope = GlobalSearchScopeUtil
      .toGlobalSearchScope(queryParameters.effectiveSearchScope, project)
      .restrictToStarlarkFiles(BazelFileType.BUILD)
    val ownBuildFile = element.containingFile?.originalFile?.virtualFile
    if (ownBuildFile != null
        && scope.contains(ownBuildFile)
        && !processTargetReferences(target = element, candidate = ownBuildFile, psiManager = psiManager, consumer = consumer)) return
    val text = computeTargetSearchText(targetName, element)
    PsiSearchHelper
      .getInstance(project)
      .processCandidateFilesForText(
        scope,
        UsageSearchContext.IN_STRINGS,
        true,
        text,
      ) { virtualFile ->
        if (virtualFile == ownBuildFile) return@processCandidateFilesForText true
        processTargetReferences(target = element, candidate = virtualFile, psiManager = psiManager, consumer = consumer)
      }
  }

  /**
   * Computes the search text for [PsiSearchHelper.processCandidateFilesForText].
   *
   * The word index finds files containing ALL words from the search text.
   * Using just the target name (e.g., "core") matches too many files.
   * Cross-package labels must contain the package path segments (e.g., "//lib/core:core"),
   * so we search for all package path segments plus the target name.
   * For shorthand labels like "//lib/bar" (meaning "//lib/bar:bar"), the package path
   * segments alone are sufficient to find the file.
   */
  private fun computeTargetSearchText(targetName: String, element: StarlarkCallExpression): String {
    val buildFile = element.containingFile?.originalFile?.virtualFile ?: return targetName
    val packagePath = calculateLabel(element.project, buildFile)?.packagePath as? Package ?: return targetName
    val searchWords = (packagePath.pathSegments + targetName).toSet()
    return searchWords.joinToString(separator = " ")
  }

  private fun processTargetReferences(
    target: StarlarkCallExpression,
    candidate: VirtualFile,
    psiManager: PsiManager,
    consumer: Processor<in PsiReference>,
  ): Boolean {
    val starlarkFile = psiManager.findFile(candidate) as? StarlarkFile ?: return true
    for (literal in starlarkFile.descendantsOfType<StarlarkStringLiteralExpression>()) {
      if (literal.isTargetNameAttributeValue()) continue
      if (!psiManager.pointsToTarget(candidate = literal, target = target)) continue
      val reference = literal.reference ?: continue
      if (!consumer.process(reference)) return false
    }
    return true
  }

  private fun PsiManager.pointsToTarget(candidate: StarlarkStringLiteralExpression, target: StarlarkCallExpression): Boolean {
    val targetName = target.getNameAttributeValue() ?: return false
    // quick exit to avoid reference resolution for unrelated strings
    if (!candidate.getStringContents().contains(targetName)) return false
    val resolvedRef = candidate.reference?.resolve() ?: return false
    return areElementsEquivalent(resolvedRef, target)
  }
}
