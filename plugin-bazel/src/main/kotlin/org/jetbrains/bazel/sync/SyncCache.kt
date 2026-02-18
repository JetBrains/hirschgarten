/*
 * Copyright 2016 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.bazel.sync

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.TestOnly
import java.util.concurrent.ConcurrentHashMap

/** Computes a cache on the project data.  */
@Service(Service.Level.PROJECT)
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

  internal class ClearSyncCache : ProjectPostSyncHook {
    override suspend fun onPostSync(environment: ProjectPostSyncHook.ProjectPostSyncHookEnvironment) {
      val syncCache = getInstance(environment.project)
      syncCache.clear()
    }
  }

  companion object {
    fun getInstance(project: Project): SyncCache = project.getService(SyncCache::class.java)
  }
}
