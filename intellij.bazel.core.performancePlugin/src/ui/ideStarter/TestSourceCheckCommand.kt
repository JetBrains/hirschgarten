package org.jetbrains.bazel.ui.ideStarter

import com.intellij.openapi.application.readAction
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.ui.playback.PlaybackContext
import com.intellij.openapi.ui.playback.commands.PlaybackCommandCoroutineAdapter
import com.intellij.openapi.vfs.LocalFileSystem

internal class TestSourceCheckCommand(text: String, line: Int) : PlaybackCommandCoroutineAdapter(text, line) {
  companion object {
    const val PREFIX = CMD_PREFIX + "testSourceCheck"
  }

  override suspend fun doExecute(context: PlaybackContext) {
    // expected argument format: "<true|false> <relativePath>"
    val arguments = extractCommandArgument(PREFIX).trim().split(' ')
    val shouldBeMarkedAsTest = arguments.first() == "true"
    val relativePath = arguments[1]

    val basePath = context.project.basePath ?: error("No project base path")
    val file = readAction {
      LocalFileSystem.getInstance().findFileByPath("$basePath/$relativePath")
    } ?: error("File not found: $relativePath")

    val isTest = readAction {
      ProjectFileIndex.getInstance(context.project).isInTestSourceContent(file)
    }

    check(isTest == shouldBeMarkedAsTest) {
      "File $relativePath marked as test source: $isTest (expected: $shouldBeMarkedAsTest)"
    }
  }
}
