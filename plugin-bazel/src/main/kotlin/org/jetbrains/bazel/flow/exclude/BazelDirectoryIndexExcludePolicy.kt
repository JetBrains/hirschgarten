package org.jetbrains.bazel.flow.exclude

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.impl.DirectoryIndexExcludePolicy
import com.intellij.util.TimeoutUtil

internal class BazelDirectoryIndexExcludePolicy(val project: Project) : DirectoryIndexExcludePolicy, DumbAware {
  override fun getExcludeUrlsForProject(): Array<out String> {
    // Log the message if the computation takes too long.
    // Clarification: The name "TimeoutUtil.compute" is misleading. It doesn't timeout.
    // It executes the function in the third argument when the function in the first argument runs longer than the threshold.
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
