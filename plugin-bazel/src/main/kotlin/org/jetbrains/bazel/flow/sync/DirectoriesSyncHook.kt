package org.jetbrains.bazel.flow.sync

import com.intellij.openapi.application.readAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.VcsDirectoryMapping
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.backend.workspace.toVirtualFileUrl
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.flow.open.exclude.BazelSymlinkExcludeService
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.sync.ProjectSyncHook.ProjectSyncHookEnvironment
import org.jetbrains.bazel.sync.projectStructure.workspaceModel.workspaceModelDiff
import org.jetbrains.bazel.sync.task.query
import org.jetbrains.bazel.sync.withSubtask
import org.jetbrains.bazel.workspacemodel.entities.BazelProjectDirectoriesEntity
import org.jetbrains.bazel.workspacemodel.entities.BazelProjectEntitySource
import org.jetbrains.bsp.protocol.WorkspaceDirectoriesResult
import java.nio.file.Path

class DirectoriesSyncHook : ProjectSyncHook {
  override suspend fun onSync(environment: ProjectSyncHookEnvironment) {
    environment.withSubtask("Collect project directories") {
      val directories = query("workspace/directories") { environment.server.workspaceDirectories() }
      val additionalExcludes = BazelSymlinkExcludeService.getInstance(environment.project).getBazelSymlinksToExclude()
      val entity = createEntity(environment.project, directories, additionalExcludes)

      environment.diff.workspaceModelDiff.mutableEntityStorage
        .addEntity(entity)

      environment.diff.workspaceModelDiff.addPostApplyAction {
        removeExcludedVcsMappings(environment.project)
      }
    }
  }

  private fun createEntity(
    project: Project,
    directories: WorkspaceDirectoriesResult,
    additionalExcludes: List<Path>,
  ): BazelProjectDirectoriesEntity.Builder {
    val virtualFileUrlManager = WorkspaceModel.getInstance(project).getVirtualFileUrlManager()

    val includedRoots = directories.includedDirectories.map { it.uri }.map { virtualFileUrlManager.getOrCreateFromUrl(it) }
    val excludedRoots =
      directories.excludedDirectories.map { it.uri }.map { virtualFileUrlManager.getOrCreateFromUrl(it) } +
        additionalExcludes.map { it.toVirtualFileUrl(virtualFileUrlManager) }

    return BazelProjectDirectoriesEntity(
      projectRoot = project.rootDir.toVirtualFileUrl(virtualFileUrlManager),
      includedRoots = includedRoots,
      excludedRoots = excludedRoots,
      entitySource = BazelProjectEntitySource,
    )
  }

  /**
   * https://youtrack.jetbrains.com/issue/BAZEL-948
   */
  private suspend fun removeExcludedVcsMappings(project: Project) {
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
