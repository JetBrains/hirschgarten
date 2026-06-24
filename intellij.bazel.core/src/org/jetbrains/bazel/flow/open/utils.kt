package org.jetbrains.bazel.flow.open

import com.intellij.ide.impl.OpenProjectTask
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.application.EDT
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileEditor.impl.FileEditorOpenOptions
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import com.intellij.openapi.vfs.refreshAndFindVirtualFileOrDirectory
import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.coroutines.BazelApplicationCoroutineScopeService
import java.nio.file.Files.isDirectory
import java.nio.file.Files.isRegularFile
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.name

/**
 * when a file/subdirectory is selected for opening a Bazel project,
 * this method provides information about the real project directory to open
 */
@RequiresBackgroundThread
internal tailrec fun findProjectFolderFromFile(path: Path?): Path? = when {
  path == null -> null
  path.workspaceFile != null -> path
  // this is to prevent opening a file that is not an acceptable Bazel config file, #BAZEL-1940
  // TODO(Son): figure out how to write a test for it to avoid regression later
  isRegularFile(path) &&
  !path.hasNameOf(*Constants.WORKSPACE_FILE_NAMES + Constants.BUILD_FILE_NAMES + Constants.MODULE_BAZEL_LOCK_FILE_NAME) &&
  !path.hasExtensionOf(Constants.PROJECT_VIEW_FILE_EXTENSION) -> null

  else -> findProjectFolderFromFile(path.parent)
}

@RequiresBackgroundThread
internal fun findProjectFolderFromVFile(projectIdentityFile: VirtualFile?): VirtualFile? =
  projectIdentityFile
    ?.toNioPathOrNull()
    ?.let { findProjectFolderFromFile(it) }
    ?.refreshAndFindVirtualFileOrDirectory()

internal val Path.workspaceFile: Path?
  get() {
    if (!isDirectory(this))
      return null

    return Constants.WORKSPACE_FILE_NAMES
      .asSequence()
      .map { resolve(it) }
      .firstOrNull { isRegularFile(it) }
  }

internal fun Path.hasNameOf(vararg names: String): Boolean =
  isRegularFile(this) &&
    name in names

internal fun Path.hasExtensionOf(vararg extensions: String): Boolean =
  isRegularFile(this) &&
    extension in extensions

internal fun VirtualFile.isBazelWorkspaceFile(): Boolean {
  if (!isFile) return false
  return name in Constants.WORKSPACE_FILE_NAMES || extension == Constants.PROJECT_VIEW_FILE_EXTENSION
}

internal suspend fun openProjectViewInEditor(
  project: Project,
  file: VirtualFile,
) {
  // README.md should still be the selected tab afterward
  val fileEditorManager = FileEditorManagerEx.getInstanceEx(project)
  withContext(Dispatchers.EDT) {
    fileEditorManager.openFile(
      file = file,
      options = FileEditorOpenOptions(
        selectAsCurrent = false,
        requestFocus = false,
      ),
    )
  }
}

/**
 * Closes the current project and reopens it as a Bazel project.
 * Must be called from application-level scope (use [BazelApplicationCoroutineScopeService.launch]).
 */
@ApiStatus.Internal
suspend fun closeAndReopenAsBazelProject(project: Project, file: Path) {
  ProjectUtil.openOrImportAsync(
    file = file,
    options = OpenProjectTask {
      forceReuseFrame = true
      runConfigurators = true
      projectToClose = project
    },
  )
}
