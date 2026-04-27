package org.jetbrains.bazel.workspace

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.fileLogger
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.impl.local.WatchRootsManager
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.sync.environment.BazelApplicationContextService
import org.jetbrains.bazel.utils.isUnder
import java.lang.reflect.Field
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.util.NavigableMap

private val LOG = fileLogger()

/**
 * WatchRootsManager doesn't check whether the symlinks it's watching are excluded.
 * Because IDEA 2025.2 has code freeze in place it can't be fixed on platform side :(
 * See:
 * https://youtrack.jetbrains.com/issue/IJPL-199364/Excluded-symlinks-are-watched-by-WatchRootsManager
 * https://youtrack.jetbrains.com/issue/BAZEL-2235/Overly-aggressive-fsnotifier
 */
@ApiStatus.Internal
fun excludeSymlinksFromFileWatcher(symlinksToExclude: List<Path>) {
  try {
    tryExcludeSymlinksFromFileWatcher(symlinksToExclude)
  }
  catch (e: Throwable) {
    LOG.error("Couldn't exclude symlinks $symlinksToExclude from FileWatcher", e)
  }
}

private fun tryExcludeSymlinksFromFileWatcher(symlinksToExclude: List<Path>) {
  if (service<BazelApplicationContextService>().disableFileWatcherSymlinkExclusion) {
    return
  }

  if (!BazelFeatureFlags.excludeSymlinksFromFileWatcherViaReflection) {
    return
  }

  val localFileSystem = LocalFileSystem.getInstance()
  val watchRootsManager: WatchRootsManager = localFileSystem.getFieldWithReflection("myWatchRootsManager")
  val lock: Object = watchRootsManager.getFieldWithReflection("myLock")
  synchronized(lock) {
    val symlinksByPath: NavigableMap<String, Any> = watchRootsManager.getFieldWithReflection("mySymlinksByPath")

    // Insane hack: replace WatchRootsManager#mySymlinksByPath with a map that prevents adding excluded symlink paths.
    // This way, WatchRootsManager#collectSymlinkRequests will not iterate over them.
    val symlinksByPathWithExcludes: MapWithExcludes =
      if (symlinksByPath is MapWithExcludes) {
        symlinksByPath
      }
      else {
        MapWithExcludes(symlinksByPath).also {
          watchRootsManager.setFieldWithReflection("mySymlinksByPath", it)
        }
      }

    symlinksByPathWithExcludes.addExcludes(
      symlinksToExclude +
      symlinksToExclude.mapNotNull { runCatching { it.toRealPath() }.getOrNull() },  // Also exclude directories to which the symlinks resolve
    )
  }
}

private fun <T> Any.getFieldWithReflection(field: String): T =
  this.declaredFieldInThisOrSuper(field)
    .apply { isAccessible = true }
    .get(this) as T

private fun Any.setFieldWithReflection(field: String, newValue: Any) =
  this.declaredFieldInThisOrSuper(field)
    .apply { isAccessible = true }
    .set(this, newValue)

private fun Any.declaredFieldInThisOrSuper(field: String): Field =
  generateSequence(this::class.java) { it.superclass }.firstNotNullOf {
    runCatching { it.getDeclaredField(field) }.getOrNull()
  }

private class MapWithExcludes(private val delegate: NavigableMap<String, Any>) : NavigableMap<String, Any> by delegate {
  private val excludes = mutableSetOf<Path>()

  override fun put(key: String, value: Any): Any? {
    if (key.isExcluded()) return null
    return delegate.put(key, value)
  }

  fun addExcludes(newExcludes: Collection<Path>) {
    if (excludes.addAll(newExcludes)) {
      delegate.entries.removeIf { it.key.isExcluded() }
    }
  }

  private fun String.isExcluded(): Boolean = toPathOrNull()?.isUnder(excludes) == true

  private fun String.toPathOrNull(): Path? =
    try {
      Path.of(this)
    }
    catch (_: InvalidPathException) {
      null
    }
}
