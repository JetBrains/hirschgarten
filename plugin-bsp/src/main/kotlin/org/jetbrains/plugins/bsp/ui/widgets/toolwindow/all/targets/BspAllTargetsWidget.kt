package org.jetbrains.plugins.bsp.ui.widgets.toolwindow.all.targets

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import org.jetbrains.plugins.bsp.ui.widgets.toolwindow.BspToolWindowPanel

public class BspAllTargetsWidgetFactory : ToolWindowFactory {

  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    val panel = BspToolWindowPanel(project)
    toolWindow.contentManager.addContent(ContentFactory.getInstance().createContent(panel.component, "", false))
  }
}
