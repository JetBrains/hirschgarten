package org.jetbrains.bazel.jvm.ideStarter

import com.intellij.openapi.ui.playback.PlaybackContext
import com.intellij.openapi.ui.playback.commands.PlaybackCommandCoroutineAdapter
import org.jetbrains.bazel.settings.bazel.bazelJVMProjectSettings

class EnableHotswapCommand(text: String, line: Int) : PlaybackCommandCoroutineAdapter(text, line) {
  companion object {
    const val PREFIX = CMD_PREFIX + "enableHotswap"
  }

  override suspend fun doExecute(context: PlaybackContext) {
    val project = context.project
    project.bazelJVMProjectSettings = project.bazelJVMProjectSettings.copy(hotSwapEnabled = true)
  }
}
