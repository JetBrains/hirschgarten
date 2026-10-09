package org.jetbrains.bazel.flow.sync

import com.intellij.openapi.components.serviceAsync
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.backend.workspace.storeAndGet
import com.intellij.platform.backend.workspace.virtualFile
import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.storage.entities
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.flow.exclude.BazelSymlinkExcludeService
import org.jetbrains.bazel.languages.projectview.index
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.sync.ProjectSyncHook.ProjectSyncHookEnvironment
import org.jetbrains.bazel.sync.withSubtask
import org.jetbrains.bazel.workspace.indexing.IndexableContent
import org.jetbrains.bazel.workspace.indexing.IndexableContentCollector
import org.jetbrains.bazel.workspacemodel.entities.BazelProjectDirectoriesEntity
import org.jetbrains.bazel.workspacemodel.entities.BazelProjectEntitySource
import java.nio.file.Path

/**
 * This sync hook does three important things:
 * 1. Creates the WSM entity
 * 2. Supports the `index:` section, see its documentation in
 *    [org.jetbrains.bazel.languages.projectview.ProjectViewSectionProvider].
 * 3. Loads all non-indexable files that happen to be under `directories:` (and not excluded) into the VFS,
 *    so that "Go to file by name" is quicker, see https://youtrack.jetbrains.com/issue/IJPL-207088
 */
internal class DirectoriesSyncHook : ProjectSyncHook {

  override suspend fun onSync(environment: ProjectSyncHookEnvironment) {
    val project = environment.project
    val virtualFileUrlManager = project.serviceAsync<WorkspaceModel>().getVirtualFileUrlManager()
    val directoryRoots = environment.withSubtask("Collect project directories") {
      computeProjectDirectories(environment, virtualFileUrlManager)
    }
    val indexPatterns = environment.server.projectView.index
    val indexableContent = environment.withSubtask("Collect indexable content") {
      computeIndexableContent(environment, indexPatterns, directoryRoots, virtualFileUrlManager)
    }
    environment.diff.addEntity(
      BazelProjectDirectoriesEntity(
        projectRoot = virtualFileUrlManager.storeAndGet(project.rootDir),
        includedRoots = directoryRoots.included.toList(),
        excludedRoots = directoryRoots.excluded.toList(),
        indexPatterns = indexPatterns,
        indexableRecursiveRoots = indexableContent.recursiveRoots.toList(),
        indexableNonRecursiveRoots = indexableContent.nonRecursiveRoots.toList(),
        entitySource = BazelProjectEntitySource,
      ),
    )
  }

  private data class DirectoryRoots(
    val included: Set<VirtualFileUrl>,
    val excluded: Set<VirtualFileUrl>,
  )

  private suspend fun computeProjectDirectories(
    environment: ProjectSyncHookEnvironment,
    virtualFileUrlManager: VirtualFileUrlManager,
  ): DirectoryRoots {
    val directories = environment
      .server
      .workspaceDirectories(environment.snapshot.repoMapping, environment.taskId)
    val symlinkExcludes = BazelSymlinkExcludeService
      .getInstance(environment.project)
      .scanForBazelSymlinksToExclude(environment.project.rootDir.toNioPath())
      .toVirtualUrlSet(virtualFileUrlManager)
    val includedRoots = directories
      .includedDirectories
      .toVirtualUrlSet(virtualFileUrlManager)
    val excludedRoots = directories
      .excludedDirectories
      .toVirtualUrlSet(virtualFileUrlManager)
    return DirectoryRoots(
      included = includedRoots,
      excluded = excludedRoots + symlinkExcludes,
    )
  }

  private fun computeIndexableContent(
    environment: ProjectSyncHookEnvironment,
    indexPatterns: List<String>,
    directoryRoots: DirectoryRoots,
    virtualFileUrlManager: VirtualFileUrlManager,
  ): IndexableContent {
    val collector = IndexableContentCollector(
      project = environment.project,
      indexPatterns = indexPatterns,
      includedRoots = directoryRoots.included.mapNotNullTo(mutableSetOf(), VirtualFileUrl::virtualFile),
      excludedRoots = directoryRoots.excluded.mapNotNullTo(mutableSetOf(), VirtualFileUrl::virtualFile),
      contentRoots = environment.diff
        .entities<ContentRootEntity>()
        .mapNotNullTo(mutableSetOf()) { it.url.virtualFile },
    )
    return collector.collect(virtualFileUrlManager)
  }

  private fun Collection<Path>.toVirtualUrlSet(
    manager: VirtualFileUrlManager,
  ): Set<VirtualFileUrl> = mapTo(mutableSetOf()) { it.toVirtualFileUrl(manager) }
}
