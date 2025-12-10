package org.jetbrains.bazel.sync_new.flow.vfs_diff

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.toNioPathOrNull
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.config.rootDir

class SyncVFSFileFilter(
  private val project: Project,
) {
  private val watchableExtensions by lazy {
    SyncVFSFileContributor.ep.extensionList
      .flatMap { it.getWatchableFileExtensions(project) }
      .toHashSet()
  }

  fun isWatchableFile(vFile: VirtualFile): Boolean {
    if (isAspectsDir(vFile)) {
      return false
    }
    return vFile.extension in watchableExtensions || isBazelFile(vFile)
  }

  private fun isBazelFile(vFile: VirtualFile): Boolean {
    val name = vFile.name
    return when {
      name == "BUILD" || name == "BUILD.bazel" -> true
      name == "WORKSPACE" || name == "WORKSPACE.bazel" -> true
      name == "MODULE" || name == "MODULE.bazel" -> true
      vFile.extension == "bzl" -> true
      else -> false
    }
  }

  private fun isAspectsDir(vFile: VirtualFile): Boolean {
    val path = vFile.toNioPathOrNull() ?: return false
    val projectRoot = project.rootDir.toNioPath()
    val bazelBspDir = projectRoot.resolve(Constants.DOT_BAZELBSP_DIR_NAME)
    return path.startsWith(bazelBspDir)
  }
}
