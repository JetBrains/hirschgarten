package com.intellij.bazel.python.backend

import com.intellij.openapi.components.service
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiUtilCore
import com.intellij.psi.util.QualifiedName
import com.jetbrains.python.psi.resolve.PyCanonicalPathProvider
import com.jetbrains.python.psi.resolve.QualifiedNameFinder
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.config.isBazelProject

@ApiStatus.Internal
class BazelPyCanonicalPathProvider : PyCanonicalPathProvider {
  override fun getCanonicalPath(symbol: PsiElement?, qName: QualifiedName, foothold: PsiElement?): QualifiedName? {
    val project = (foothold ?: symbol)?.project ?: return null
    if (!project.isBazelProject) return null

    val virtualFile = PsiUtilCore.getVirtualFile(symbol) ?: return null
    return project.service<PythonResolveIndexService>().findShortestQualifiedName(virtualFile)
  }
}

/** Returns `true` if PyCharm is capable of generating an import for symbol */
internal fun isImportableByPyCharm(symbol: PsiElement, foothold: PsiElement?): Boolean =
  QualifiedNameFinder.findCanonicalImportPath(symbol, foothold) != null
