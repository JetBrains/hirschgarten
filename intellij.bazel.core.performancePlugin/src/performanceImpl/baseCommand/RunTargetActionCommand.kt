package org.jetbrains.bazel.performanceImpl.baseCommand

import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.playback.PlaybackContext
import com.intellij.openapi.ui.playback.commands.PlaybackCommandCoroutineAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.target.targetStorage
import org.jetbrains.bazel.ui.widgets.tool.window.utils.runOrBuildTarget

internal abstract class RunTargetActionCommand(text: String, line: Int) : PlaybackCommandCoroutineAdapter(text, line) {
  override suspend fun doExecute(context: PlaybackContext) {
    executeRunTargetAction(context.project)
  }

  private suspend fun executeRunTargetAction(project: Project) {
    val id = getTargetId(project) ?: return
    val targetInfo = project.targetStorage.getTargetSummary(id) ?: return
    withContext(Dispatchers.EDT) { runOrBuildTarget(project, targetInfo) }
  }

  abstract suspend fun getTargetId(project: Project): Label?
}
