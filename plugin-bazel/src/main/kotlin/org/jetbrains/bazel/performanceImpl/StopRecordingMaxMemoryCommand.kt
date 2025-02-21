package org.jetbrains.bazel.performanceImpl

import com.intellij.openapi.ui.playback.PlaybackContext
import com.intellij.openapi.ui.playback.commands.PlaybackCommandCoroutineAdapter
import org.jetbrains.bazel.performance.MemoryProfiler

internal class StopRecordingMaxMemoryCommand(text: String, line: Int) : PlaybackCommandCoroutineAdapter(text, line) {
  companion object {
    const val PREFIX = CMD_PREFIX + "stopRecordingMaxMemory"
  }

  override suspend fun doExecute(context: PlaybackContext) {
    MemoryProfiler.stopRecordingMaxMemory()
  }
}
