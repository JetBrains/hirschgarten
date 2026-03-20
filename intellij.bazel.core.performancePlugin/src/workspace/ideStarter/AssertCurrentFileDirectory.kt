package org.jetbrains.bazel.workspace.ideStarter

import com.intellij.openapi.application.readAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ui.playback.PlaybackContext
import com.intellij.openapi.ui.playback.commands.AbstractCommand
import com.intellij.openapi.ui.playback.commands.PlaybackCommandCoroutineAdapter
import org.jetbrains.annotations.NonNls

internal class AssertCurrentFileDirectory(text: String, line: Int) : PlaybackCommandCoroutineAdapter(text, line) {
  companion object {
    const val PREFIX: String = AbstractCommand.CMD_PREFIX + "assertCurrentFileDirectory"
  }

  override suspend fun doExecute(context: PlaybackContext) {
    val qualifiedFileName = extractCommandArgument(PREFIX).split(" ").filterNot { it.trim() == "" }.singleOrNull()
    if (qualifiedFileName == null) {
      throw IllegalArgumentException("File name (with directory) is not provided")
    }
    readAction {
      val editor = checkNotNull(FileEditorManager.getInstance(context.project).selectedTextEditor)
      val currentFilePath = editor.virtualFile?.path ?: throw Exception("Could not obtain path of current file")
      if (!currentFilePath.endsWith(qualifiedFileName)) {
        throw Exception("Current file path $currentFilePath does not end in $qualifiedFileName")
      }
    }
  }

}
