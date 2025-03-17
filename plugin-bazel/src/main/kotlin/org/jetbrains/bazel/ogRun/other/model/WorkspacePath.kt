package org.jetbrains.bazel.ogRun.other.model


import com.intellij.openapi.util.SystemInfo
import java.io.Serializable
import java.nio.file.Path
import java.nio.file.Paths
import javax.annotation.concurrent.Immutable

/**
 * Represents a path relative to the workspace root. The path component separator is Blaze specific.
 *
 *
 * A [WorkspacePath] is *not* necessarily a valid package name/path. The primary reason is
 * because it could represent a file and files don't have to follow the same conventions as package
 * names.
 */
@Immutable
class WorkspacePath(relativePath: String) {
  val relativePath: String
  /**
   * @param relativePath relative path that must use the Blaze specific separator char to separate
   * path components
   * @throws IllegalArgumentException if the path is invalid
   */
  init {
    val normalizedRelativePath = normalizePathSeparator(relativePath)
    val error = validate(normalizedRelativePath)
    require(error == null) { String.format("Invalid workspace path '%s': %s", relativePath, error) }
    this.relativePath = normalizedRelativePath
  }

  constructor(parentPath: WorkspacePath, childPath: String) : this(
    if (parentPath.isWorkspaceRoot)
      childPath
    else
      parentPath.relativePath() + BLAZE_COMPONENT_SEPARATOR + childPath,
  )

  val parent: WorkspacePath?
    /**
     * Returns the workspace path of this path's parent directory. Returns null if this is the
     * workspace root.
     */
    get() {
      if (isWorkspaceRoot) {
        return null
      }
      val lastSeparatorIndex = relativePath.lastIndexOf('/')
      val parentPath =
        if (lastSeparatorIndex == -1) "" else relativePath.substring(0, lastSeparatorIndex)
      return WorkspacePath(parentPath)
    }

  /** Returns this workspace path, relative to the workspace root.  */
  fun asPath(): Path {
    return Paths.get(relativePath)
  }

  val isWorkspaceRoot: Boolean
    get() = relativePath.isEmpty() || relativePath == "."

  override fun toString(): String {
    return relativePath
  }

  fun relativePath(): String {
    return relativePath
  }

  companion object {
    /** Silently returns null if this is not a valid workspace path.  */
    fun createIfValid(relativePath: String): WorkspacePath? {
      return if (isValid(relativePath)) WorkspacePath(relativePath) else null
    }

    private const val BLAZE_COMPONENT_SEPARATOR = '/'

    private fun normalizePathSeparator(relativePath: String): String {
      return if (SystemInfo.isWindows) relativePath.replace('\\', BLAZE_COMPONENT_SEPARATOR) else relativePath
    }

    fun isValid(relativePath: String): Boolean {
      return validate(relativePath) == null
    }

    /** Validates a workspace path. Returns null on success or an error message otherwise.  */
    fun validate(relativePath: String): String? {
      if (relativePath.startsWith("/")) {
        return "Workspace path must be relative; cannot start with '/': $relativePath"
      }
      if (relativePath.startsWith("../")) {
        return ("Workspace path must be inside the workspace; cannot start with '../': "
          + relativePath)
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

