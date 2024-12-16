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

import java.io.File

/**
 * Converts workspace-relative paths to absolute files with a minimum of file system calls
 * (typically none).
 */
interface WorkspacePathResolver {
    /**
     * Resolves a workspace relative path to an absolute file.
     */
    fun resolveToFile(workspaceRelativePath: String): File {
        val packageRoot = findPackageRoot(workspaceRelativePath)
        return File(packageRoot, workspaceRelativePath)
    }

    /**
     * Finds the package root directory that a workspace relative path is in.
     */
    fun findPackageRoot(relativePath: String?): File?
}
