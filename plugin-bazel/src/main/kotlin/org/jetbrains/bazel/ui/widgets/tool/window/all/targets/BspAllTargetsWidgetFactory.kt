package org.jetbrains.bazel.ui.widgets.tool.window.all.targets

import com.intellij.openapi.application.EDT
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
import org.jetbrains.bazel.config.isBspProject
import org.jetbrains.bazel.ui.widgets.tool.window.components.BspToolWindowPanel

public class BspAllTargetsWidgetFactory :
  ToolWindowFactory,
  DumbAware {
  override suspend fun isApplicableAsync(project: Project): Boolean = project.isBspProject

  override fun shouldBeAvailable(project: Project): Boolean = project.isBspProject

  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    val panel = BspToolWindowPanel(project)
    toolWindow.contentManager.removeAllContents(true)
    toolWindow.contentManager.addContent(ContentFactory.getInstance().createContent(panel, "", false))
  }
}

public suspend fun registerBspToolWindow(project: Project) {
  val toolWindowManager = ToolWindowManager.getInstance(project)
  val currentToolWindow = toolWindowManager.getToolWindow(project.bspToolWindowId)
  if (currentToolWindow == null) {
    withContext(Dispatchers.EDT) {
      toolWindowManager
        .registerToolWindow(project.bspToolWindowId) {
          this.icon = BazelPluginIcons.bazelToolWindow
          this.anchor = ToolWindowAnchor.RIGHT
          this.canCloseContent = false
          this.contentFactory = BspAllTargetsWidgetFactory()
        }.show()
    }
  }
}

public suspend fun showBspToolWindow(project: Project) {
  val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(project.bspToolWindowId) ?: return
  withContext(Dispatchers.EDT) {
    toolWindow.show()
  }
}

val Project.bspToolWindowId: String
  get() = BazelPluginConstants.BAZEL_DISPLAY_NAME

val Project.bspToolWindowIdOrNull: String?
  get() {
    if (!isBspProject) return null
    return bspToolWindowId
  }
