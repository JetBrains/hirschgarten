package org.jetbrains.bazel.build.fileresolver

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.workspace.WorkspaceRoot
import org.jetbrains.bazel.workspacePath.WorkspacePathResolverImpl
import java.nio.file.Path
import kotlin.io.path.*

/**
 * Standard file resolver that handles absolute and workspace-relative paths.
 *
 * Ported from Google's Bazel plugin StandardFileResolver.
 * Resolution strategy:
 * 1. If path is absolute and exists, use it directly (after canonicalization)
 * 2. If path is workspace-relative, resolve via WorkspacePathResolver
 * 3. Fall back to resolving relative to project root
 */
class StandardFileResolver : FileResolver {

  override fun resolveToPath(project: Project, fileString: String): Path? {
    val path = Path(fileString)

    // Handle absolute paths
    if (path.isAbsolute) {
      return try {
        // Get canonical path to resolve symlinks and normalize
        path.toRealPath()
      } catch (_: Exception) {
        // If canonicalization fails (e.g., file doesn't exist yet), use absolute path
        path.absolute()
      }
    }

    // Try workspace-relative resolution first
    val projectRootPath = project.rootDir.toNioPath()
    val workspaceRoot = WorkspaceRoot(projectRootPath)
    val workspaceResolver = WorkspacePathResolverImpl(workspaceRoot)
    val resolved = workspaceResolver.resolveToFile(fileString)
    if (resolved != null && resolved.exists()) {
      return resolved
    }

    // Fall back to project-relative resolution
    val projectRelative = projectRootPath.resolve(fileString)
    if (projectRelative.exists()) {
      return projectRelative
    }

    // Last resort: return the project-relative path even if it doesn't exist
    // (it might be created later, or the error message will be clearer with a full path)
    return projectRelative
  }
}
