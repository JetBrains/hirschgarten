/*
 * Copyright 2018 The Bazel Authors. All rights reserved.
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
package org.jetbrains.bazel.golang.sync

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.golang.resolve.BazelGoPackage
import org.jetbrains.bazel.sync.libraries.BazelExternalLibraryProvider
import java.nio.file.Path
import kotlin.io.path.extension

const val GO_EXTERNAL_LIBRARY_ROOT_NAME = "Go Libraries"

class GoAdditionalLibraryRootsProvider : BazelExternalLibraryProvider() {
  override val libraryName: String = GO_EXTERNAL_LIBRARY_ROOT_NAME

  override fun getLibraryFiles(project: Project): List<Path> {
    if (!BazelFeatureFlags.isGoSupportEnabled) return emptyList()
    if (!project.isBazelProject) return emptyList()
    val workspacePath = project.rootDir.toNioPath()

    // don't use sync cache, because
    // 1. this is used during sync before project data is saved
    // 2. the roots provider is its own cache
    val libraryFiles =
      BazelGoPackage
        .getUncachedTargetToFileMap(project)
        .values()
        .filter { !it.startsWith(workspacePath) && it.extension == "go" }
        .distinct()
        .toList()
    return libraryFiles
  }
}
