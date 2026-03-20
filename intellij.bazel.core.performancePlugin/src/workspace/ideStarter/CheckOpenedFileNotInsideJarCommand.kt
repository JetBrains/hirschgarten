package org.jetbrains.bazel.workspace.ideStarter

import com.intellij.openapi.application.readAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ui.playback.PlaybackContext
import com.intellij.openapi.ui.playback.commands.PlaybackCommandCoroutineAdapter
import com.intellij.openapi.vfs.newvfs.ArchiveFileSystem

internal class CheckOpenedFileNotInsideJarCommand(text: String, line: Int) : PlaybackCommandCoroutineAdapter(text, line) {
  companion object {
    const val PREFIX = CMD_PREFIX + "checkOpenedFileNotInsideJar"
  }

  override suspend fun doExecute(context: PlaybackContext) {
    readAction {
      val editor = checkNotNull(FileEditorManager.getInstance(context.project).selectedTextEditor)
      val currentFile = editor.virtualFile
      check(currentFile?.fileSystem !is ArchiveFileSystem) {
        "File $currentFile should not be inside a jar"
      }
    }
  }
}
