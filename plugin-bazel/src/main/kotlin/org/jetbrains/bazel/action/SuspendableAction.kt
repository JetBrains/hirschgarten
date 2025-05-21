package org.jetbrains.bazel.action

import com.intellij.ide.impl.isTrusted
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.EDT
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import javax.swing.Icon

private val log = logger<SuspendableAction>()

suspend fun saveAllFiles() {
  withContext(Dispatchers.EDT) {
    FileDocumentManager.getInstance().saveAllDocuments()
  }
}

abstract class SuspendableAction(text: () -> String, icon: Icon? = null) :
  AnAction(text, icon),
  DumbAware {
  constructor(text: String, icon: Icon? = null) : this({ text }, icon)

  final override fun actionPerformed(e: AnActionEvent) {
    val project = e.project

    if (project != null) {
      BazelCoroutineService.getInstance(project).start {
        saveAllFiles()
        actionPerformed(project, e)
      }
    } else {
      log.warn("`actionPerformed` for action '${e.presentation.text}' cannot be performed. Project is missing.")
    }
  }

  protected abstract suspend fun actionPerformed(project: Project, e: AnActionEvent)

  final override fun update(e: AnActionEvent) {
    val project = e.project

    if (project != null) {
      doUpdate(project, e)
    } else {
      log.warn("`update` for action '${e.presentation.text}' cannot be performed. Project is missing.")
    }
  }

  private fun doUpdate(project: Project, e: AnActionEvent) {
    if (project.isTrusted()) {
      e.presentation.isVisible = project.isBazelProject
      if (project.isBazelProject) {
        update(project, e)
      }
    } else {
      e.presentation.isEnabled = false
    }
  }

  protected open fun update(project: Project, e: AnActionEvent) {
    // do nothing by default
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}

fun AnActionEvent.getPsiFile(): PsiFile? = CommonDataKeys.PSI_FILE.getData(dataContext)

fun AnActionEvent.getEditor(): Editor? = CommonDataKeys.EDITOR.getData(dataContext)
