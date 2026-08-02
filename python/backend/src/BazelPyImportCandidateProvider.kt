package com.intellij.bazel.python.backend

import com.intellij.openapi.components.service
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiReference
import com.intellij.psi.util.QualifiedName
import com.jetbrains.python.codeInsight.imports.AutoImportQuickFix
import com.jetbrains.python.codeInsight.imports.PyImportCandidateProvider
import com.jetbrains.python.psi.PyUtil
import com.jetbrains.python.psi.search.PySearchUtilBase
import com.jetbrains.python.psi.stubs.PyClassNameIndex
import com.jetbrains.python.psi.stubs.PyFunctionNameIndex
import com.jetbrains.python.psi.stubs.PyVariableNameIndex
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.config.isBazelProject

/** Offers Python import quickfix suggestions for Bazel dependencies */
@ApiStatus.Internal
class BazelPyImportCandidateProvider : PyImportCandidateProvider {
  override fun addImportCandidates(reference: PsiReference, name: String, quickFix: AutoImportQuickFix) {
    val node = reference.element
    val project = node.project
    if (!project.isBazelProject) return

    val indexService = project.service<PythonResolveIndexService>()
    val defaultSuggestionScope = PySearchUtilBase.defaultSuggestionScope(node)

    // expand the scope to contain third-party dependencies
    val scope = defaultSuggestionScope.uniteWith(indexService.getStubScope())

    val symbols: List<PsiNamedElement> = PyClassNameIndex.find(name, project, scope) +
                                         PyFunctionNameIndex.find(name, project, scope) +
                                         PyVariableNameIndex.find(name, project, scope)

    val seen = hashSetOf<QualifiedName>()
    for (symbol in symbols) {
      if (!PyUtil.isTopLevel(symbol)) continue
      val containingFile = symbol.containingFile ?: continue
      if (containingFile == node.containingFile) continue
      val virtualFile = containingFile.virtualFile ?: continue

      // do not contribute an import if PyCharm will do it for us
      if (defaultSuggestionScope.contains(virtualFile) && isImportableByPyCharm(symbol, node)) continue
      val moduleQName = indexService.findShortestQualifiedName(virtualFile) ?: continue
      if (!seen.add(moduleQName.append(name))) continue

      // If it's a single-file source root, the candidate file must be its parent.
      // Otherwise, the generated import will be just "import symbol" without its qualified path
      val candidateFile = if (PyUtil.isRoot(containingFile)) {
        containingFile.parent ?: containingFile
      } else containingFile

      quickFix.addImport(symbol, candidateFile, moduleQName)
    }
  }
}
