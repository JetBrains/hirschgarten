package org.jetbrains.bazel.flow.exclude

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.impl.DirectoryIndexExcludePolicy

internal class BazelDirectoryIndexExcludePolicy(val project: Project) : DirectoryIndexExcludePolicy, DumbAware {
  override fun getExcludeUrlsForProject(): Array<out String> =
    BazelSymlinkExcludeService.getInstance(project)
      .getBazelSymlinksToExclude()
      .map { it.toUri().toString() }
      .toTypedArray()
}
