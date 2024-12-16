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

import com.intellij.openapi.util.SystemInfo

/**
 * Represents a path relative to the workspace root. The path component separator is Bazel specific.
 *
 *
 * A [WorkspacePath] is *not* necessarily a valid package name/path. The primary reason is
 * because it could represent a file and files don't have to follow the same conventions as package
 * names.
 */
class WorkspacePath(relativePath: String) {
  private val relativePath: String

  /**
   * @param relativePath relative path that must use the Bazel specific separator char to separate
   * path components
   * @throws IllegalArgumentException if the path is invalid
   */
  init {
    val normalizedRelativePath: String = normalizePathSeparator(relativePath)
    val error: String? = validate(normalizedRelativePath)
    require(error == null) { String.format("Invalid workspace path '%s': %s", relativePath, error) }
    this.relativePath = normalizedRelativePath
  }

  val parent: WorkspacePath?
    /**
     * Returns the workspace path of this path's parent directory. Returns null if this is the
     * workspace root.
     */
    get() {
      if (this.isWorkspaceRoot) {
        return null
      }
      val lastSeparatorIndex = relativePath.lastIndexOf('/')
      val parentPath =
        if (lastSeparatorIndex == -1) "" else relativePath.substring(0, lastSeparatorIndex)
      return WorkspacePath(parentPath)
    }

  val isWorkspaceRoot: Boolean
    get() = relativePath.isEmpty() || relativePath == "."

  override fun toString(): String = relativePath

  fun relativePath(): String = relativePath

  override fun equals(o: Any?): Boolean {
    if (this === o) {
      return true
    }
    if (o == null || this.javaClass != o.javaClass) {
      return false
    }

    val that = o as WorkspacePath
    return relativePath == that.relativePath
  }

  override fun hashCode(): Int = relativePath.hashCode()

  companion object {
    private const val BAZEL_COMPONENT_SEPARATOR = '/'

    private fun normalizePathSeparator(relativePath: String): String =
      if (SystemInfo.isWindows) relativePath.replace('\\', BAZEL_COMPONENT_SEPARATOR) else relativePath

    /**
     * Validates a workspace path. Returns null on success or an error message otherwise.
     */
    fun validate(relativePath: String): String? {
      if (relativePath.startsWith("/")) {
        return "Workspace path must be relative; cannot start with '/': " + relativePath
      }
      if (relativePath.startsWith("../")) {
        return "Workspace path must be inside the workspace; cannot start with '../': " + relativePath
      }
      if (relativePath.endsWith("/")) {
        return "Workspace path may not end with '/': " + relativePath
      }

      if (relativePath.indexOf(':') >= 0) {
        return "Workspace path may not contain ':': " + relativePath
      }

      return null
    }
  }
}
