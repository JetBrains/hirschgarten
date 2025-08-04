package org.jetbrains.bazel.java.ui.notifications

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import org.jetbrains.bazel.ui.notifications.UnresolvedImportChecker

class JavaUnresolvedImportChecker : UnresolvedImportChecker {
  override fun hasUnresolvedImport(project: Project, psiFile: PsiFile): Boolean {
    val javaPsiFile = psiFile as? PsiJavaFile ?: return false
    val importList = javaPsiFile.importList ?: return false
    return importList.allImportStatements.any { it.resolve() == null }
  }
}
