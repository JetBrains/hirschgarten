package org.jetbrains.bazel.performanceImpl

import com.intellij.openapi.ui.playback.PlaybackContext
import com.intellij.openapi.ui.playback.commands.PlaybackCommandCoroutineAdapter
import org.jetbrains.bazel.sync.status.awaitSyncAndCheckOutcome
import org.jetbrains.bazel.target.targetStorage
import kotlin.time.Duration.Companion.seconds

private val SYNC_START_WINDOW = 10.seconds

/**
 * Waits for the startup sync, or for the sync that runs now, and fails when the last sync did not end in a success.
 * See [awaitSyncAndCheckOutcome].
 *
 * A project that reopens from its cache runs no sync. The command then checks only the restored targets.
 */
internal class WaitForBazelSyncCommand(text: String, line: Int) : PlaybackCommandCoroutineAdapter(text, line) {
  companion object {
    const val PREFIX = CMD_PREFIX + "waitForBazelSync"
  }

  override suspend fun doExecute(context: PlaybackContext) {
    val project = context.project
    project.awaitSyncAndCheckOutcome(SYNC_START_WINDOW)
    check(project.targetStorage.getTotalTargetCount() != 0) { "Target id list is empty after sync" }
  }
}
