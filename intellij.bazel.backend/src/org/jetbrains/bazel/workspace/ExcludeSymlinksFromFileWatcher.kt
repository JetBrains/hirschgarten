package org.jetbrains.bazel.workspace

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.fileLogger
import com.intellij.openapi.vfs.WatchRoots
import com.intellij.openapi.vfs.impl.local.WatchRootsServiceImpl
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.symlinks.BazelSymlinksCalculator
import org.jetbrains.bazel.sync.environment.BazelApplicationContextService
import java.nio.file.Path

private val LOG = fileLogger()

/**
 * WatchRootsServiceImpl doesn't check whether the symlinks it's watching are excluded.
 * Until it is fixed on the platform side, tell the service explicitly which symlinks it must not follow.
 * See:
 * https://youtrack.jetbrains.com/issue/IJPL-199364/Excluded-symlinks-are-watched-by-WatchRootsManager
 * https://youtrack.jetbrains.com/issue/BAZEL-2235/Overly-aggressive-fsnotifier
 */
@ApiStatus.Internal
fun excludeSymlinksFromFileWatcher(symlinksToExclude: List<Path>) {
  if (symlinksToExclude.isEmpty() || service<BazelApplicationContextService>().disableFileWatcherSymlinkExclusion) {
    return
  }

  try {
    val watchRoots = WatchRoots.getInstance() as? WatchRootsServiceImpl ?: return
    val paths = symlinksToExclude + symlinksToExclude.mapNotNull { BazelSymlinksCalculator.resolveSymlinkTarget(it) }
    @Suppress("UsagesOfObsoleteApi")
    watchRoots.excludeSymlinks(paths)
  }
  catch (e: Throwable) {
    LOG.error("Couldn't exclude symlinks $symlinksToExclude from FileWatcher", e)
  }
}
