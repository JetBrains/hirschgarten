package org.jetbrains.plugins.bsp.ui.widgets.tool.window.all.targets

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import org.jetbrains.plugins.bsp.config.isBspProject
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.components.BspToolWindowPanel
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.components.BspToolWindowService

public class BspAllTargetsWidgetFactory : ToolWindowFactory {

  override fun isApplicable(project: Project): Boolean =
    project.isBspProject

  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    BspToolWindowService.getInstance(project).setDeepPanelReload { doCreateToolWindowContent(project, toolWindow) }
    doCreateToolWindowContent(project, toolWindow)
  }

  private fun doCreateToolWindowContent(project: Project, toolWindow: ToolWindow) {
    val panel = BspToolWindowPanel(project)
    toolWindow.contentManager.removeAllContents(true)
    toolWindow.contentManager.addContent(ContentFactory.getInstance().createContent(panel.component, "", false))
  }
}
