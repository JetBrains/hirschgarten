package org.jetbrains.bazel.run.coverage.ideStarter

import com.intellij.coverage.CoverageDataManager
import com.intellij.openapi.application.readAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ui.playback.PlaybackContext
import com.intellij.openapi.ui.playback.commands.PlaybackCommandCoroutineAdapter
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.bazel.runnerAction.RunWithCoverageAction
import org.jetbrains.bazel.target.targetUtils

class RunTestWithCoverageCommand(text: String, line: Int) : PlaybackCommandCoroutineAdapter(text, line) {
  companion object {
    const val PREFIX = CMD_PREFIX + "runTestWithCoverage"
  }

  override suspend fun doExecute(context: PlaybackContext) {
    val project = context.project
    val virtualFile = readAction { checkNotNull(FileEditorManager.getInstance(project).selectedTextEditor?.virtualFile) }
    val targetUtils = project.targetUtils
    val target = checkNotNull(targetUtils.getTargetsForFile(virtualFile).singleOrNull()) { "Expected one target for $virtualFile" }
    val targetInfo = checkNotNull(targetUtils.getBuildTargetForLabel(target))

    RunWithCoverageAction(project, listOf(targetInfo)).doPerformAction(project)

    val coverageDataManager = CoverageDataManager.getInstance(project)
    withTimeoutOrNull(60000) {
      while (coverageDataManager.currentSuitesBundle == null) {
        delay(1000)
      }
    } ?: throw IllegalStateException("Timed out waiting for coverage suite to be set")
  }
}
