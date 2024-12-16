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
 *
 * Original Java code taken from https://github.com/bazelbuild, converted to Kotlin and modified.
 */
package org.jetbrains.plugins.bsp.golang.debug

import com.google.common.collect.ImmutableList
import java.io.File

/**
 * Converts execution-root-relative paths to absolute files with a minimum of file system calls
 * (typically none).
 *
 *
 * Files which exist both underneath the execution root and within a workspace will be resolved
 * to paths within their workspace. This prevents those paths from being broken when a different
 * target is built.
 */
class ExecutionRootPathResolver(
    private val buildArtifactDirectories: ImmutableList<String>,
    val executionRoot: File,
    private val outputBase: File,
    private val workspacePathResolver: WorkspacePathResolver
) {
    fun resolveExecutionRootPath(path: ExecutionRootPath): File {
        if (path.isAbsolute) {
            return path.absoluteOrRelativeFile
        }
        val firstPathComponent: String = getFirstPathComponent(path.absoluteOrRelativeFile.getPath())
        if (buildArtifactDirectories.contains(firstPathComponent)) {
            // Build artifacts accumulate under the execution root, independent of symlink settings
            return path.getFileRootedAt(executionRoot)
        }
        if (firstPathComponent == "external") { // In external workspace
            // External workspaces accumulate under the output base.
            // The symlinks to them under the execution root are unstable, and only linked per build.
            return path.getFileRootedAt(outputBase)
        }
        // Else, in main workspace
        return workspacePathResolver.resolveToFile(path.absoluteOrRelativeFile.getPath())
    }

    companion object {
        private fun getFirstPathComponent(path: String): String {
            val index = path.indexOf(File.separatorChar)
            return if (index == -1) path else path.substring(0, index)
        }
    }
}
