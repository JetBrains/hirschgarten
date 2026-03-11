package org.jetbrains.bazel.sync

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.TestOnly
import java.util.concurrent.ConcurrentHashMap

/** Computes a cache on the project data.  */
@Service(Service.Level.PROJECT)
@ApiStatus.Internal
class SyncCache(private val project: Project) {
  /** Computes a value based on the sync project data. */
  fun interface SyncCacheComputable<T : Any> {
    fun compute(project: Project): T
  }

  private val cache = ConcurrentHashMap<Any, Any>()

  /**
   * Computes a value derived from the sync project data and caches it until the next sync.
   * [computable] must be the same object for the cache to work, i.e., this is **wrong**:
   * ```kotlin
   * fun getCachedSum(project): Int {
   *   // Lambda object is created on every function call, bad
   *   return SyncCache.getInstance(project).get {
   *     2 + 2
   *   }
   * }
   * ```
   * And this is correct:
   * ```kotlin
   * val computable = SyncCacheComputable {
   *   2 + 2
   * }
   * fun getCachedSum(project): Int {
   *   return SyncCache.getInstance(project).get(computable)
   * }
   * ```
   *
   * @see com.intellij.platform.workspace.storage.VersionedEntityStorage.cachedValue
   */
  @Suppress("UNCHECKED_CAST")
  fun <T : Any> get(computable: SyncCacheComputable<T>): T =
    // From computeIfAbsent docs: "The mapping function must not modify this map during computation."
    // So we're using getOrPut instead
    cache.getOrPut(computable) {
      computable.compute(project)
    } as T

  fun clear(): Unit = cache.clear()

  @TestOnly
  fun <T : Any> injectValueForTest(key: SyncCacheComputable<T>, value: T) {
    cache[key] = value
  }

  @TestOnly
  fun <T : Any> isAlreadyComputed(computable: SyncCacheComputable<T>): Boolean =
    cache[computable] != null

  companion object {
    fun getInstance(project: Project): SyncCache = project.getService(SyncCache::class.java)
  }
}
