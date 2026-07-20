package com.intellij.bazel.python.backend

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.jetbrains.python.PythonLanguage
import com.jetbrains.python.psi.PyFromImportStatement
import com.jetbrains.python.psi.PyImportElement
import com.jetbrains.python.psi.PyImportStatement
import com.jetbrains.python.psi.PyReferenceExpression
import com.jetbrains.python.psi.impl.PyPsiUtils
import com.jetbrains.python.psi.types.PyModuleType
import org.jetbrains.bazel.config.isBazelProject

internal class BazelPythonImportCompletionContributor : CompletionContributor(), DumbAware {
  init {
    extend(CompletionType.BASIC, psiElement().withLanguage(PythonLanguage.INSTANCE), BazelPythonImportCompletionProvider)
  }
}

private object BazelPythonImportCompletionProvider : CompletionProvider<CompletionParameters>() {
  override fun addCompletions(
    parameters: CompletionParameters,
    context: ProcessingContext,
    result: CompletionResultSet,
  ) {
    val project = parameters.position.project
    if (!project.isBazelProject) return

    val importSource = findQualifiedImportSourceReference(parameters) ?: return
    val qualifier = PyPsiUtils.asQualifiedName(importSource)?.removeLastComponent() ?: return
    val children = project.service<PythonResolveIndexService>().findDirectChildren(qualifier)
    if (children.isEmpty()) return

    val psiManager = PsiManager.getInstance(project)
    val existingNames = hashSetOf<String>()
    for (path in children.values) {
      val virtualFile = VirtualFileManager.getInstance().findFileByNioPath(path) ?: continue
      val item =
        if (virtualFile.isDirectory) {
          psiManager.findDirectory(virtualFile)
        }
        else {
          psiManager.findFile(virtualFile)
        }
      if (item is PsiFileSystemItem) {
        PyModuleType.buildFileLookupElement(parameters.originalFile, item, existingNames)?.let(result::addElement)
      }
    }
  }

  /**
   * Finds the module-path reference expression at the caret in `from <here> import ...` or plain `import <here>`.
   * The imported-names list of `from X import <here>` also uses [PyImportElement], so it is explicitly excluded.
   */
  private fun findQualifiedImportSourceReference(parameters: CompletionParameters): PyReferenceExpression? {
    // Use `position` (completion copy with the dummy identifier), not `originalPosition`: right after a `.`
    // (e.g. `from lib.<caret>`) `originalPosition` is the whitespace after the dot and has no PyReferenceExpression
    // ancestor. The dummy is stripped by the caller via `asQualifiedName(...).removeLastComponent()`.
    val position = parameters.position
    val referenceExpression = PsiTreeUtil.getParentOfType(position, PyReferenceExpression::class.java, false) ?: return null

    val fromImportStatement = PsiTreeUtil.getParentOfType(referenceExpression, PyFromImportStatement::class.java)
    val importSource = fromImportStatement?.importSource
    if (importSource != null && PsiTreeUtil.isAncestor(importSource, referenceExpression, false)) return importSource

    val importElement = PsiTreeUtil.getParentOfType(referenceExpression, PyImportElement::class.java)
    if (importElement?.parent is PyImportStatement) {
      val importReference = importElement.importReferenceExpression
      if (importReference != null && PsiTreeUtil.isAncestor(importReference, referenceExpression, false)) return importReference
    }

    return null
  }
}
