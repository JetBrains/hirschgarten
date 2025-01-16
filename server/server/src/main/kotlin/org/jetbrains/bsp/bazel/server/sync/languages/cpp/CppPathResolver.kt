package org.jetbrains.bsp.bazel.server.sync.languages.cpp

import org.apache.logging.log4j.LogManager
import org.jetbrains.bsp.bazel.bazelrunner.utils.BazelInfo
import org.jetbrains.bsp.bazel.info.BspTargetInfo.TargetInfo
import org.jetbrains.bsp.bazel.server.paths.BazelPathsResolver
import java.io.IOException
import java.net.URI
import java.nio.file.Path

class CppPathResolver(bazelInfo: BazelInfo) : BazelPathsResolver(bazelInfo) {
  private val log = LogManager.getLogger(CppPathResolver::class.java)
  private val VIRTUAL_INCLUDES_DIRECTORY: Path = Path.of("_virtual_includes")
  private val virtualIncludesHandler: VirtualIncludesHandler = VirtualIncludesHandler(this)

  /**
   * Resolve a cpp include path to correct absolute path
   * - For absolute path, it remains as it is
   * - For relative path in  build artifacts, it resolves to folders in executionRoot
   * - For realative path in build artifacts' _virtual_include, it resolves back to corresponding file in workspace
   * - For external path, resolve to its real path; if real path is under workspace, resolve to workspace
   * - For other relative paths, it resolves to workspace
   * */
  fun resolveToIncludeDirectories(path: Path, targetMap: Map<String, TargetInfo>): List<URI> {
    val refinedTargetMap=targetMap.map { (targetName, targetInfo) ->targetName.removePrefix("@@") to targetInfo}.toMap()
    return when {
      // For absolute path, it remains as it is
      path.isAbsolute -> listOf(path.toAbsolutePath().toUri())
      isArtifact(path.toString()) -> resolveArtifactHeaders(path, refinedTargetMap)
      isInExternalWorkspace(path.toString()) -> resolveExternalHeaders(path)
      // For other relative paths, it resolves to workspace
      //todo: validate the path
      else -> listOf(bazelInfo.workspaceRoot.resolve(path).normalize().toAbsolutePath().toUri())
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
      //todo: log something
      log.info("Failed to resolve real path for ${fileInExecutionRoot.toRealPath()}: ${e.message},${e.stackTrace.joinToString("\n")}")
    }
    return listOf(fileInExecutionRoot.toAbsolutePath().toUri())
  }


  private fun containsVirtualInclude(executionRootPath: Path): Boolean = executionRootPath.contains(VIRTUAL_INCLUDES_DIRECTORY)
  private fun isRealFileInWorkspace(path: Path): Boolean {
    if (!path.toFile().exists()) return false
    return path.startsWith(bazelInfo.workspaceRoot)
  }

  companion object {
    fun Path.getFileRootedAt(absoluteRoot: Path): Path =
      if (isAbsolute) {
        this
      } else {
        absoluteRoot.resolve(this)
      }

  }

}
