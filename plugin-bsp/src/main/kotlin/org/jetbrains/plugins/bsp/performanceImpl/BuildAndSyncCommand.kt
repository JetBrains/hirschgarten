package org.jetbrains.plugins.bsp.performanceImpl

import com.intellij.openapi.ui.playback.PlaybackContext
import com.intellij.openapi.ui.playback.commands.PlaybackCommandCoroutineAdapter
import kotlinx.coroutines.coroutineScope
import org.jetbrains.plugins.bsp.impl.flow.sync.ProjectSyncTask
import org.jetbrains.plugins.bsp.impl.flow.sync.SecondPhaseSync

internal class BuildAndSyncCommand(text: String, line: Int) : PlaybackCommandCoroutineAdapter(text, line) {
  companion object {
    const val PREFIX = CMD_PREFIX + "buildAndSync"
  }

  override suspend fun doExecute(context: PlaybackContext) =
    coroutineScope {
      ProjectSyncTask(context.project).sync(syncScope = SecondPhaseSync, buildProject = true)
    }
}
