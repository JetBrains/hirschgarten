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
import org.jetbrains.bazel.commons.WorkspaceRoot
import java.nio.file.Path

/** Uses the package path locations to resolve a workspace path.  */
data class WorkspacePathResolverImpl(private val workspaceRoot: WorkspaceRoot) : WorkspacePathResolver {
  override fun resolveToIncludeDirectories(relativePath: WorkspacePath): List<Path> = listOf(workspaceRoot.fileForPath(relativePath))

  override fun findPackageRoot(relativePath: String): Path = workspaceRoot.directory

  override fun getWorkspacePath(absolutePath: Path): WorkspacePath? = workspaceRoot.workspacePathForSafe(absolutePath)

  override fun findWorkspaceRoot(absolutePath: Path): WorkspaceRoot? =
    if (workspaceRoot.isInWorkspace(absolutePath)) workspaceRoot else null
}
