package org.jetbrains.bazel.settings.bazel

import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileEditor.impl.FileEditorOpenOptions
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.backend.workspace.virtualFile
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Path

fun Project.setProjectViewPath(newProjectViewFilePath: Path) {
  val vfsUrlManager = this.service<WorkspaceModel>()
    .getVirtualFileUrlManager()
  newProjectViewFilePath.toVirtualFileUrl(vfsUrlManager).virtualFile?.let {
    bazelProjectSettings = bazelProjectSettings.withNewProjectViewPath(it)
  }
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
