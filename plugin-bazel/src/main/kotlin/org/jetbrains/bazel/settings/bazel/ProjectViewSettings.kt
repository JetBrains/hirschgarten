package org.jetbrains.bazel.settings.bazel

import com.intellij.openapi.application.EDT
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileEditor.impl.FileEditorOpenOptions
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import java.nio.file.Path

fun Project.setProjectViewPath(newProjectViewFilePath: Path, openProjectViewInEditor: Boolean = true) {
  this.bazelProjectSettings = bazelProjectSettings.withNewProjectViewPath(newProjectViewFilePath)
  if (openProjectViewInEditor) {
    openProjectViewInEditor(this)
  }
}

private fun openProjectViewInEditor(project: Project) {
  BazelCoroutineService.getInstance(project).start {
    val projectViewPath = project.bazelProjectSettings.projectViewPath ?: return@start
    val virtualFile = VirtualFileManager.getInstance().refreshAndFindFileByNioPath(projectViewPath) ?: return@start
    // README.md should still be the selected tab afterward
    val fileEditorManager = FileEditorManagerEx.getInstanceEx(project)
    withContext(Dispatchers.EDT) {
      fileEditorManager.openFile(virtualFile, FileEditorOpenOptions(selectAsCurrent = false, requestFocus = false))
    }
  }
}
