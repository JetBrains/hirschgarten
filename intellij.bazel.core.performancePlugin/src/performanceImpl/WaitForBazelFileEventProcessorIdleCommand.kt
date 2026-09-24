package org.jetbrains.bazel.performanceImpl

import com.intellij.openapi.ui.playback.PlaybackContext
import com.intellij.openapi.ui.playback.commands.PlaybackCommandCoroutineAdapter
import kotlinx.coroutines.delay
import org.jetbrains.bazel.workspace.fileEvents.BazelFileEventProcessor
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

internal class WaitForBazelFileEventProcessorIdleCommand(text: String, line: Int) : PlaybackCommandCoroutineAdapter(text, line) {
  companion object {
    const val PREFIX = CMD_PREFIX + "waitForBazelFileEventProcessorIdle"
  }

  override suspend fun doExecute(context: PlaybackContext) {
    val timeout = 30.seconds
    val start = TimeSource.Monotonic.markNow()
    val processor = BazelFileEventProcessor.getInstance(context.project)

    while (!processor.isIdle()) {
      check(start.elapsedNow() < timeout) {
        "Bazel file event processor did not become idle within $timeout"
      }
      delay(100.milliseconds)
    }
  }
}
