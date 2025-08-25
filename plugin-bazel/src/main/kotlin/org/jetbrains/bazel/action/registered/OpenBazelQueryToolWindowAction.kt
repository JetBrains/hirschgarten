package org.jetbrains.bazel.action.registered

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.action.SuspendableAction
import org.jetbrains.bazel.assets.BazelPluginIcons
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.bazel.ui.widgets.queryTab.registerBazelQueryToolWindow

class OpenBazelQueryToolWindowAction :
  SuspendableAction(
    BazelPluginBundle.message("action.open.bazelquery.toolwindow.text"),
    BazelPluginIcons.bazelToolWindow,
  ) {
  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    val toolWindowManager = ToolWindowManager.getInstance(project)
    val currentToolWindow = toolWindowManager.getToolWindow(BazelPluginConstants.BAZEL_QUERY_TOOLWINDOW_ID)

    withContext(Dispatchers.EDT) {
      if (currentToolWindow == null) {
        registerBazelQueryToolWindow(project)
      } else if (currentToolWindow.isVisible) {
        currentToolWindow.hide()
      } else {
        currentToolWindow.show()
      }
    }
  }
}
