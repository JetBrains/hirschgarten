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
package org.jetbrains.bazel.workspace

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.bazel.commons.WorkspacePath
import java.io.File
import java.nio.file.Path

/** Represents a workspace root  */
data class WorkspaceRoot(val directory: Path) {
  constructor(directory: File) : this(directory.toPath())

  fun fileForPath(workspacePath: WorkspacePath): Path = directory.resolve(workspacePath.relativePath())

  fun directory(): Path = directory

  fun absolutePathFor(workspaceRelativePath: String): Path? = directory.resolve(workspaceRelativePath)

  fun path(): Path = directory

  fun workspacePathFor(file: Path): WorkspacePath = workspacePathFor(file.toString())

  fun workspacePathFor(file: VirtualFile): WorkspacePath = workspacePathFor(file.path)

  fun relativize(file: VirtualFile): Path = workspacePathFor(file).asPath()

  fun tryRelativize(file: VirtualFile): Path? {
    if (!isInWorkspace(file)) {
      return null
    }

    return relativize(file)
  }

  private fun workspacePathFor(path: String): WorkspacePath {
    require(isInWorkspace(path)) { String.format("File '%s' is not under workspace %s", path, directory) }
    val dirPathStr = directory.toString()
    if (dirPathStr.length == path.length) {
      return WorkspacePath("")
    }
    return WorkspacePath(path.substring(dirPathStr.length + 1))
  }

  /**
   * Returns the WorkspacePath for the given absolute file, if it's a child of this WorkspaceRoot
   * and a valid WorkspacePath. Otherwise returns null.
   */
  fun workspacePathForSafe(absoluteFile: Path): WorkspacePath? = workspacePathForSafe(absoluteFile.toString())

  fun workspacePathForSafe(absoluteFile: File): WorkspacePath? = workspacePathForSafe(absoluteFile.toPath())

  /**
   * Returns the WorkspacePath for the given virtual file, if it's a child of this WorkspaceRoot and
   * a valid WorkspacePath. Otherwise returns null.
   */
  fun workspacePathForSafe(file: VirtualFile): WorkspacePath? = workspacePathForSafe(file.path)

  private fun workspacePathForSafe(path: String): WorkspacePath? {
    if (!isInWorkspace(path)) {
      return null
    }
    val dirPathStr = directory.toString()
    if (dirPathStr.length == path.length) {
      return WorkspacePath("")
    }
    return WorkspacePath.createIfValid(path.substring(dirPathStr.length + 1))
  }

  fun isInWorkspace(file: Path): Boolean = file.startsWith(directory)

  fun isInWorkspace(file: File): Boolean = isInWorkspace(file.toPath())

  fun isInWorkspace(file: VirtualFile): Boolean = isInWorkspace(file.path)

  private fun isInWorkspace(path: String): Boolean =
    com.intellij.openapi.util.io.FileUtil
      .isAncestor(directory.toString(), path, false)
}
