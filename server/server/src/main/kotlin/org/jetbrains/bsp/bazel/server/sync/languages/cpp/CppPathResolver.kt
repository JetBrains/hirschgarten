package org.jetbrains.bsp.bazel.server.sync.languages.cpp

import org.jetbrains.bsp.bazel.info.BspTargetInfo.TargetInfo
import org.jetbrains.bsp.bazel.server.paths.BazelPathsResolver
import java.io.IOException
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

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
  fun resolve(path: String): URI = resolveToPath(path).toUri()

  fun resolveToPath(path: String): Path =
    when {
      isAbsolute(path) -> Path.of(path)
      isInExternalWorkspace(path) -> bazelPathsResolver.resolveExternal(Path.of(path))
      isArtifact(path) -> bazelPathsResolver.resolveOutput(Path.of(path))
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
    val refinedTargetMap = targetMap.map { (targetName, targetInfo) -> targetName.removePrefix("@@") to targetInfo }.toMap()
    return when {
      // For absolute path, it remains as it is
      isAbsolute(path.toString()) -> listOf(path.toAbsolutePath().toUri())
      isArtifact(path.toString()) -> resolveArtifactHeaders(path, refinedTargetMap)
      isInExternalWorkspace(path.toString()) -> resolveExternalHeaders(path)
      // For other relative paths, it resolves to workspace
      // todo: validate the path
      else ->
        listOf(
          bazelInfo.workspaceRoot
            .resolve(path)
            .normalize()
            .toAbsolutePath()
            .toUri(),
        )
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
    val fileInExecutionRoot = path.getFileRootedAt(Path.of(bazelInfo.execRoot))
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

  private fun isArtifact(path: String): Boolean = buildArtifactDirectories.contains(Paths.get(path).getFirstComponent())

  private fun isInExternalWorkspace(path: String): Boolean = path.startsWith("external/") || path.startsWith("../")

  private fun isAbsolute(path: String): Boolean = path.startsWith("/") && Files.exists(Paths.get(path))

  companion object CppPathResolver {
    fun Path.getFileRootedAt(absoluteRoot: Path): Path =
      if (isAbsolute) {
        this
      } else {
        absoluteRoot.resolve(this)
      }
  }

  fun Path.getLastComponent(): String = getName(count() - 1).toString()

  fun Path.getFirstComponent(): String = getName(0).toString()
}
