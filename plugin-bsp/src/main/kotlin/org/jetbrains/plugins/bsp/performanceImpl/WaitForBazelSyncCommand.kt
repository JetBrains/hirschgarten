package org.jetbrains.plugins.bsp.performanceImpl

import com.intellij.collaboration.async.nestedDisposable
import com.intellij.openapi.ui.playback.PlaybackContext
import com.intellij.openapi.ui.playback.commands.PlaybackCommandCoroutineAdapter
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.plugins.bsp.impl.projectAware.BspWorkspaceListener
import org.jetbrains.plugins.bsp.impl.projectAware.isSyncInProgress
import org.jetbrains.plugins.bsp.target.temporaryTargetUtils

private const val SYNC_START_TIMEOUT_MS = 10000L

internal class WaitForBazelSyncCommand(text: String, line: Int) : PlaybackCommandCoroutineAdapter(text, line) {
  companion object {
    const val PREFIX = CMD_PREFIX + "waitForBazelSync"
  }

  override suspend fun doExecute(context: PlaybackContext) =
    coroutineScope {
      val project = context.project

      val syncStarted = Channel<Unit>(capacity = 1)
      val syncFinished = Channel<Unit>(capacity = 1)

      @Suppress("UnstableApiUsage")
      project.messageBus.connect(nestedDisposable()).subscribe(
        BspWorkspaceListener.TOPIC,
        object : BspWorkspaceListener {
          override fun syncStarted() {
            runBlocking { syncStarted.send(Unit) }
          }

          override fun syncFinished(canceled: Boolean) {
            runBlocking { syncFinished.send(Unit) }
          }
        },
      )

      // If the sync was started before the call to `doExecute`, we will not receive the event. Hence the timeout.
      withTimeoutOrNull(SYNC_START_TIMEOUT_MS) {
        syncStarted.receive()
      }

      if (project.isSyncInProgress()) {
        syncFinished.receive()
      }

      check(project.temporaryTargetUtils.allTargetIds().isNotEmpty()) { "Target id list is empty after sync" }
    }
}
