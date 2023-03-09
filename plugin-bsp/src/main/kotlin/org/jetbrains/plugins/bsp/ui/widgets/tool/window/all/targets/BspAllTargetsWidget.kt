package org.jetbrains.plugins.bsp.ui.widgets.tool.window.all.targets

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import org.jetbrains.plugins.bsp.config.BspProjectPropertiesService
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.components.BspToolWindowPanel

public class BspAllTargetsWidgetFactory : ToolWindowFactory {

  override fun isApplicable(project: Project): Boolean {
    val projectProperties = BspProjectPropertiesService.getInstance(project).value

    return projectProperties.isBspProject
  }

  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    val panel = BspToolWindowPanel(project)
    toolWindow.contentManager.addContent(ContentFactory.getInstance().createContent(panel.component, "", false))
  }
}
