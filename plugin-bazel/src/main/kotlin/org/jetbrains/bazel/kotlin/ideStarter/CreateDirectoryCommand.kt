package org.jetbrains.bazel.kotlin.ideStarter

import com.intellij.openapi.application.writeAction
import com.intellij.openapi.ui.playback.PlaybackContext
import com.intellij.openapi.ui.playback.commands.PlaybackCommandCoroutineAdapter
import com.intellij.openapi.vfs.LocalFileSystem
import org.jetbrains.bazel.config.rootDir
import kotlin.io.path.name

internal class CreateDirectoryCommand(text: String, line: Int) : PlaybackCommandCoroutineAdapter(text, line) {
  companion object {
    const val PREFIX = CMD_PREFIX + "createDirectory"
  }

  override suspend fun doExecute(context: PlaybackContext): Unit =
    writeAction {
      val directoryPath =
        try {
          extractCommandArgument(PREFIX)
        } catch (_: Exception) {
          throw IllegalArgumentException("Usage: $PREFIX directoryPath")
        }
      val rootDir = context.project.rootDir
      val rootDirPath = rootDir.toNioPath().resolve(directoryPath)

      val parent = checkNotNull(LocalFileSystem.getInstance().findFileByNioFile(rootDirPath.parent))
      parent.createChildDirectory(null, rootDirPath.name)
    }
}
