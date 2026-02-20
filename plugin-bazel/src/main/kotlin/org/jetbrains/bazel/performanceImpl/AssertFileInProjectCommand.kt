package org.jetbrains.bazel.performanceImpl

import com.intellij.openapi.application.readAction
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.ui.playback.PlaybackContext
import com.intellij.openapi.ui.playback.commands.PlaybackCommandCoroutineAdapter
import org.jetbrains.bazel.config.rootDir

internal class AssertFileInProjectCommand(text: String, line: Int) : PlaybackCommandCoroutineAdapter(text, line) {
  companion object {
    const val PREFIX = CMD_PREFIX + "assertFileInProject"
  }

  override suspend fun doExecute(context: PlaybackContext) {
    val args = extractCommandArgument(PREFIX).trim().split(" ")
    val relativePath = args[0]
    val expectedInProject = args.getOrElse(1) { "true" }.toBooleanStrict()

    val project = context.project
    val rootDir = project.rootDir
    val file = rootDir.findFileByRelativePath(relativePath)
    val isInContent = readAction {
      file != null && ProjectFileIndex.getInstance(project).isInContent(file)
    }

    check(isInContent == expectedInProject) {
      if (expectedInProject) "Expected '$relativePath' to be in project content, but it was not"
      else "Expected '$relativePath' to NOT be in project content, but it was"
    }
  }
}
