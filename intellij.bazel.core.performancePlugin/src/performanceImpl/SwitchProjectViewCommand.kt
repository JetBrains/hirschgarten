package org.jetbrains.bazel.performanceImpl

import com.intellij.openapi.ui.playback.PlaybackContext
import com.intellij.openapi.ui.playback.commands.PlaybackCommandCoroutineAdapter
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.settings.bazel.bazelProjectSettings

internal class SwitchProjectViewCommand(text: String, line: Int) : PlaybackCommandCoroutineAdapter(text, line) {
  companion object {
    const val PREFIX = "${CMD_PREFIX}switchProjectView"
  }

  override suspend fun doExecute(context: PlaybackContext) {
    val project = context.project
    val fileName = extractCommandArgument(PREFIX).trim()

    val projectViewFile = project.rootDir.findFileByRelativePath(fileName)
    checkNotNull(projectViewFile) { "Project view file not found: $fileName in ${project.rootDir}" }

    project.bazelProjectSettings = project.bazelProjectSettings.withNewProjectViewPath(projectViewFile)
  }
}
