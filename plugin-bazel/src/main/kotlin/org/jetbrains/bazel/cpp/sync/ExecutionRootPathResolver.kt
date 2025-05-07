package org.jetbrains.bazel.cpp.sync

import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.bazel.bazelrunner.utils.BazelInfo
import org.jetbrains.bazel.commons.ExecutionRootPath
import org.jetbrains.bazel.commons.TargetKey
import org.jetbrains.bazel.commons.WorkspacePath
import org.jetbrains.bazel.commons.WorkspaceRoot
import org.jetbrains.bazel.info.BspTargetInfo
import java.io.File
import java.io.IOException

/**
 * Converts execution-root-relative paths to absolute files with a minimum of file system calls
 * (typically none).
 *
 *
 * Files which exist both underneath the execution root and within a workspace will be resolved
 * to paths within their workspace. This prevents those paths from being broken when a different
 * target is built.
 *
 * See com.google.idea.blaze.base.sync.workspace.ExecutionRootPathResolver
 */
class ExecutionRootPathResolver(val bazelInfo: BazelInfo, val targetMap: Map<TargetKey, BspTargetInfo.TargetInfo>) {
  private val buildArtifactDirectories: List<String>
  private val executionRoot = bazelInfo.execRoot.toFile()
  private val outputBase = bazelInfo.outputBase.toFile()
  private val workspaceRoot = WorkspaceRoot(bazelInfo.workspaceRoot)
  // private val targetMap: TargetMap?

  init {
    buildArtifactDirectories = buildArtifactDirectories(workspaceRoot)
    // this.workspacePathResolver = workspacePathResolver
    // this.targetMap = targetMap
  }

  fun resolveExecutionRootPath(path: ExecutionRootPath): File {
    if (path.isAbsolute) {
      return path.absoluteOrRelativePath.toFile()
    }
    val firstPathComponent = getFirstPathComponent(path.absoluteOrRelativePath.toString())
    if (buildArtifactDirectories.contains(firstPathComponent)) {
      // Build artifacts accumulate under the execution root, independent of symlink settings

      return path.getFileRootedAt(executionRoot)
    }
    if (firstPathComponent == "external") { // In external workspace
      // External workspaces accumulate under the output base.
      // The symlinks to them under the execution root are unstable, and only linked per build.
      return path.getFileRootedAt(outputBase)
    }
    // Else, in main workspace
    // inlined from `workspacePathResolver.resolveToFile(path.getAbsoluteOrRelativeFile().getPath());`
    return bazelInfo.workspaceRoot.resolve(path.absoluteOrRelativePath).toFile()
  }

  /**
   * This method should be used for directories. Returns all workspace files corresponding to the
   * given execution-root-relative path. If the file does not exist inside a workspace (e.g. for
   * blaze output files), returns the path rooted in the execution root.
   */
  fun resolveToIncludeDirectories(path: ExecutionRootPath): List<File> {
    if (path.isAbsolute) {
      return listOf(path.absoluteOrRelativePath.toFile())
    }
    val firstPathComponent = getFirstPathComponent(path.absoluteOrRelativePath.toString())
    if (buildArtifactDirectories.contains(firstPathComponent)) {
      // Build artifacts accumulate under the execution root, independent of symlink settings

      if (VirtualIncludesHandler.useHeuristic() && VirtualIncludesHandler.containsVirtualInclude(path)) {
        // Resolve virtual_include from execution root either to local or external workspace for correct code insight
        val resolved =
          try {
            VirtualIncludesHandler.resolveVirtualInclude(
              path,
              outputBase,
              WorkspaceRoot(bazelInfo.workspaceRoot),
              targetMap,
            )
          } catch (throwable: Throwable) {
            logger.error("Failed to resolve virtual includes for $path", throwable)
            emptyList()
          }

        return resolved.ifEmpty { listOf(path.getFileRootedAt(executionRoot)) }
      } else {
        return listOf(path.getFileRootedAt(executionRoot))
      }
    }
    if (firstPathComponent == externalPrefix) { // In external workspace
      // External workspaces accumulate under the output base.
      // The symlinks to them under the execution root are unstable, and only linked per build.
      return resolveToExternalWorkspaceWithSymbolicLinkResolution(path)
    }
    // Else, in main workspace
    val workspacePath =
      WorkspacePath.createIfValid(path.absoluteOrRelativePath.toString())
    if (workspacePath != null) {
      return listOf(workspaceRoot.fileForPath(workspacePath).toFile())
    } else {
      return emptyList()
    }
  }

  /**
   * Resolves ExecutionRootPath to external workspace location and in case if item in external
   * workspace is a link to workspace root then follows it and returns a path to workspace root
   */
  fun resolveToExternalWorkspaceWithSymbolicLinkResolution(path: ExecutionRootPath): List<File> {
    val fileInExecutionRoot: File = path.getFileRootedAt(outputBase)

    try {
      val realPath: File = fileInExecutionRoot.toPath().toRealPath().toFile()
      if (workspaceRoot.workspacePathForSafe(realPath) != null) {
        return listOf(realPath)
      }
    } catch (ioException: IOException) {
      logger.warn("Failed to resolve real path for $fileInExecutionRoot", ioException)
    }

    return listOf(fileInExecutionRoot)
  }

  fun getExecutionRoot(): File? = executionRoot

  companion object {
    private val logger: Logger = Logger.getInstance(BazelCWorkspace::class.java)
    private val externalPrefix = "external"
    val externalPath: File = File(externalPrefix)

    private fun buildArtifactDirectories(root: WorkspaceRoot): List<String> {
      val rootDir: String = root.directory.toFile().getName()
      return listOf(
        "bazel-bin",
        "bazel-genfiles",
        "bazel-out",
        "bazel-testlogs",
        "bazel-$rootDir",
      )
    }

    private fun getFirstPathComponent(path: String): String {
      val index: Int = path.indexOf(File.separatorChar)
      return if (index == -1) path else path.substring(0, index)
    }
  }
}
