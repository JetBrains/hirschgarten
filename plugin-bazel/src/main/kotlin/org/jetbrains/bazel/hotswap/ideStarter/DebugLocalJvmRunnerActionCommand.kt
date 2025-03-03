package org.jetbrains.bazel.hotswap.ideStarter

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.playback.PlaybackContext
import com.intellij.openapi.ui.playback.commands.PlaybackCommandCoroutineAdapter
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.runnerAction.RunWithLocalJvmRunnerAction
import org.jetbrains.bazel.runnerAction.TestWithLocalJvmRunnerAction
import org.jetbrains.bazel.target.targetUtils

abstract class DebugLocalJvmRunnerActionCommand(text: String, line: Int) : PlaybackCommandCoroutineAdapter(text, line) {
  override suspend fun doExecute(context: PlaybackContext) {
    executeDebugLocalJVMRunnerAction(context.project)
  }

  private suspend fun executeDebugLocalJVMRunnerAction(project: Project) {
    val id = getTargetId(project) ?: return
    val targetInfo = project.targetUtils.getBuildTargetInfoForLabel(id) ?: return
    if (targetInfo.capabilities.canTest) {
      TestWithLocalJvmRunnerAction(targetInfo, isDebugMode = true).doPerformAction(project)
    } else {
      RunWithLocalJvmRunnerAction(targetInfo, isDebugMode = true).doPerformAction(project)
    }
  }

  abstract suspend fun getTargetId(project: Project): Label?
}
