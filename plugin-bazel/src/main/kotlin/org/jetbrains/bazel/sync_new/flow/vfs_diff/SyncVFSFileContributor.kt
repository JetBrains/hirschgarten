package org.jetbrains.bazel.sync_new.flow.vfs_diff

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project

interface SyncVFSFileContributor {
  fun getWatchableFileExtensions(project: Project): List<String>

  companion object {
    val ep: ExtensionPointName<SyncVFSFileContributor> = ExtensionPointName("org.jetbrains.bazel.syncVFSFileContributor")
  }
}
