package org.jetbrains.bazel.build.fileresolver

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import java.nio.file.Path
import kotlin.io.path.Path

private val LOG = logger<FileResolver>()

/**
 * Extensible file path resolver for Bazel build output.
 *
 * Ported from Google's Bazel plugin to provide flexible file path resolution.
 * Different resolvers can handle different path formats:
 * - Absolute paths
 * - Workspace-relative paths (e.g., "foo/bar/BUILD")
 * - Execution-root-relative paths
 * - External repository paths
 *
 * Implementations are tried in registration order until one succeeds.
 *
 * To register a resolver, add to plugin.xml:
 * ```xml
 * <org.jetbrains.bazel.fileResolver implementation="your.resolver.Class"/>
 * ```
 */
interface FileResolver {
  companion object {
    val EP_NAME: ExtensionPointName<FileResolver> =
      ExtensionPointName.create("org.jetbrains.bazel.fileResolver")

    /**
     * Resolve a file path string to an absolute Path using all registered resolvers.
     * Returns the first successful resolution, or null if no resolver can handle it.
     */
    fun resolve(project: Project, fileString: String): Path? {
      return EP_NAME.extensionList.firstNotNullOfOrNull { resolver ->
        try {
          resolver.resolveToPath(project, fileString)
        } catch (t: Throwable) {
          LOG.warn("Resolver ${resolver.javaClass.simpleName} failed for path: $fileString", t)
          null
        }
      }
    }
  }

  /**
   * Attempt to resolve the given file string to an absolute Path.
   * Return null if this resolver cannot handle the path format.
   *
   * @param project The current project
   * @param fileString The file path string from build output
   * @return Absolute Path if resolved, null otherwise
   */
  fun resolveToPath(project: Project, fileString: String): Path?
}
