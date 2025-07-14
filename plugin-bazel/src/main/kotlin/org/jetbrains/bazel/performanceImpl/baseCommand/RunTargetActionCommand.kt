package org.jetbrains.bazel.performanceImpl.baseCommand

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.playback.PlaybackContext
import com.intellij.openapi.ui.playback.commands.PlaybackCommandCoroutineAdapter
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.label.CanonicalLabel
import org.jetbrains.bazel.runnerAction.RunTargetAction
import org.jetbrains.bazel.runnerAction.TestTargetAction
import org.jetbrains.bazel.target.targetUtils

abstract class RunTargetActionCommand(text: String, line: Int) : PlaybackCommandCoroutineAdapter(text, line) {
  override suspend fun doExecute(context: PlaybackContext) {
    executeRunTargetAction(context.project)
  }

  private suspend fun executeRunTargetAction(project: Project) {
    val id = getTargetId(project) ?: return
    val targetInfo = project.targetUtils.getBuildTargetForLabel(id) ?: return
    if (targetInfo.kind.ruleType == RuleType.TEST) {
      TestTargetAction(project, listOf(targetInfo)).doPerformAction(project)
    } else {
      RunTargetAction(project, targetInfo).doPerformAction(project)
    }
  }

  abstract suspend fun getTargetId(project: Project): CanonicalLabel?
}
