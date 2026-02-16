package org.jetbrains.bazel.performanceImpl

import com.intellij.openapi.ui.playback.PlaybackContext
import com.intellij.openapi.ui.playback.commands.PlaybackCommandCoroutineAdapter
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.target.targetUtils

internal class AssertSyncedTargetsCommand(text: String, line: Int) : PlaybackCommandCoroutineAdapter(text, line) {
  companion object {
    const val PREFIX = "${CMD_PREFIX}assertSyncedTargets"
  }

  override suspend fun doExecute(context: PlaybackContext) {
    val project = context.project
    val args = extractCommandArgument(PREFIX).trim()
    val expectedLabels = args.split(" ").filter { it.isNotBlank() }.map { Label.parse(it) }.toSet()

    val actualTargets = project.targetUtils.allTargets().toSet()

    check(actualTargets == expectedLabels) {
      "Target mismatch.\nExpected: $expectedLabels\nActual:   $actualTargets"
    }
  }
}
