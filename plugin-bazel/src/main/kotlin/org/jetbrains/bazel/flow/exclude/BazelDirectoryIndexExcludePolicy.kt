package org.jetbrains.bazel.flow.exclude

import org.jetbrains.bazel.config.isBazelProject
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.impl.DirectoryIndexExcludePolicy
import com.intellij.util.TimeoutUtil

internal class BazelDirectoryIndexExcludePolicy(val project: Project) : DirectoryIndexExcludePolicy, DumbAware {
  override fun getExcludeUrlsForProject(): Array<out String> {
    if (!project.isBazelProject) {
      return emptyArray()
    }
    return TimeoutUtil.compute<Array<out String>, Throwable>(
      {
        BazelSymlinkExcludeService.getInstance(project)
          .getOrComputeBazelSymlinksToExclude()
          .map { it.toUri().toString() }
          .toTypedArray()
      },
      250,
      { duration ->
        logger.warn("Bazel symlinks computation took ${duration}ms in BazelDirectoryIndexExcludePolicy")
      },
    )
  }

  companion object {
    private val logger = logger<BazelDirectoryIndexExcludePolicy>()
  }
}
