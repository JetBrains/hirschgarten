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
package org.jetbrains.plugins.bsp.golang.sync

import com.google.idea.blaze.base.sync.libraries.LibrarySource

/** Prevents garbage collection of Go libraries  */
internal class BlazeGoLibrarySource private constructor() : LibrarySource.Adapter() {
  val gcRetentionFilter: java.util.function.Predicate<com.intellij.openapi.roots.libraries.Library?>
    get() = java.util.function.Predicate { library: com.intellij.openapi.roots.libraries.Library? ->
      com.google.idea.blaze.golang.sync.BlazeGoLibrarySource.Companion.isGoLibrary(
        library,
      )
    }

  companion object {
    val INSTANCE: com.google.idea.blaze.golang.sync.BlazeGoLibrarySource =
      com.google.idea.blaze.golang.sync.BlazeGoLibrarySource()

    fun isGoLibrary(library: com.intellij.openapi.roots.libraries.Library): Boolean {
      val name: String? = library.getName()
      if (name == null) {
        return false
      }
      for (prefix in BlazeGoSyncPlugin.GO_LIBRARY_PREFIXES) {
        if (name.startsWith(prefix)) {
          return true
        }
      }
      return false
    }
  }
}
