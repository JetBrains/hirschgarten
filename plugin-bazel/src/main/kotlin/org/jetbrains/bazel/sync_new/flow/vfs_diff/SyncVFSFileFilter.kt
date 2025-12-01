package org.jetbrains.bazel.sync_new.flow.vfs_diff

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class SyncVFSFileFilter(
  private val project: Project,
) {
  private val watchableExtensions by lazy {
    SyncVFSFileContributor.ep.extensionList
      .flatMap { it.getWatchableFileExtensions(project) }
      .toHashSet()
  }

  fun isWatchableFile(vFile: VirtualFile): Boolean {
    return vFile.extension in watchableExtensions || isBazelFile(vFile) || REMOVE_THIS(vFile)
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

  private fun REMOVE_THIS(vFile: VirtualFile): Boolean {
    val ext = vFile.extension
    return when (ext) {
      "java", "kt" -> true
      else -> false
    }
  }
}
