package org.jetbrains.bsp.bazel.server.sync.languages.cpp

import org.jetbrains.bsp.bazel.info.BspTargetInfo.TargetInfo
import org.jetbrains.bsp.bazel.server.paths.BazelPathsResolver
import java.io.IOException
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path

class CppPathResolver(val bazelPathsResolver: BazelPathsResolver) {
  private val virtualIncludeDirectory: Path = Path.of("_virtual_includes")
  private val virtualIncludesHandler: VirtualIncludesHandler = VirtualIncludesHandler(this)
  val bazelInfo = bazelPathsResolver.bazelInfo
  val buildArtifactDirectories =
    listOf(
      "bazel-bin",
      "bazel-genfiles",
      "bazel-out",
      "bazel-testlogs",
      "bazel-${bazelInfo.workspaceRoot.getLastComponent()}",
    )

  /**
   * Resolve a normal path string in cpp options to correct absolute uri
   * */
  fun resolve(path: Path): URI = resolveToPath(path).toUri()

  fun resolveToPath(path: Path): Path =

    when {
      isAbsolute(path) -> path
      isInExternalWorkspace(path) -> bazelPathsResolver.resolveExternal(path)
      isArtifact(path) -> bazelPathsResolver.resolveOutput(path)
      else -> bazelInfo.workspaceRoot.resolve(path)
    }

  /**
   * Resolve a cpp include path to correct absolute uri
   * - For absolute path, it remains as it is
   * - For relative path in  build artifacts, it resolves to folders in executionRoot
   * - For realative path in build artifacts' _virtual_include, it resolves back to corresponding file in workspace
   * - For external path, resolve to its real path; if real path is under workspace, resolve to workspace
   * - For other relative paths, it resolves to workspace
   * */
  fun resolveToIncludeDirectories(path: Path, targetMap: Map<String, TargetInfo>): List<URI> {
    val refinedTargetMap =
      targetMap.map { (targetName, targetInfo) -> targetName.removePrefix("@@") to targetInfo }.toMap()
    return when {
      // For absolute path, it remains as it is
      isAbsolute(path) -> listOf(path.toAbsolutePath().toUri())
      isArtifact(path) -> resolveArtifactHeaders(path, refinedTargetMap)
      isInExternalWorkspace(path) -> resolveExternalHeaders(path)
      // For other relative paths, it resolves to workspace
      else -> {
        if (!isValidWorkspacePath(path))return listOf()
        listOf(
          bazelInfo.workspaceRoot
            .resolve(path)
            .normalize()
            .toAbsolutePath()
            .toUri(),
        )
      }
    }
  }

  private fun resolveArtifactHeaders(path: Path, targetMap: Map<String, TargetInfo>): List<URI> {
    if (containsVirtualInclude(path)) {
      val result = virtualIncludesHandler.resolveVirtualInclude(path, bazelInfo.outputBase, targetMap)
      if (result.isNotEmpty()) return result
    }
    return listOf(path.getFileRootedAt(Path.of(bazelInfo.execRoot)).toAbsolutePath().toUri())
  }

  private fun resolveExternalHeaders(path: Path): List<URI> {
    // For external path, resolve to its real path
    val fileInExecutionRoot = path.getFileRootedAt(bazelInfo.outputBase)
    try {
      val realPath = fileInExecutionRoot.toRealPath()
      if (isRealFileInWorkspace(realPath)) {
        return listOf(realPath.toAbsolutePath().toUri())
      }
    } catch (e: IOException) {
      // todo: log something
    }
    return listOf(fileInExecutionRoot.toAbsolutePath().toUri())
  }

  private fun containsVirtualInclude(executionRootPath: Path): Boolean = executionRootPath.contains(virtualIncludeDirectory)

  private fun isRealFileInWorkspace(path: Path): Boolean {
    if (!path.toFile().exists()) return false
    return path.startsWith(bazelInfo.workspaceRoot)
  }

  private fun isArtifact(path: Path): Boolean = buildArtifactDirectories.contains(path.getFirstComponent())

  private fun isInExternalWorkspace(path: Path): Boolean = path.startsWith("external/") || path.startsWith("../")

  private fun isAbsolute(path: Path): Boolean = path.startsWith("/") && Files.exists(path)

  companion object CppPathResolver {
    fun Path.getFileRootedAt(absoluteRoot: Path): Path =
      if (isAbsolute) {
        this
      } else {
        absoluteRoot.resolve(this)
      }
  }

  private fun isValidWorkspacePath(path: Path) =
    !path.startsWith("/") && !path.startsWith("..") && !path.endsWith("/") && !path.toString().contains(":")

  fun Path.getLastComponent(): String = if (this.count() == 0) "" else getName(count() - 1).toString()

  fun Path.getFirstComponent(): String = if (this.count() == 0) "" else getName(0).toString()
}
