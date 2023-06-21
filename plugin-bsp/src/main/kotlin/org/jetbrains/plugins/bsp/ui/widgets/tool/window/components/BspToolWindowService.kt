package org.jetbrains.plugins.bsp.ui.widgets.tool.window.components

import com.intellij.openapi.application.AppUIExecutor
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus

@Service(Service.Level.PROJECT)
@ApiStatus.Internal
public class BspToolWindowService {

  private var deepPanelReload: (() -> Unit)? = null

  public fun setDeepPanelReload(function: () -> Unit) {
    deepPanelReload = function
  }

  public fun doDeepPanelReload() {
    deepPanelReload?.let {
      AppUIExecutor.onUiThread().execute(it)
    }
  }

  public companion object {
    public fun getInstance(project: Project): BspToolWindowService =
      project.getService(BspToolWindowService::class.java)
  }
}
