package org.jetbrains.bazel.flow.sync

import com.intellij.openapi.application.readAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.VcsDirectoryMapping
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.sync.ProjectPostSyncHook

/**
 * https://youtrack.jetbrains.com/issue/BAZEL-948
 */
class RemoveExcludedVcsMappingsSyncHook : ProjectPostSyncHook {
  override suspend fun onPostSync(environment: ProjectPostSyncHook.ProjectPostSyncHookEnvironment) {
    val project = environment.project
    val manager = ProjectLevelVcsManager.getInstance(project)
    val directoryMappings = manager.directoryMappings
    val directoryMappingsWithoutExcludes = manager.directoryMappings.filter { mapping -> !isExcludedPath(project, mapping) }
    if (directoryMappingsWithoutExcludes.size == directoryMappings.size) return
    manager.directoryMappings = directoryMappingsWithoutExcludes
  }

  private suspend fun isExcludedPath(project: Project, mapping: VcsDirectoryMapping): Boolean {
    if (mapping.isDefaultMapping) return false
    val file = LocalFileSystem.getInstance().findFileByPath(mapping.directory) ?: return false
    // If the project root is a child of the VCS root, then keep it
    if (VfsUtilCore.isAncestor(file, project.rootDir, false)) return false
    // Otherwise, if the VCS root is somewhere else (not inside our project directory), then remove it.
    // Usually it is something like /private/var/tmp/_bazel_...
    if (!VfsUtilCore.isAncestor(project.rootDir, file, false)) return true
    val projectFileIndex = ProjectFileIndex.getInstance(project)
    return readAction { projectFileIndex.isExcluded(file) }
  }
}
