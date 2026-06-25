package org.jetbrains.bazel.performanceImpl

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.playback.PlaybackContext
import com.intellij.openapi.ui.playback.commands.PlaybackCommandCoroutineAdapter
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope
import org.jetbrains.bazel.sync.status.SyncStatusListener
import org.jetbrains.bazel.sync.status.isSyncInProgress
import org.jetbrains.bazel.sync.task.ProjectSyncTask

internal class BuildAndSyncCommand(text: String, line: Int) : PlaybackCommandCoroutineAdapter(text, line) {
  companion object {
    const val PREFIX = CMD_PREFIX + "buildAndSync"
  }

  override suspend fun doExecute(context: PlaybackContext) =
    coroutineScope {
      val project = context.project
      project.waitForRunningSyncToFinish()
      ProjectSyncTask(project).fullSync(buildProject = true)
    }

  private suspend fun Project.waitForRunningSyncToFinish() {
    if (!isSyncInProgress()) return

    val syncFinished = CompletableDeferred<Unit>()
    val connection = messageBus.connect()
    try {
      connection.subscribe(
        SyncStatusListener.TOPIC,
        object : SyncStatusListener {
          override fun syncStarted() = Unit

          override fun syncFinished(canceled: Boolean) {
            syncFinished.complete(Unit)
          }
        },
      )

      if (isSyncInProgress()) {
        syncFinished.await()
      }
    } finally {
      connection.disconnect()
    }
  }
}
