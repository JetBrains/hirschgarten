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
package org.jetbrains.bazel.commons

import java.nio.file.Path
import java.nio.file.Paths
import kotlin.ConsistentCopyVisibility
import kotlin.io.path.Path

/**
 * Represents a path relative to the workspace root. The path component separator is Bazel specific.
 *
 *
 * A [WorkspacePath] is *not* necessarily a valid package name/path. The primary reason is
 * because it could represent a file and files don't have to follow the same conventions as package
 * names.
 */
@ConsistentCopyVisibility
data class WorkspacePath private constructor(private val path: Path) {
  /**
   * @param relativePath relative path that must use the Bazel specific separator char to separate
   * path components
   * @throws IllegalArgumentException if the path is invalid
   */
  constructor(relativePath: String) : this(validateAndCreatePath(relativePath))

  constructor(parentPath: WorkspacePath, childPath: String) : this(
    if (parentPath.isWorkspaceRoot) {
      validateAndCreatePath(childPath)
    } else {
      validateAndCreatePath(parentPath.relativePath() + BAZEL_COMPONENT_SEPARATOR + childPath)
    },
  )

  val parent: WorkspacePath?
    /**
     * Returns the workspace path of this path's parent directory. Returns null if this is the
     * workspace root.
     */
    get() {
      if (this.isWorkspaceRoot) {
        return null
      }
      val parentPath = path.parent
      return parentPath?.let { WorkspacePath(it) }
    }

  /** Returns this workspace path, relative to the workspace root.  */
  fun asPath(): Path = path

  val isWorkspaceRoot: Boolean
    get() = path.toString().isEmpty() || path.toString() == "."

  override fun toString(): String = path.toString()

  fun relativePath(): String = path.toString()

  companion object {
    /** Silently returns null if this is not a valid workspace path.  */
    @JvmStatic
    fun createIfValid(relativePath: String): WorkspacePath? = if (isValid(relativePath)) WorkspacePath(Path(relativePath)) else null

    private const val BAZEL_COMPONENT_SEPARATOR = '/'

    private fun normalizePathSeparator(relativePath: String): String {
      val systemInfoProvider = SystemInfoProvider.getInstance()
      return if (systemInfoProvider.isWindows) relativePath.replace('\\', BAZEL_COMPONENT_SEPARATOR) else relativePath
    }

    private fun validateAndCreatePath(relativePath: String): Path {
      val normalizedPath = normalizePathSeparator(relativePath)
      val error = validate(normalizedPath)
      require(error == null) { "Invalid workspace path '$relativePath': $error" }
      return Paths.get(normalizedPath)
    }

    @JvmStatic
    fun isValid(relativePath: String): Boolean = validate(relativePath) == null

    /** Validates a workspace path. Returns null on success or an error message otherwise.  */
    @JvmStatic
    fun validate(relativePath: String): String? {
      if (relativePath.startsWith("/")) {
        return "Workspace path must be relative; cannot start with '/': $relativePath"
      }
      if (relativePath.startsWith("../")) {
        return (
          "Workspace path must be inside the workspace; cannot start with '../': " +
            relativePath
        )
      }
      if (relativePath.endsWith("/")) {
        return "Workspace path may not end with '/': $relativePath"
      }

      if (relativePath.indexOf(':') >= 0) {
        return "Workspace path may not contain ':': $relativePath"
      }

      return null
    }
  }
}
