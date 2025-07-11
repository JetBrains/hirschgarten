package org.jetbrains.bazel.ui.queryTab

import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.ContentFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.assets.BazelPluginIcons
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.ui.widgets.tool.window.components.BazelQueryToolWindow

class BazelQueryWidgetFactory :
  ToolWindowFactory,
  DumbAware {
  override suspend fun isApplicableAsync(project: Project): Boolean = project.isBazelProject

  override fun shouldBeAvailable(project: Project): Boolean = project.isBazelProject

  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    val panel = BazelQueryToolWindow(project)
    toolWindow.contentManager.removeAllContents(true)
    toolWindow.contentManager.addContent(ContentFactory.getInstance().createContent(panel, "", false))
  }
}

suspend fun registerBazelQueryToolWindow(project: Project) {
  val toolWindowManager = project.serviceAsync<ToolWindowManager>()
  val currentToolWindow = toolWindowManager.getToolWindow(bazelQueryToolWindowId)
  if (currentToolWindow == null) {
    withContext(Dispatchers.EDT) {
      toolWindowManager
        .registerToolWindow(bazelQueryToolWindowId) {
          this.icon = BazelPluginIcons.bazelToolWindow
          this.anchor = ToolWindowAnchor.BOTTOM
          this.sideTool = false
          this.canCloseContent = false
          this.contentFactory = BazelQueryWidgetFactory()
        }.show()
    }
  }
}

suspend fun showQueryToolWindow(project: Project) {
  val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(bazelQueryToolWindowId) ?: return
  withContext(Dispatchers.EDT) {
    toolWindow.show()
  }
}

val bazelQueryToolWindowId: String
  get() = BazelPluginConstants.BAZEL_QUERY_DISPLAY_NAME

val Project.bazelQueryToolWindowIdOrNull: String?
  get() {
    if (!isBazelProject) return null
    return bazelQueryToolWindowId
  }
