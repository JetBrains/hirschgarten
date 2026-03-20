package org.jetbrains.bazel.flow.exclude

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import com.intellij.util.concurrency.annotations.RequiresWriteLock
import com.intellij.workspaceModel.core.fileIndex.impl.WorkspaceFileIndexEx
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.symlinks.BazelSymlinksCalculator
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.bazelProjectProperties
import org.jetbrains.bazel.workspace.bazelProjectDirectoriesEntity
import org.jetbrains.bazel.workspace.excludeSymlinksFromFileWatcher
import org.jetbrains.bazel.workspacemodel.entities.modifyBazelProjectDirectoriesEntity
import java.nio.file.Path

@ApiStatus.Internal
@Service(Service.Level.PROJECT)
class BazelSymlinkExcludeService(private val project: Project) : DumbAware {
  @Volatile
  private var symlinksToExclude: Set<Path>? = null

  fun getOrComputeBazelSymlinksToExclude(bazelWorkspace: Path): Set<Path> {
    val currentSymlinks = this.symlinksToExclude
    if (currentSymlinks != null) {
      return currentSymlinks
    }

    val computedSymlinks = BazelSymlinksCalculator.calculateBazelSymlinksToExclude(bazelWorkspace, BazelFeatureFlags.symlinkScanMaxDepth)
    if (computedSymlinks.isNotEmpty()) {
      logger.info("Found bazel symlinks to exclude during workspace scan: $computedSymlinks")
      return addBazelSymlinksToExcludeUnderLock(computedSymlinks)
    }
    return emptySet()
  }

  fun getOrComputeBazelSymlinksToExclude(): Set<Path> {
    // Most of the unit tests don't fill the rootDir property,
    // so we need a separate method with null-checking to handle this
    val bazelWorkspace = project.bazelProjectProperties.rootDir?.toNioPathOrNull() ?: return emptySet()
    return getOrComputeBazelSymlinksToExclude(bazelWorkspace)
  }

  @RequiresWriteLock
  fun addBazelSymlinksToExclude(newSymlinks: Set<Path>) {
    logger.info("Excluding newly detected Bazel symlinks: $newSymlinks")
    addBazelSymlinksToProjectDirectoriesEntity(newSymlinks)
    addBazelSymlinksToExcludeUnderLock(newSymlinks)
    // Make sure that IntelliJ's workspace machinery will pull the most recent
    // list of excluded directories from BazelDirectoryIndexExcludePolicy immediately.
    // Otherwise, LVCS can start iterating over Bazel's symlinks, effectively freezing the IDE.
    WorkspaceFileIndexEx.getInstance(project).reset()
  }

  @Synchronized
  private fun addBazelSymlinksToExcludeUnderLock(newSymlinks: Set<Path>): Set<Path> {
    excludeSymlinksFromFileWatcher(newSymlinks.toList())
    val currentSymlinks = symlinksToExclude ?: emptySet()
    return currentSymlinks.plus(newSymlinks).also { symlinksToExclude = it }
  }

  @RequiresWriteLock
  private fun addBazelSymlinksToProjectDirectoriesEntity(newSymlinks: Set<Path>) {
    val workspaceModel = WorkspaceModel.getInstance(project)
    val newSymlinksUrls = newSymlinks.map { it.toVirtualFileUrl(workspaceModel.getVirtualFileUrlManager()) }
    workspaceModel.updateProjectModel("Add new excluded symlinks") { mutableEntityStorage ->
      val bazelProjectDirectoriesEntity = mutableEntityStorage.bazelProjectDirectoriesEntity() ?: return@updateProjectModel
      mutableEntityStorage.modifyBazelProjectDirectoriesEntity(bazelProjectDirectoriesEntity) {
        this.excludedRoots = this.excludedRoots.plus(newSymlinksUrls).distinct().toMutableList()
      }
    }
  }

  companion object {
    @JvmStatic
    fun getInstance(project: Project): BazelSymlinkExcludeService = project.service()

    private val logger = logger<BazelSymlinkExcludeService>()
  }
}
