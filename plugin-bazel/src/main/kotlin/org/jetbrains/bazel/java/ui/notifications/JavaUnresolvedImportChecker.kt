package org.jetbrains.bazel.java.ui.notifications

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiPolyVariantReference
import org.jetbrains.bazel.ui.notifications.UnresolvedImportChecker
import kotlin.collections.any
import kotlin.collections.isEmpty
import kotlin.collections.mapNotNull

class JavaUnresolvedImportChecker : UnresolvedImportChecker {
  override fun hasUnresolvedImport(project: Project, psiFile: PsiFile): Boolean {
    val javaPsiFile = psiFile as? PsiJavaFile ?: return false
    val importList = javaPsiFile.importList ?: return false
    return importList.allImportStatements
      .mapNotNull { it.reference as? PsiPolyVariantReference }
      .any { it.multiResolve(false).isEmpty() }
  }
}
