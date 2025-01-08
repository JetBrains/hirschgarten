package org.jetbrains.plugins.bsp.performanceImpl

import com.intellij.openapi.ui.playback.PlaybackContext
import com.intellij.openapi.ui.playback.commands.PlaybackCommandCoroutineAdapter
import org.jetbrains.plugins.bsp.performance.MemoryProfiler

internal class StartRecordingMaxMemoryCommand(text: String, line: Int) : PlaybackCommandCoroutineAdapter(text, line) {
  companion object {
    const val PREFIX = CMD_PREFIX + "startRecordingMaxMemory"
  }

  override suspend fun doExecute(context: PlaybackContext) {
    MemoryProfiler.startRecordingMaxMemory()
  }
}
