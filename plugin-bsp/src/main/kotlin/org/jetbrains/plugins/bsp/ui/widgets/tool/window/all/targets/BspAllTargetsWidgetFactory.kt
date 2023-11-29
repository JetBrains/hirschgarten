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
import org.jetbrains.plugins.bsp.assets.BuildToolAssetsExtension
import org.jetbrains.plugins.bsp.config.buildToolId
import org.jetbrains.plugins.bsp.config.isBspProject
import org.jetbrains.plugins.bsp.extension.points.withBuildToolIdOrDefault
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.components.BspToolWindowPanel
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.components.BspToolWindowService

public class BspAllTargetsWidgetFactory : ToolWindowFactory, DumbAware {
  override suspend fun isApplicableAsync(project: Project): Boolean = project.isBspProject
  override fun shouldBeAvailable(project: Project): Boolean =
    project.isBspProject

  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    BspToolWindowService.getInstance(project).setDeepPanelReload { doCreateToolWindowContent(project, toolWindow) }
    doCreateToolWindowContent(project, toolWindow)
  }

  private fun doCreateToolWindowContent(project: Project, toolWindow: ToolWindow) {
    val panel = BspToolWindowPanel(project)
    toolWindow.contentManager.removeAllContents(true)
    toolWindow.contentManager.addContent(ContentFactory.getInstance().createContent(panel.component, "", false))
    toolWindow.show()
  }
}

public suspend fun registerBspToolWindow(project: Project) {
  val toolWindowManager = ToolWindowManager.getInstance(project)
  val assetsExtension = BuildToolAssetsExtension.ep.withBuildToolIdOrDefault(project.buildToolId)
  val currentToolWindow = toolWindowManager.getToolWindow(assetsExtension.presentableName)
  if (currentToolWindow == null) {
    withContext(Dispatchers.EDT) {
      toolWindowManager.registerToolWindow(assetsExtension.presentableName) {
        this.icon = assetsExtension.icon
        this.anchor = ToolWindowAnchor.RIGHT
        this.canCloseContent = false
        this.contentFactory = BspAllTargetsWidgetFactory()
      }
    }
  }
}
