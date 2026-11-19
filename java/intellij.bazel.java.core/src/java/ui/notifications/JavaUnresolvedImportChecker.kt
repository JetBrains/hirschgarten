package org.jetbrains.bazel.java.ui.notifications

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.sync.JavaLanguageClass

@ApiStatus.Internal
class JavaUnresolvedImportChecker : JvmUnresolvedImportChecker() {
  override fun isApplicable(project: Project, file: VirtualFile): Boolean =
    file.extension in JavaLanguageClass.JAVA.recognizedFilenameExtensions &&
      ProjectFileIndex.getInstance(project).isInSourceContent(file)

  override fun hasUnresolvedImport(project: Project, psiFile: PsiFile): Boolean {
    val javaPsiFile = psiFile as? PsiJavaFile ?: return false
    val importList = javaPsiFile.importList ?: return false
    val hasUnresolvedImport = importList.allImportStatements.asSequence()
      .mapNotNull { it.importReference }
      .any { it.multiResolve(false).isEmpty() }
    return hasUnresolvedImport &&
           fileHasMissingGeneratedCode(project, psiFile.virtualFile)
  }
}
