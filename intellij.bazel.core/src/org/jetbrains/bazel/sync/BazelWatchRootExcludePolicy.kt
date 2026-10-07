package org.jetbrains.bazel.sync

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.impl.WatchRootExcludePolicy
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.sync.workspace.mapper.normal.HARDLINKS_DIR_NAME

/**
 * hardlinks don't need to be watched by fsnotifier, because we already handle refresh ourselves.
 * See `targetHardLinkAttributes.lastModifiedTime() == realFile.getLastModifiedTime()` inside [org.jetbrains.bazel.sync.workspace.mapper.normal.DefaultBazelOutputFileHardLinks]
 * Moreover fsnotifier fails on two files that share the same inode (which is the whole point of hardlinking).
 */
internal class BazelWatchRootExcludePolicy : WatchRootExcludePolicy {
  override fun isApplicable(project: Project): Boolean =
    project.isBazelProject && BazelFeatureFlags.hardLinkOutputFiles

  override fun shouldExclude(watchRootPath: String): Boolean = "/$HARDLINKS_DIR_NAME/" in watchRootPath
}
