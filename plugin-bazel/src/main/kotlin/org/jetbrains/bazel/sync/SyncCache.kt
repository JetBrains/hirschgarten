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

/** Computes a cache on the project data.  */
@Service(Service.Level.PROJECT)
class SyncCache(private val project: Project) {
  /** Computes a value based on the sync project data.  */
  fun interface SyncCacheComputable<T> {
    fun compute(project: Project): T & Any
  }

  private var cache: MutableMap<Any, Any> = HashMap()

  /** Computes a value derived from the sync project data and caches it until the next sync.  */
  @Suppress("UNCHECKED_CAST")
  @Synchronized
  fun <T> get(key: Any, computable: SyncCacheComputable<T>): T? {
    cache[key]?.also { return it as T? }
    val newValue = computable.compute(project)
    cache[key] = newValue
    return newValue
  }

  @Synchronized
  fun clear() {
    // assign a new map instead of clearing.
    // this should be faster than clearing the map.
    cache = HashMap()
  }

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
