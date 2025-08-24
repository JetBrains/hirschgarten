package org.jetbrains.bazel.performanceImpl

import com.intellij.openapi.ui.playback.PlaybackContext
import com.intellij.openapi.ui.playback.commands.PlaybackCommandCoroutineAdapter
import org.jetbrains.bazel.performance.MemoryProfiler
import org.jetbrains.bazel.performance.telemetry.TelemetryManager
import org.jetbrains.bazel.startup.IntellijTelemetryManager

internal class StartRecordingMaxMemoryCommand(text: String, line: Int) : PlaybackCommandCoroutineAdapter(text, line) {
  companion object {
    const val PREFIX = CMD_PREFIX + "startRecordingMaxMemory"
  }

  override suspend fun doExecute(context: PlaybackContext) {
    // Initialize TelemetryManager if not already initialized
    try {
      TelemetryManager.getInstance()
    } catch (e: IllegalStateException) {
      TelemetryManager.provideTelemetryManager(IntellijTelemetryManager)
    }

    MemoryProfiler.startRecordingMaxMemory()
  }
}
