package org.jetbrains.bazel.performanceImpl

import com.intellij.openapi.ui.playback.PlaybackContext
import com.intellij.openapi.ui.playback.commands.PlaybackCommandCoroutineAdapter
import kotlinx.coroutines.coroutineScope
import org.jetbrains.bazel.sync.scope.SecondPhaseSync
import org.jetbrains.bazel.sync.task.ProjectSyncTask

internal class BuildAndSyncCommand(text: String, line: Int) : PlaybackCommandCoroutineAdapter(text, line) {
  companion object {
    const val PREFIX = CMD_PREFIX + "buildAndSync"
  }

  override suspend fun doExecute(context: PlaybackContext) =
    coroutineScope {
      ProjectSyncTask(context.project).sync(syncScope = SecondPhaseSync, buildProject = true)
    }
}
