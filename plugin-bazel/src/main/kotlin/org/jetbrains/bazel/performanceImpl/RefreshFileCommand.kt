package org.jetbrains.bazel.performanceImpl

import com.intellij.openapi.ui.playback.PlaybackContext
import com.intellij.openapi.ui.playback.commands.PlaybackCommandCoroutineAdapter
import com.intellij.openapi.vfs.VfsUtil
import org.jetbrains.bazel.config.rootDir

internal class RefreshFileCommand(text: String, line: Int) : PlaybackCommandCoroutineAdapter(text, line) {
  companion object {
    const val PREFIX = "${CMD_PREFIX}refreshFile"
  }

  override suspend fun doExecute(context: PlaybackContext) {
    val relativePath = extractCommandArgument(PREFIX).trim()
    val project = context.project
    val rootDir = project.rootDir
    val file = rootDir.findFileByRelativePath(relativePath)
    checkNotNull(file) { "File not found: $relativePath in $rootDir" }
    VfsUtil.markDirtyAndRefresh(false, false, false, file)
  }
}
