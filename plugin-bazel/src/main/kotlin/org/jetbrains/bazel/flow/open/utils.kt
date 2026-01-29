package org.jetbrains.bazel.flow.open

import com.intellij.openapi.application.EDT
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileEditor.impl.FileEditorOpenOptions
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.refreshAndFindVirtualDirectory
import com.intellij.openapi.vfs.refreshAndFindVirtualFileOrDirectory
import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.config.rootDir
import java.io.IOException
import java.nio.file.Files.isDirectory
import java.nio.file.Files.isRegularFile
import java.nio.file.Files.newDirectoryStream
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.name
import kotlin.io.path.relativeTo

/**
 * this method can be used to set up additional project properties before opening the Bazel project
 * @param path the file passed to the project open processor
 */
@RequiresBackgroundThread
fun getProjectViewPath(
  projectRootDir: Path,
  path: Path,
): Path? {
  fun calculateProjectViewFilePath(directory: Path): Path? {
    val virtualDirectory = projectRootDir.refreshAndFindVirtualDirectory() ?: return null
    return ProjectViewFileUtils.calculateProjectViewFilePath(
      projectRootDir = virtualDirectory,
      projectViewPath = null,
      overwrite = true,
      format = directory.relativeTo(projectRootDir).toString(),
    )
  }

  return when {
    path.hasExtensionOf(Constants.PROJECT_VIEW_FILE_EXTENSION) -> path
    // BUILD file at the root can be treated as a workspace file in this context
    path.hasNameOf(*Constants.BUILD_FILE_NAMES) -> {
      path.parent
        //?.takeUnless { it.workspaceFile != null }
        ?.let { calculateProjectViewFilePath(it) }
    }

    path.hasNameOf(*Constants.WORKSPACE_FILE_NAMES) -> null
    path.workspaceFile != null -> null
    else -> {
      path.getBuildFileForPackageDirectory()
        ?.parent
        ?.let { calculateProjectViewFilePath(it) }
    }
  }
}

/**
 * when a file/subdirectory is selected for opening a Bazel project,
 * this method provides information about the real project directory to open
 */
@RequiresBackgroundThread
tailrec fun findProjectFolderFromFile(path: Path?): Path? = when {
  path == null -> null
  path.workspaceFile != null -> path
  // this is to prevent opening a file that is not an acceptable Bazel config file, #BAZEL-1940
  // TODO(Son): figure out how to write a test for it to avoid regression later
  isRegularFile(path) &&
  !path.hasNameOf(*Constants.WORKSPACE_FILE_NAMES + Constants.BUILD_FILE_NAMES) &&
  !path.hasExtensionOf(Constants.PROJECT_VIEW_FILE_EXTENSION) -> null

  else -> findProjectFolderFromFile(path.parent)
}

@RequiresBackgroundThread
fun findProjectFolderFromVFile(projectIdentityFile: VirtualFile?): VirtualFile? =
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

fun Path.hasNameOf(vararg names: String): Boolean =
  isRegularFile(this) &&
    name in names

fun Path.hasExtensionOf(vararg extensions: String): Boolean =
  isRegularFile(this) &&
    extension in extensions

private fun Path.getBuildFileForPackageDirectory(): Path? {
  if (!isDirectory(this)) return null

  return try {
    newDirectoryStream(this, BUILD_FILE_GLOB)
      .firstOrNull { isRegularFile(it) }
  } catch (e: IOException) {
    thisLogger().warn("Cannot retrieve Bazel BUILD file from directory: $this", e)
    null
  }
}

fun Project.initProperties(projectRootDir: Path) {
  val virtualFile = projectRootDir.refreshAndFindVirtualDirectory()
  if (virtualFile != null) {
    initProperties(virtualFile)
  } else {
    thisLogger().warn("Unable to initialize project properties for project root directory: $projectRootDir")
  }
}

fun Project.initProperties(projectRootDir: VirtualFile) {
  thisLogger().debug("Initializing properties for project: $projectRootDir")

  this.isBazelProject = true
  this.rootDir = projectRootDir
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
