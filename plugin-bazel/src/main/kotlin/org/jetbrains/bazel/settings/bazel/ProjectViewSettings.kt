package org.jetbrains.bazel.settings.bazel

import com.intellij.openapi.application.EDT
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileEditor.impl.FileEditorOpenOptions
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Path

fun Project.setProjectViewPath(newProjectViewFilePath: Path) {
  bazelProjectSettings = bazelProjectSettings.withNewProjectViewPath(newProjectViewFilePath)
}

suspend fun openProjectViewInEditor(project: Project, projectViewPath: Path) {
  val virtualFile =
    withContext(Dispatchers.Default) {
      VirtualFileManager.getInstance().refreshAndFindFileByNioPath(projectViewPath.toAbsolutePath())
    } ?: return

  // README.md should still be the selected tab afterward
  val fileEditorManager = FileEditorManagerEx.getInstanceEx(project)
  withContext(Dispatchers.EDT) {
    fileEditorManager.openFile(
      virtualFile,
      FileEditorOpenOptions(
        selectAsCurrent = false,
        requestFocus = false,
      ),
    )
  }
}
