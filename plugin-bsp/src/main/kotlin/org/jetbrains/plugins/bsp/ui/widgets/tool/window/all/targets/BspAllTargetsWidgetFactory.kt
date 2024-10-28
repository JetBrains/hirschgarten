package org.jetbrains.plugins.bsp.ui.widgets.tool.window.all.targets

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
import org.jetbrains.plugins.bsp.assets.assets
import org.jetbrains.plugins.bsp.assets.defaultAssets
import org.jetbrains.plugins.bsp.config.isBspProject
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.components.BspToolWindowPanel

class BspAllTargetsWidgetFactory :
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

suspend fun registerBspToolWindow(project: Project) {
  val toolWindowManager = ToolWindowManager.getInstance(project)
  // avoid the situation of 2 similar tool windows, one having label BSP and one from another build tool name
  if (project.bspToolWindowId != project.defaultBspToolWindowId) {
    val defaultBspToolWindow = toolWindowManager.getToolWindow(project.defaultBspToolWindowId)
    if (defaultBspToolWindow != null) {
      withContext(Dispatchers.EDT) {
        @Suppress("DEPRECATION")
        toolWindowManager.unregisterToolWindow(project.defaultBspToolWindowId)
      }
    }
  }
  val currentToolWindow = toolWindowManager.getToolWindow(project.bspToolWindowId)
  if (currentToolWindow == null) {
    withContext(Dispatchers.EDT) {
      toolWindowManager.registerToolWindow(project.bspToolWindowId) {
        this.icon = project.assets.toolWindowIcon
        this.anchor = ToolWindowAnchor.RIGHT
        this.canCloseContent = false
        this.contentFactory = BspAllTargetsWidgetFactory()
      }
    }
  }
}

suspend fun showBspToolWindow(project: Project) {
  val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(project.bspToolWindowId) ?: return
  withContext(Dispatchers.EDT) {
    toolWindow.show()
  }
}

val Project.bspToolWindowId: String
  get() = this.assets.presentableName

val Project.defaultBspToolWindowId: String
  get() = this.defaultAssets.presentableName

val Project.bspToolWindowIdOrNull: String?
  get() {
    if (!isBspProject) return null
    return bspToolWindowId
  }
