package org.jetbrains.bazel.flow.open

import com.intellij.openapi.application.EDT
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileEditor.impl.FileEditorOpenOptions
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
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
 * @param virtualFile the virtual file passed to the project open processor
 */
@RequiresBackgroundThread
fun getProjectViewPath(
  projectRootDir: VirtualFile,
  virtualFile: VirtualFile,
): Path? {
  val path = virtualFile.toNioPathOrNull()
    ?: return null

  fun calculateProjectViewFilePath(directory: Path): Path =
    ProjectViewFileUtils.calculateProjectViewFilePath(
      projectRootDir = projectRootDir,
      projectViewPath = null,
      overwrite = true,
      format = directory.relativeTo(projectRootDir.toNioPath()).toString(),
    )

  return when {
    path.hasExtensionOf(Constants.PROJECT_VIEW_FILE_EXTENSION) -> path
    // BUILD file at the root can be treated as a workspace file in this context
    path.hasNameOf(*Constants.BUILD_FILE_NAMES) -> {
      path.parent
        // ?.takeUnless { it.isWorkspaceRoot() }
        ?.let { calculateProjectViewFilePath(it) }
    }

    path.hasNameOf(*Constants.WORKSPACE_FILE_NAMES) -> null
    path.isWorkspaceRoot() -> null
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
tailrec fun findProjectFolderFromVFile(file: VirtualFile?): VirtualFile? {
  val path = file?.toNioPathOrNull()
    ?: return null

  return when {
    path.isWorkspaceRoot() -> file
    // this is to prevent opening a file that is not an acceptable Bazel config file, #BAZEL-1940
    // TODO(Son): figure out how to write a test for it to avoid regression later
    isRegularFile(path) &&
      !path.hasNameOf(*Constants.WORKSPACE_FILE_NAMES + Constants.BUILD_FILE_NAMES) &&
      !path.hasExtensionOf(Constants.PROJECT_VIEW_FILE_EXTENSION) -> null

    else -> findProjectFolderFromVFile(file.parent)
  }
}

private fun Path.isWorkspaceRoot(): Boolean =
  isDirectory(this) &&
    Constants.WORKSPACE_FILE_NAMES
      .asSequence()
      .map { resolve(it) }
      .any { isRegularFile(it) }

fun Path.hasNameOf(vararg names: String): Boolean =
  isRegularFile(this) &&
    name in names

fun Path.hasExtensionOf(vararg extensions: String): Boolean =
  isRegularFile(this) &&
    extension in extensions

/**
 * @see [hasNameOf]
 */
fun VirtualFile.isBuildFile(): Boolean =
  isFile && name in Constants.BUILD_FILE_NAMES

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
