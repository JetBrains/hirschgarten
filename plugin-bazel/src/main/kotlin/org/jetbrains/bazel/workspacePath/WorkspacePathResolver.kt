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
package org.jetbrains.bazel.workspacePath

import org.jetbrains.bazel.commons.WorkspacePath
import org.jetbrains.bazel.workspace.WorkspaceRoot
import java.nio.file.Path

/**
 * Converts workspace-relative paths to absolute files with a minimum of file system calls
 * (typically none).
 */
interface WorkspacePathResolver {
  /** Resolves a workspace path to an absolute file.  */
  fun resolveToFile(workspacepath: WorkspacePath): Path? = resolveToFile(workspacepath.relativePath())

  /** Resolves a workspace relative path to an absolute file.  */
  fun resolveToFile(workspaceRelativePath: String): Path? {
    val packageRoot = findPackageRoot(workspaceRelativePath)
    return packageRoot?.resolve(workspaceRelativePath)
  }

  /**
   * This method should be used for directories. Returns all workspace files corresponding to the
   * given workspace path.
   */
  fun resolveToIncludeDirectories(relativePath: WorkspacePath): List<Path>

  /** Finds the package root directory that a workspace relative path is in.  */
  fun findPackageRoot(relativePath: String): Path?

  /**
   * Finds the workspace root directory that an absolute file lies under. Returns null if the file
   * is not in a known workspace.
   */
  fun findWorkspaceRoot(absolutePath: Path): WorkspaceRoot?

  /**
   * Given a resolved, absolute file, returns the corresponding [WorkspacePath]. Returns null
   * if the file is not in the workspace.
   */
  fun getWorkspacePath(absolutePath: Path): WorkspacePath?
}
