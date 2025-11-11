package org.jetbrains.bazel.settings.bazel

import com.intellij.openapi.application.EDT
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileEditor.impl.FileEditorOpenOptions
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun openProjectViewInEditor(
  project: Project,
  file: VirtualFile,
) {
  // README.md should still be the selected tab afterward
  val fileEditorManager = FileEditorManagerEx.getInstanceEx(project)
  withContext(Dispatchers.EDT) {
    fileEditorManager.openFile(
      file,
      FileEditorOpenOptions(
        selectAsCurrent = false,
        requestFocus = false,
      ),
    )
  }
}
