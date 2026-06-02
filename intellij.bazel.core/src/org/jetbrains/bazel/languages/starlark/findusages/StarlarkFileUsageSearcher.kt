package org.jetbrains.bazel.languages.starlark.findusages

import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.GlobalSearchScopeUtil
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.search.searches.ReferencesSearch.SearchParameters
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.Processor
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkGlobExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression
import org.jetbrains.bazel.languages.starlark.references.BazelLabelReference
import org.jetbrains.bazel.languages.starlark.references.findBuildFile
import org.jetbrains.bazel.languages.starlark.references.resolveLabel
import org.jetbrains.bazel.languages.starlark.restrictToStarlarkFiles

/**
 * Finds file usages in Starlark files. Requires [StarlarkFileUseScopeEnlarger] to enlarge the scope of the [PsiFile] with Starlark files.
 */
internal class StarlarkFileUsageSearcher : QueryExecutorBase<PsiReference, SearchParameters>(true) {
  override fun processQuery(params: SearchParameters, processor: Processor<in PsiReference>) {
    if (!params.project.isBazelProject) return
    val file = params.elementToSearch as? PsiFile ?: return
    val scope = GlobalSearchScopeUtil
      .toGlobalSearchScope(params.effectiveSearchScope, params.project)
      .restrictToStarlarkFiles()
    if (!processExplicitLabelReferences(file, scope, processor)) return
    processGlobReferences(file, scope, processor)
  }

  /**
   * Uses the word index to find only Starlark files whose string literals might contain the target filename,
   * then checks those candidates for explicit label references.
   */
  private fun processExplicitLabelReferences(
    file: PsiFile,
    scope: GlobalSearchScope,
    processor: Processor<in PsiReference>,
  ): Boolean = PsiSearchHelper
    .getInstance(file.project)
    .processCandidateFilesForText(
      scope,
      UsageSearchContext.IN_STRINGS,
      true,
      file.name,
    ) { candidate ->
      val starlarkFile = candidate.findPsiFile(file.project) as? StarlarkFile ?: return@processCandidateFilesForText true
      PsiTreeUtil.processElements(starlarkFile, StarlarkStringLiteralExpression::class.java) { element ->
        !isReferringToFile(element, file, starlarkFile) || processor.process(BazelLabelReference(element, true))
      }
    }

  /**
   * Finds the BUILD file for the Bazel package to which [file] belongs to and checks if any glob in this BUILD file contains [file].
   *
   * It's enough to search through only single BUILD file due to the [glob restrictions](https://bazel.build/reference/be/functions#glob).
   * Globs are matching only files in current Bazel package and cannot cross the package boundaries.
   */
  private fun processGlobReferences(
    file: PsiFile,
    scope: GlobalSearchScope,
    processor: Processor<in PsiReference>,
  ) {
    val dir = file.virtualFile?.parent ?: return
    val buildFile = generateSequence(dir) { it.parent }.firstNotNullOfOrNull(::findBuildFile) ?: return
    if (!scope.contains(buildFile)) return
    val starlarkFile = buildFile.findPsiFile(file.project) as? StarlarkFile ?: return
    PsiTreeUtil.processElements(starlarkFile, StarlarkGlobExpression::class.java) { element ->
      val ref = when {
        element.getGlob()?.match(file.virtualFile) == true -> element.reference
        else -> null
      }
      ref == null || processor.process(ref)
    }
  }

  private fun isReferringToFile(
    literal: StarlarkStringLiteralExpression,
    targetFile: PsiFile,
    contextFile: PsiFile,
  ): Boolean {
    val label = Label.parseOrNull(literal.getStringContents()) ?: return false
    val contextVFile = contextFile.originalFile.virtualFile ?: return false
    val resolvedElement = resolveLabel(targetFile.project, label, contextVFile, acceptOnlyFileTarget = true)
    return resolvedElement is PsiFile && PsiManager.getInstance(literal.project).areElementsEquivalent(resolvedElement, targetFile)
  }
}
