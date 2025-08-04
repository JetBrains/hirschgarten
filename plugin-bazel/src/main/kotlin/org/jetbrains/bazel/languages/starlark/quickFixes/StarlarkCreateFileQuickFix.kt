package org.jetbrains.bazel.languages.starlark.quickFixes

import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths

class StarlarkCreateFileQuickFix(element: StarlarkStringLiteralExpression) : LocalQuickFixAndIntentionActionOnPsiElement(element) {
  override fun getText(): String = familyName

  override fun getFamilyName(): String = StarlarkBundle.message("quickfix.name.create.file")

  override fun startInWriteAction(): Boolean = false

  override fun invoke(
    project: Project,
    psiFile: PsiFile,
    editor: Editor?,
    startElement: PsiElement,
    endElement: PsiElement,
  ) {
    val absolutePath = getAbsolutePath() ?: return
    val parentDirectory = absolutePath.parent ?: return
    val fileName = absolutePath.fileName.toString()

    try {
      val newPsiFile = createFile(project, parentDirectory, fileName)
      newPsiFile?.virtualFile?.let {
        OpenFileDescriptor(project, it).navigate(true)
      }
    } catch (e: Exception) {
      showErrorDialog(project, StarlarkBundle.message("error.dialog.message.create.file", e.message!!))
    }
  }

  private fun getAbsolutePath(): Path? {
    val element = startElement as? StarlarkStringLiteralExpression ?: return null
    val str = element.text.trim('"')
    if (str.isBlank()) return null

    val currentDir = element.containingFile.containingDirectory?.virtualFile ?: return null
    return Paths.get(currentDir.path).resolve(str).normalize()
  }

  private fun createFile(
    project: Project,
    parentDirectory: Path,
    fileName: String,
  ): PsiFile? =
    WriteAction.compute<PsiFile?, Throwable> {
      val parentDirVFile = VfsUtil.createDirectories(parentDirectory.toString())
      val parentPsiDir =
        PsiManager.getInstance(project).findDirectory(parentDirVFile)
          ?: throw IOException(StarlarkBundle.message("error.dialog.message.find.psi.dir", parentDirVFile.path))

      parentPsiDir.findFile(fileName) ?: parentPsiDir.createFile(fileName)
    }

  private fun showErrorDialog(project: Project, message: String) {
    Messages.showErrorDialog(project, message, StarlarkBundle.message("error.dialog.name"))
  }
}
