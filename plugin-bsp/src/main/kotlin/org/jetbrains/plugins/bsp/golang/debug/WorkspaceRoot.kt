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

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.nio.file.Path


/**
 * Represents a workspace root
 */
class WorkspaceRoot(private val directory: File) {
    fun directory(): File {
        return directory
    }

    fun path(): Path {
        return directory.toPath()
    }

    fun workspacePathFor(file: VirtualFile): WorkspacePath {
        return workspacePathFor(file.getPath())
    }

    private fun workspacePathFor(path: String): WorkspacePath {
        require(isInWorkspace(path)) { String.format("File '%s' is not under workspace %s", path, directory) }
        if (directory.getPath().length == path.length) {
            return WorkspacePath("")
        }
        return WorkspacePath(path.substring(directory.getPath().length + 1))
    }

    fun isInWorkspace(file: VirtualFile): Boolean {
        return isInWorkspace(file.getPath())
    }

    private fun isInWorkspace(path: String): Boolean {
        return FileUtil.isAncestor(directory.getPath(), path, false)
    }

    override fun toString(): String {
        return directory.toString()
    }

  override fun equals(o: Any?): Boolean {
    if (this === o) {
      return true
    }
    if (o == null || javaClass != o.javaClass) {
      return false
    }

    val that = o as WorkspaceRoot
    return FileUtil.filesEqual(directory, that.directory)
  }

  override fun hashCode(): Int {
    return FileUtil.fileHashCode(directory)
  }
}
