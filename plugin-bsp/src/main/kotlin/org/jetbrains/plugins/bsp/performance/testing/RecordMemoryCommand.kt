package org.jetbrains.plugins.bsp.performance.testing

import com.intellij.openapi.ui.playback.PlaybackContext
import com.intellij.openapi.ui.playback.commands.PlaybackCommandCoroutineAdapter

internal class RecordMemoryCommand(text: String, line: Int) : PlaybackCommandCoroutineAdapter(text, line) {
  companion object {
    const val PREFIX = CMD_PREFIX + "recordMemory"
  }

  override suspend fun doExecute(context: PlaybackContext) {
    val gaugeName = extractCommandArgument(PREFIX)
    MemoryProfiler.recordMemory(gaugeName)
  }
}
