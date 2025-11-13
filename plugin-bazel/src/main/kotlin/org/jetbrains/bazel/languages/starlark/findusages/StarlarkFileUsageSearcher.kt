package org.jetbrains.bazel.languages.starlark.findusages

import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.TextRange
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
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
import org.jetbrains.bazel.languages.starlark.references.resolveLabel
import java.io.IOException

class StarlarkFileUsageSearcher : QueryExecutorBase<PsiReference, SearchParameters>(true) {
  override fun processQuery(params: SearchParameters, processor: Processor<in PsiReference>) {
    if (!params.project.isBazelProject) return

    val psiManager = PsiManager.getInstance(params.project)
    val file = params.elementToSearch as? PsiFile ?: return
    val starlarkFiles = FileTypeIndex.getFiles(StarlarkFileType, GlobalSearchScope.projectScope(params.project))
    if (starlarkFiles.isEmpty()) return

    starlarkFiles.forEach { virtualFile ->
      val starlarkFile = psiManager.findFile(virtualFile) as? StarlarkFile ?: return
      val baseDir = starlarkFile.virtualFile.parent ?: return

      // Find all text occurrences that refer to the file
      PsiTreeUtil
        .collectElementsOfType(starlarkFile, StarlarkStringLiteralExpression::class.java)
        .filter { isReferringToFile(it, file, psiManager) }
        .forEach { processor.process(getReferenceToTextOccurrence(it, file, psiManager)) }

      // Find all glob() calls that refer to the file
      PsiTreeUtil
        .collectElementsOfType(starlarkFile, StarlarkCallExpression::class.java)
        .asSequence()
        .filter { it.firstChild?.text == "glob" }
        .mapNotNull { getReferenceToGlobCallOrNull(it, file, StarlarkGlob.forPath(baseDir)) }
        .forEach { processor.process(it) }
    }
  }

  private fun getReferenceToTextOccurrence(
    literal: StarlarkStringLiteralExpression,
    file: PsiFile,
    psiManager: PsiManager,
  ): PsiReferenceBase<StarlarkStringLiteralExpression> {
    val range = ElementManipulators.getValueTextRange(literal)
    val reference =
      object : PsiReferenceBase<StarlarkStringLiteralExpression>(literal, range) {
        override fun resolve(): PsiElement = file

        override fun isReferenceTo(element: PsiElement): Boolean = psiManager.areElementsEquivalent(resolve(), element)

        override fun handleElementRename(newElementName: String): PsiElement {
          val newContent = buildNewLabelContent(element.getStringContents(), newElementName)
          return ElementManipulators.getManipulator(element)?.handleContentChange(element, range, newContent) ?: element
        }
      }
    return reference
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

    return try {
      if (globBuilder
        .addPatterns(includePatterns)
        .addExcludes(excludePatterns)
        .glob()
        .contains(file.virtualFile)
      ) {
        PsiReferenceBase.Immediate(call, TextRange(0, call.textLength), file)
      } else {
        null
      }
    } catch (e: IOException) {
      Logger.getInstance(StarlarkGlob::class.java).warn(e.message)
      null
    } catch (_: InterruptedException) {
      Thread.currentThread().interrupt()
      null
    }
  }

  private fun buildNewLabelContent(oldContent: String, newFilename: String): String {
    val label = Label.parseOrNull(oldContent)
    val fileNameIndex =
      if (label != null && oldContent.contains(":")) {
        oldContent.lastIndexOf(':')
      } else {
        oldContent.lastIndexOf('/')
      }
    return if (fileNameIndex >= 0) oldContent.take(fileNameIndex + 1) + newFilename else newFilename
  }

  private fun isReferringToFile(
    literal: StarlarkStringLiteralExpression,
    targetFile: PsiFile,
    psiManager: PsiManager,
  ): Boolean {
    val label = Label.parseOrNull(literal.getStringContents()) ?: return false
    val contextFile = literal.containingFile.originalFile.virtualFile ?: return false
    val resolvedElement = resolveLabel(targetFile.project, label, contextFile, acceptOnlyFileTarget = true)
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
