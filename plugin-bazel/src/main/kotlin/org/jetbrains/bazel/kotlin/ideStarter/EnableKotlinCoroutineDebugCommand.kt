package org.jetbrains.bazel.kotlin.ideStarter

import com.intellij.openapi.ui.playback.PlaybackContext
import com.intellij.openapi.ui.playback.commands.PlaybackCommandCoroutineAdapter
import org.jetbrains.bazel.settings.bazel.bazelJVMProjectSettings

class EnableKotlinCoroutineDebugCommand(text: String, line: Int) : PlaybackCommandCoroutineAdapter(text, line) {
  companion object {
    const val PREFIX = CMD_PREFIX + "enableKotlinCoroutineDebug"
  }

  override suspend fun doExecute(context: PlaybackContext) {
    val project = context.project
    project.bazelJVMProjectSettings = project.bazelJVMProjectSettings.copy(enableKotlinCoroutineDebug = true)
  }
}
