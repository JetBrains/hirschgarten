package org.jetbrains.bazel.flow.exclude

import com.intellij.openapi.application.edtWriteAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.diagnostic.telemetry.helpers.useWithScope
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import com.intellij.util.concurrency.ThreadingAssertions
import com.intellij.util.concurrency.annotations.RequiresWriteLock
import com.intellij.workspaceModel.core.fileIndex.impl.WorkspaceFileIndexEx
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.symlinks.BazelSymlinksCalculator
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.performance.bspTracer
import org.jetbrains.bazel.workspace.bazelProjectDirectoriesEntity
import org.jetbrains.bazel.workspace.excludeSymlinksFromFileWatcher
import org.jetbrains.bazel.workspacemodel.entities.modifyBazelProjectDirectoriesEntity
import java.nio.file.Path

@ApiStatus.Internal
@Service(Service.Level.PROJECT)
class BazelSymlinkExcludeService(
  private val project: Project,
  private val coroutineScope: CoroutineScope
) : DumbAware {
  @Volatile
  private var symlinksToExclude: Set<Path> = emptySet()

  fun getBazelSymlinksToExclude(): Set<Path> = symlinksToExclude

  suspend fun scanForBazelSymlinksToExclude(bazelWorkspace: Path): Set<Path> {
    val computedSymlinks = measure("BazelSymlinkExcludeService.scanForBazelSymlinksToExclude.ms") {
      coroutineScope.async(Dispatchers.IO) {
        logger.info("Scanning directory for bazel symlinks to exclude: $bazelWorkspace")
        BazelSymlinksCalculator.calculateBazelSymlinksToExclude(bazelWorkspace, BazelFeatureFlags.symlinkScanMaxDepth)
      }.await()
    }

    if (computedSymlinks.isNotEmpty()) {
      logger.info("Found bazel symlinks to exclude during workspace scan: $computedSymlinks")
      edtWriteAction {
        addBazelSymlinksToExclude(computedSymlinks)
      }
    }

    return computedSymlinks
  }

  @RequiresWriteLock
  fun addBazelSymlinksToExclude(newSymlinks: Set<Path>) {
    ThreadingAssertions.assertWriteAccess()
    logger.info("Excluding newly detected Bazel symlinks: $newSymlinks")
    excludeSymlinksFromFileWatcher(newSymlinks.toList())
    symlinksToExclude = symlinksToExclude + newSymlinks
    // Make sure that IntelliJ's workspace machinery will pull the most recent
    // list of excluded directories from BazelDirectoryIndexExcludePolicy immediately.
    // Otherwise, LVCS can start iterating over Bazel's symlinks, effectively freezing the IDE.
    WorkspaceFileIndexEx.getInstance(project).indexData.resetCustomContributors()
  }

  @RequiresWriteLock
  fun refreshWorkspaceModel() {
    ThreadingAssertions.assertWriteAccess()
    logger.info("Refreshing workspace model with excluded symlinks")
    val workspaceModel = WorkspaceModel.getInstance(project)
    val newSymlinksUrls = symlinksToExclude.map { it.toVirtualFileUrl(workspaceModel.getVirtualFileUrlManager()) }
    workspaceModel.updateProjectModel("Add new excluded symlinks") { mutableEntityStorage ->
      val bazelProjectDirectoriesEntity = mutableEntityStorage.bazelProjectDirectoriesEntity() ?: return@updateProjectModel
      mutableEntityStorage.modifyBazelProjectDirectoriesEntity(bazelProjectDirectoriesEntity) {
        this.excludedRoots = this.excludedRoots.plus(newSymlinksUrls).distinct().toMutableList()
      }
    }
  }

  private suspend inline fun <T> measure(spanName: String, crossinline f: suspend () -> T): T =
    bspTracer.spanBuilder(spanName).useWithScope { f() }

  companion object {
    @JvmStatic
    fun getInstance(project: Project): BazelSymlinkExcludeService = project.service()

    private val logger = logger<BazelSymlinkExcludeService>()
  }
}
