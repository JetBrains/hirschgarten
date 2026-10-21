package org.jetbrains.bazel.performanceImpl

import com.intellij.openapi.ui.playback.PlaybackContext
import com.intellij.openapi.ui.playback.commands.PlaybackCommandCoroutineAdapter
import org.jetbrains.bazel.sync.ProjectSyncScope
import org.jetbrains.bazel.sync.status.syncAndCheckOutcome

/**
 * Runs a sync with a build, and fails when this sync did not end in a success. See [syncAndCheckOutcome].
 */
internal class BuildAndSyncCommand(text: String, line: Int) : PlaybackCommandCoroutineAdapter(text, line) {
  companion object {
    const val PREFIX = CMD_PREFIX + "buildAndSync"
  }

  override suspend fun doExecute(context: PlaybackContext) {
    context.project.syncAndCheckOutcome(ProjectSyncScope.Full(build = true, phased = false))
  }
}
