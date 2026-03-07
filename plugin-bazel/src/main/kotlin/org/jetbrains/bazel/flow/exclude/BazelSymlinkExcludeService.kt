package org.jetbrains.bazel.flow.exclude

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.workspaceModel.core.fileIndex.impl.WorkspaceFileIndexEx
import org.jetbrains.bazel.annotations.InternalApi
import org.jetbrains.bazel.commons.symlinks.BazelSymlinksCalculator
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.bazelProjectProperties
import org.jetbrains.bazel.workspace.excludeSymlinksFromFileWatcher
import java.nio.file.Path

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

  @InternalApi
  fun addBazelSymlinksToExclude(newSymlinks: Set<Path>) {
    logger.info("Excluding newly detected Bazel symlinks: $newSymlinks")
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

  companion object {
    @JvmStatic
    fun getInstance(project: Project): BazelSymlinkExcludeService = project.service()

    private val logger = logger<BazelSymlinkExcludeService>()
  }
}
