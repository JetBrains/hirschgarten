package org.jetbrains.bazel.languages.starlark.findusages

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Computable
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

class StarlarkFileUsageSearcher : QueryExecutorBase<PsiReference, SearchParameters>(true) {
  override fun processQuery(params: SearchParameters, processor: Processor<in PsiReference>) {
    val file = params.elementToSearch as? PsiFile ?: return
    if (!file.project.isBazelProject) return

    val project = file.project
    val psiManager = PsiManager.getInstance(project)
    val projectScope = GlobalSearchScope.projectScope(project)

    val starlarkFiles =
      ApplicationManager.getApplication().runReadAction(
        Computable { FileTypeIndex.getFiles(StarlarkFileType, projectScope) },
      )
    if (starlarkFiles.isEmpty()) return

    starlarkFiles.forEach { virtualFile ->
      ApplicationManager.getApplication().runReadAction {
        val starlarkFile = psiManager.findFile(virtualFile) as? StarlarkFile ?: return@runReadAction
        val baseDir = starlarkFile.virtualFile.parent ?: return@runReadAction

        // Find all text occurrences that refer to the file
        PsiTreeUtil
          .collectElementsOfType(starlarkFile, StarlarkStringLiteralExpression::class.java)
          .filter { isReferringToFile(it, file, psiManager) }
          .forEach { processTextOccurrence(it, file, processor, psiManager) }

        // Find all glob() calls that refer to the file
        PsiTreeUtil
          .collectElementsOfType(starlarkFile, StarlarkCallExpression::class.java)
          .filter { it.firstChild?.text == "glob" }
          .forEach { processGlobCall(it, file, processor, baseDir) }
      }
    }
  }

  private fun processTextOccurrence(
    literal: StarlarkStringLiteralExpression,
    file: PsiFile,
    processor: Processor<in PsiReference>,
    psiManager: PsiManager,
  ) {
    val range = ElementManipulators.getValueTextRange(literal)
    val reference =
      object : PsiReferenceBase<StarlarkStringLiteralExpression>(literal, range) {
        override fun resolve(): PsiElement = file

        override fun isReferenceTo(element: PsiElement): Boolean = psiManager.areElementsEquivalent(resolve(), element)

        override fun handleElementRename(newElementName: String): PsiElement {
          val manipulator = ElementManipulators.getManipulator(element)
          val contentRange = manipulator?.getRangeInElement(element) ?: return element
          val newContent = buildNewLabelContent(element.getStringContents(), newElementName)
          return manipulator.handleContentChange(element, contentRange, newContent) ?: element
        }
      }
    processor.process(reference)
  }

  private fun processGlobCall(
    call: StarlarkCallExpression,
    file: PsiFile,
    processor: Processor<in PsiReference>,
    baseDir: com.intellij.openapi.vfs.VirtualFile,
  ) {
    val arguments = call.getArgumentList()?.getArguments() ?: return

    val includeArgument = arguments.firstOrNull { it !is StarlarkNamedArgumentExpression || it.name == "include" }
    val (includePatterns, includeLiterals) = includeArgument?.let { extractPatternsAndLiterals(it) } ?: return
    if (includePatterns.isEmpty()) return

    val excludeArgument = arguments.filterIsInstance<StarlarkNamedArgumentExpression>().firstOrNull { it.name == "exclude" }
    val excludePatterns = excludeArgument?.let { extractPatternsAndLiterals(it).first } ?: emptyList()

    try {
      val includedFiles =
        StarlarkGlob.forPath(baseDir).addPatterns(includePatterns).glob()
      if (!includedFiles.contains(file.virtualFile)) return

      val excludedFiles =
        if (excludePatterns.isNotEmpty()) {
          StarlarkGlob.forPath(baseDir).addPatterns(excludePatterns).glob()
        } else {
          emptyList()
        }
      if (excludedFiles.contains(file.virtualFile)) return
    } catch (e: Exception) {
      Logger.getInstance(StarlarkGlob::class.java).warn(e.message)
    }

    includeLiterals.forEach { literal ->
      val textRange = ElementManipulators.getValueTextRange(literal)
      processor.process(PsiReferenceBase.Immediate(literal, textRange, file))
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

  private fun extractPatternsAndLiterals(arg: StarlarkArgumentElement): Pair<List<String>, List<StarlarkStringLiteralExpression>> {
    val literals =
      when (val expr = arg.lastChild) {
        is StarlarkStringLiteralExpression -> listOf(expr)
        is StarlarkListLiteralExpression -> expr.getElements().filterIsInstance<StarlarkStringLiteralExpression>()
        else -> emptyList()
      }
    return literals.map { it.getStringContents() } to literals
  }
}
