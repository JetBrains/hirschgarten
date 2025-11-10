package org.jetbrains.bazel.flow.open

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import com.intellij.openapi.vfs.refreshAndFindVirtualFile
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.config.rootDir
import java.io.IOException
import java.nio.file.Files.isDirectory
import java.nio.file.Files.isRegularFile
import java.nio.file.Files.newDirectoryStream
import java.nio.file.Path

/**
 * this method can be used to set up additional project properties before opening the Bazel project
 * @param virtualFile the virtual file passed to the project open processor
 */
@RequiresBackgroundThread
fun getProjectViewPath(projectRootDir: VirtualFile, virtualFile: VirtualFile): Path? =
  when {
    virtualFile.isProjectViewFile() -> virtualFile.toNioPath()
    // BUILD file at the root can be treated as a workspace file in this context
    virtualFile.isBuildFile() -> {
      virtualFile.parent
        // ?.takeUnless { it.isWorkspaceRoot() }
        ?.let { calculateProjectViewFilePath(projectRootDir, it) }
    }

    virtualFile.isWorkspaceFile() -> null
    virtualFile.isWorkspaceRoot() -> null
    else -> {
      virtualFile.getBuildFileForPackageDirectory()
        ?.parent
        ?.let { calculateProjectViewFilePath(projectRootDir, it) }
    }
  }

private fun calculateProjectViewFilePath(projectRootDir: VirtualFile, bazelPackageDir: VirtualFile): Path =
  ProjectViewFileUtils.calculateProjectViewFilePath(
    projectRootDir = projectRootDir,
    projectViewPath = null,
    overwrite = true,
    bazelPackageDir = bazelPackageDir,
  )

/**
 * when a file/subdirectory is selected for opening a Bazel project,
 * this method provides information about the real project directory to open
 */
tailrec fun findProjectFolderFromVFile(file: VirtualFile?): VirtualFile? =
  when {
    file == null -> null
    file.isWorkspaceRoot() -> file
    // this is to prevent opening a file that is not an acceptable Bazel config file, #BAZEL-1940
    // TODO(Son): figure out how to write a test for it to avoid regression later
    file.isFile && !file.isEligibleFile() -> null

    else -> findProjectFolderFromVFile(file.parent)
  }

fun VirtualFile.isEligibleFile(): Boolean =
  isWorkspaceFile() || isBuildFile() || isProjectViewFile()

fun VirtualFile.isProjectViewFile(): Boolean =
  isFile && extension == Constants.PROJECT_VIEW_FILE_EXTENSION

fun VirtualFile.isWorkspaceRoot(): Boolean = toNioPath().isWorkspaceRoot()

private fun Path.isWorkspaceRoot(): Boolean =
  isDirectory(this) &&
    Constants.WORKSPACE_FILE_NAMES
      .asSequence()
      .map { resolve(it) }
      .any { isRegularFile(it) }

private fun VirtualFile.isWorkspaceFile() = isFile && name in Constants.WORKSPACE_FILE_NAMES

fun VirtualFile.isBuildFile(): Boolean =
  isFile && name in Constants.BUILD_FILE_NAMES

fun VirtualFile.getBuildFileForPackageDirectory(): VirtualFile? {
  try {
    if (!isDirectory) return null
    val path = toNioPath()

    return newDirectoryStream(path, BUILD_FILE_GLOB)
      .firstOrNull { isRegularFile(it) }
      ?.refreshAndFindVirtualFile()
  } catch (e: IOException) {
    thisLogger().warn("Cannot retrieve Bazel BUILD file from directory $path", e)
    return null
  }
}

fun Project.initProperties(projectRootDir: VirtualFile) {
  thisLogger().debug("Initializing properties for project: $projectRootDir")

  this.isBazelProject = true
  this.rootDir = projectRootDir
}
