package org.jetbrains.bazel.languages.starlark.formatting

import com.intellij.ide.actionsOnSave.impl.ActionsOnSaveFileDocumentManagerListener
import com.intellij.openapi.application.readAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.settings.bazel.bazelProjectSettings

class StarlarkFormattingActionOnSave : ActionsOnSaveFileDocumentManagerListener.DocumentUpdatingActionOnSave() {
  override val presentableName: String
    get() = "buildifier"

  override fun isEnabledForProject(project: Project): Boolean = project.isBazelProject && project.bazelProjectSettings.runBuildifierOnSave

  override suspend fun updateDocument(project: Project, document: Document) {
    val psiFile = readAction { PsiDocumentManager.getInstance(project).getPsiFile(document) } as? StarlarkFile ?: return
    formatBuildFile(psiFile)
  }
}
