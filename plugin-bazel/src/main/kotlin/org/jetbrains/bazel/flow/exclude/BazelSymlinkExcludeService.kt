package org.jetbrains.bazel.flow.open.exclude

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.commons.symlinks.BazelSymlinksCalculator
import org.jetbrains.bazel.config.BspFeatureFlags
import org.jetbrains.bazel.config.rootDir
import java.nio.file.Path

@Service(Service.Level.PROJECT)
class BazelSymlinkExcludeService(private val project: Project) : DumbAware {
  private var symlinksToExclude: List<Path>? = null

  @Synchronized
  fun getBazelSymlinksToExclude(bazelWorkspace: Path = project.rootDir.toNioPath()): List<Path> {
    this.symlinksToExclude?.let { return it }
    val symlinksToExclude = BazelSymlinksCalculator.calculateBazelSymlinksToExclude(bazelWorkspace, BspFeatureFlags.symlinkScanMaxDepth)

    if (symlinksToExclude.isNotEmpty()) {
      this.symlinksToExclude = symlinksToExclude
    }
    return symlinksToExclude
  }

  companion object {
    @JvmStatic
    fun getInstance(project: Project): BazelSymlinkExcludeService = project.service()
  }
}
