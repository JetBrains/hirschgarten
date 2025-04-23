package org.jetbrains.bazel.kotlin.ui.notifications

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.bazel.ui.notifications.UnresolvedImportChecker
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedElementSelector

class KotlinUnresolvedImportChecker : UnresolvedImportChecker {
  override fun hasUnresolvedImport(project: Project, psiFile: PsiFile): Boolean {
    val ktFile = psiFile as? KtFile ?: return false
    val importList = ktFile.importList ?: return false
    return importList.imports
      .mapNotNull { it.importedReference?.getQualifiedElementSelector()?.mainReference }
      .any { it.multiResolve(false).isEmpty() }
  }
}
