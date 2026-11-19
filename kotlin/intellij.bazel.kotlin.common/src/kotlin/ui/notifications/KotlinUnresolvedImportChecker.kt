package org.jetbrains.bazel.kotlin.ui.notifications

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.java.ui.notifications.JvmUnresolvedImportChecker
import org.jetbrains.bazel.sync.JavaLanguageClass
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedElementSelector

@ApiStatus.Internal
class KotlinUnresolvedImportChecker : JvmUnresolvedImportChecker() {
  override fun isApplicable(project: Project, file: VirtualFile): Boolean =
    file.extension in JavaLanguageClass.KOTLIN.recognizedFilenameExtensions &&
      ProjectFileIndex.getInstance(project).isInSourceContent(file)

  override fun hasUnresolvedImport(project: Project, psiFile: PsiFile): Boolean {
    val ktFile = psiFile as? KtFile ?: return false
    val importList = ktFile.importList ?: return false
    val hasUnresolvedImport = importList.imports
      .mapNotNull { it.importedReference?.getQualifiedElementSelector()?.mainReference }
      .any { it.multiResolve(false).isEmpty() }
    return hasUnresolvedImport && fileHasMissingGeneratedCode(project, psiFile.virtualFile)
  }
}
