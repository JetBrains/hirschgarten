package org.jetbrains.bazel.flow.sync

import com.intellij.openapi.application.readAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.VcsDirectoryMapping
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.backend.workspace.toVirtualFileUrl
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import kotlinx.coroutines.coroutineScope
import org.jetbrains.bazel.config.BazelPluginConstants.bazelBspBuildToolId
import org.jetbrains.bazel.flow.open.exclude.BazelSymlinkExcludeService
import org.jetbrains.bsp.protocol.WorkspaceDirectoriesResult
import org.jetbrains.plugins.bsp.config.BuildToolId
import org.jetbrains.plugins.bsp.config.rootDir
import org.jetbrains.plugins.bsp.impl.flow.sync.ProjectSyncHook
import org.jetbrains.plugins.bsp.impl.flow.sync.ProjectSyncHook.ProjectSyncHookEnvironment
import org.jetbrains.plugins.bsp.impl.flow.sync.query
import org.jetbrains.plugins.bsp.projectStructure.workspaceModel.workspaceModelDiff
import org.jetbrains.plugins.bsp.workspacemodel.entities.BspProjectDirectoriesEntity
import org.jetbrains.plugins.bsp.workspacemodel.entities.BspProjectEntitySource
import java.nio.file.Path

class DirectoriesSyncHook : ProjectSyncHook {
  override val buildToolId: BuildToolId = bazelBspBuildToolId

  override suspend fun onSync(environment: ProjectSyncHookEnvironment) {
    coroutineScope {
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
  ): BspProjectDirectoriesEntity.Builder {
    val virtualFileUrlManager = WorkspaceModel.getInstance(project).getVirtualFileUrlManager()

    val includedRoots = directories.includedDirectories.map { it.uri }.map { virtualFileUrlManager.getOrCreateFromUrl(it) }
    val excludedRoots =
      directories.excludedDirectories.map { it.uri }.map { virtualFileUrlManager.getOrCreateFromUrl(it) } +
        additionalExcludes.map { it.toVirtualFileUrl(virtualFileUrlManager) }

    return BspProjectDirectoriesEntity(
      projectRoot = project.rootDir.toVirtualFileUrl(virtualFileUrlManager),
      includedRoots = includedRoots,
      excludedRoots = excludedRoots,
      entitySource = BspProjectEntitySource,
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
    val projectFileIndex = ProjectFileIndex.getInstance(project)
    return readAction { projectFileIndex.isExcluded(file) }
  }
}
