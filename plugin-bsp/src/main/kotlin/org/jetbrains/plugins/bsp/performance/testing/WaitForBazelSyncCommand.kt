package org.jetbrains.plugins.bsp.performance.testing

import com.intellij.collaboration.async.nestedDisposable
import com.intellij.openapi.ui.playback.PlaybackContext
import com.intellij.openapi.ui.playback.commands.PlaybackCommandCoroutineAdapter
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.jetbrains.plugins.bsp.config.BspWorkspaceListener

internal class WaitForBazelSyncCommand(text: String, line: Int) : PlaybackCommandCoroutineAdapter(text, line) {
  companion object {
    const val PREFIX = CMD_PREFIX + "waitForBazelSync"
  }

  override suspend fun doExecute(context: PlaybackContext) = coroutineScope {
    val project = context.project

    val syncFinished = Channel<Unit>()

    @Suppress("UnstableApiUsage")
    project.messageBus.connect(nestedDisposable()).subscribe(BspWorkspaceListener.TOPIC, object : BspWorkspaceListener {
      override fun syncFinished(canceled: Boolean) {
        runBlocking { syncFinished.send(Unit) }
      }

      override fun syncStarted() {}
    })

    syncFinished.receive()
  }
}
