package org.jetbrains.plugins.bsp.performanceImpl

import com.intellij.openapi.ui.playback.PlaybackContext
import com.intellij.openapi.ui.playback.commands.PlaybackCommandCoroutineAdapter
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.all.targets.showBspToolWindow

internal class OpenBspToolWindowCommand(text: String, line: Int) : PlaybackCommandCoroutineAdapter(text, line) {
  companion object {
    const val PREFIX = CMD_PREFIX + "openBspToolWindow"
  }

  override suspend fun doExecute(context: PlaybackContext) {
    showBspToolWindow(context.project)
  }
}
