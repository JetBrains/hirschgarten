package org.jetbrains.bazel.languages.starlark.findusages

import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch.SearchParameters
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.Processor
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.starlark.StarlarkFileType
import org.jetbrains.bazel.languages.starlark.globbing.StarlarkGlob
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkListLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkArgumentElement
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkNamedArgumentExpression
import org.jetbrains.bazel.languages.starlark.references.BazelLabelReference
import org.jetbrains.bazel.languages.starlark.references.resolveLabel

internal class StarlarkFileUsageSearcher : QueryExecutorBase<PsiReference, SearchParameters>(true) {
  override fun processQuery(params: SearchParameters, processor: Processor<in PsiReference>) {
    if (!params.project.isBazelProject) return

    val psiManager = PsiManager.getInstance(params.project)
    val file = params.elementToSearch as? PsiFile ?: return
    val starlarkFiles = FileTypeIndex.getFiles(StarlarkFileType, GlobalSearchScope.projectScope(params.project))
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
          is StarlarkCallExpression -> {
            if (element.firstChild?.text == "glob") {
              val ref = getReferenceToGlobCallOrNull(element, file, StarlarkGlob.forPath(baseDir))
              if (ref != null && !processor.process(ref)) {
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

  private fun getReferenceToGlobCallOrNull(
    call: StarlarkCallExpression,
    file: PsiFile,
    globBuilder: StarlarkGlob.Builder,
  ): PsiReferenceBase<StarlarkCallExpression>? {
    val arguments = call.getArgumentList()?.getArguments() ?: return null

    val includeArgument = arguments.firstOrNull { it !is StarlarkNamedArgumentExpression || it.name == "include" } ?: return null
    val includePatterns = extractPatterns(includeArgument)
    if (includePatterns.isEmpty()) return null

    val excludeArgument = arguments.filterIsInstance<StarlarkNamedArgumentExpression>().firstOrNull { it.name == "exclude" }
    val excludePatterns = excludeArgument?.let { extractPatterns(it) } ?: emptyList()

    val files = globBuilder
      .addPatterns(includePatterns)
      .addExcludes(excludePatterns)
      .glob()
    if (!files.contains(file.virtualFile)) {
      return null
    }
    return PsiReferenceBase.Immediate(call, TextRange(0, call.textLength), file)
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
