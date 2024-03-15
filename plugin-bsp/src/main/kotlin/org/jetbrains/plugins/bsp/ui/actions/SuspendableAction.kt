package org.jetbrains.plugins.bsp.ui.actions

import com.intellij.ide.impl.isTrusted
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.config.isBspProject
import org.jetbrains.plugins.bsp.services.BspCoroutineService
import javax.swing.Icon

private val log = logger<SuspendableAction>()

public abstract class SuspendableAction(text: () -> String, icon: Icon? = null) : AnAction(text, icon) {
  public constructor(text: String, icon: Icon? = null) : this({ text }, icon)

  final override fun actionPerformed(e: AnActionEvent) {
    val project = e.project

    if (project != null) {
      BspCoroutineService.getInstance(project).start {
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
      e.presentation.isVisible = project.isBspProject
      if (project.isBspProject) {
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
