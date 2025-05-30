package org.jetbrains.bazel.ui.widgets.tool.window.all.targets

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
import org.jetbrains.bazel.ui.widgets.tool.window.components.BazelToolWindowPanel

class BazelAllTargetsWidgetFactory :
  ToolWindowFactory,
  DumbAware {
  override suspend fun isApplicableAsync(project: Project): Boolean = project.isBazelProject

  override fun shouldBeAvailable(project: Project): Boolean = project.isBazelProject

  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    val panel = BazelToolWindowPanel(project)
    toolWindow.contentManager.removeAllContents(true)
    toolWindow.contentManager.addContent(ContentFactory.getInstance().createContent(panel, "", false))
  }
}

suspend fun registerBazelToolWindow(project: Project) {
  val toolWindowManager = project.serviceAsync<ToolWindowManager>()
  val currentToolWindow = toolWindowManager.getToolWindow(bazelToolWindowId)
  if (currentToolWindow == null) {
    withContext(Dispatchers.EDT) {
      toolWindowManager
        .registerToolWindow(bazelToolWindowId) {
          this.icon = BazelPluginIcons.bazelToolWindow
          this.anchor = ToolWindowAnchor.RIGHT
          this.canCloseContent = false
          this.contentFactory = BazelAllTargetsWidgetFactory()
        }.show()
    }
  }
}

suspend fun showBspToolWindow(project: Project) {
  val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(bazelToolWindowId) ?: return
  withContext(Dispatchers.EDT) {
    toolWindow.show()
  }
}

val bazelToolWindowId: String
  get() = BazelPluginConstants.BAZEL_DISPLAY_NAME

val Project.bazelToolWindowIdOrNull: String?
  get() {
    if (!isBazelProject) return null
    return bazelToolWindowId
  }
