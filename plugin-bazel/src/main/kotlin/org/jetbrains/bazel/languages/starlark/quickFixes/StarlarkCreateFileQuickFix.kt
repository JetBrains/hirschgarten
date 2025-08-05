package org.jetbrains.bazel.languages.starlark.quickFixes

import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression

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
    val element = startElement as? StarlarkStringLiteralExpression ?: return
    val relativePath = element.text.trim('"')
    val baseDir = psiFile.containingDirectory ?: return

    try {
      val newFile =
        WriteAction.compute<PsiFile, Throwable> {
          createFile(baseDir, relativePath)
        }
      newFile.virtualFile?.let {
        OpenFileDescriptor(project, it).navigate(true)
      }
    } catch (e: Exception) {
      showErrorDialog(project, StarlarkBundle.message("error.dialog.message.create.file", e.message ?: e.javaClass.name))
    }
  }

  private fun createFile(baseDir: PsiDirectory, relativePath: String): PsiFile {
    val pathParts = relativePath.replace('\\', '/').split('/')
    val fileName = pathParts.last()
    val dirParts = pathParts.dropLast(1)

    var currentDir = baseDir
    for (dirName in dirParts) {
      if (dirName.isEmpty() || dirName == ".") continue
      if (dirName == "..") {
        currentDir = currentDir.parent ?: currentDir
        continue
      }
      val subDir = currentDir.findSubdirectory(dirName)
      currentDir = subDir ?: currentDir.createSubdirectory(dirName)
    }

    return currentDir.findFile(fileName) ?: currentDir.createFile(fileName)
  }

  private fun showErrorDialog(project: Project, message: String) {
    Messages.showErrorDialog(project, message, StarlarkBundle.message("error.dialog.name"))
  }
}
