package org.jetbrains.plugins.bsp.performance.testing

import com.intellij.openapi.application.EDT
import com.intellij.openapi.ui.playback.PlaybackContext
import com.intellij.openapi.ui.playback.commands.PlaybackCommandCoroutineAdapter
import com.intellij.openapi.wm.ToolWindowManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.all.targets.bspToolWindowId

internal class OpenBspToolWindowCommand(text: String, line: Int) : PlaybackCommandCoroutineAdapter(text, line) {
  companion object {
    const val PREFIX = CMD_PREFIX + "openBspToolWindow"
  }

  override suspend fun doExecute(context: PlaybackContext) {
    val project = context.project
    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(project.bspToolWindowId) ?: return
    withContext(Dispatchers.EDT) {
      toolWindow.show()
    }
  }
}
