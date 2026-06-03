package org.jetbrains.bazel.languages.starlark.findusages

import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReference
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScopeUtil
import com.intellij.psi.search.searches.ReferencesSearch.SearchParameters
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.Processor
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.starlark.StarlarkFileType
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkGlobExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkListLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkArgumentElement
import org.jetbrains.bazel.languages.starlark.references.BazelLabelReference
import org.jetbrains.bazel.languages.starlark.references.resolveLabel

/**
 * Finds file usages in Starlark files. Requires [StarlarkFileUseScopeEnlarger] to enlarge the scope of the [PsiFile] with Starlark files.
 */
internal class StarlarkFileUsageSearcher : QueryExecutorBase<PsiReference, SearchParameters>(true) {
  override fun processQuery(params: SearchParameters, processor: Processor<in PsiReference>) {
    if (!params.project.isBazelProject) return

    val psiManager = PsiManager.getInstance(params.project)
    val file = params.elementToSearch as? PsiFile ?: return
    val scope = GlobalSearchScopeUtil.toGlobalSearchScope(params.effectiveSearchScope, params.project)
    val starlarkFiles = FileTypeIndex.getFiles(StarlarkFileType, scope)
    if (starlarkFiles.isEmpty()) return

    starlarkFiles.forEach { virtualFile ->
      val starlarkFile = psiManager.findFile(virtualFile) as? StarlarkFile ?: return@forEach
      val baseDir = virtualFile.parent ?: return@forEach

      val shouldContinue = PsiTreeUtil.processElements(starlarkFile) { element ->
        when (element) {
          is StarlarkStringLiteralExpression -> {
            if (isReferringToFile(element, file, starlarkFile, psiManager)) {
              val ref = BazelLabelReference(element, true)
              if (!processor.process(ref)) {
                return@processElements false
              }
            }
          }

          is StarlarkGlobExpression -> {
            if (element.getGlob()?.match(file.virtualFile) == true) {
              val ref = element.reference
              if (!processor.process(ref)) {
                return@processElements false
              }
            }
          }
        }
        true
      }

      if (!shouldContinue) {
        return
      }
    }
  }

  private fun isReferringToFile(
    literal: StarlarkStringLiteralExpression,
    targetFile: PsiFile,
    contextFile: PsiFile,
    psiManager: PsiManager,
  ): Boolean {
    val label = Label.parseOrNull(literal.getStringContents()) ?: return false
    val contextVFile = contextFile.originalFile.virtualFile ?: return false
    val resolvedElement = resolveLabel(targetFile.project, label, contextVFile, acceptOnlyFileTarget = true)
    return when (resolvedElement) {
      is PsiFile -> psiManager.areElementsEquivalent(resolvedElement, targetFile)
      else -> false
    }
  }

  private fun extractPatterns(arg: StarlarkArgumentElement): List<String> {
    val literals =
      when (val expr = arg.lastChild) {
        is StarlarkStringLiteralExpression -> listOf(expr)
        is StarlarkListLiteralExpression -> expr.getElements().filterIsInstance<StarlarkStringLiteralExpression>()
        else -> emptyList()
      }
    return literals.map { it.getStringContents() }
  }
}
