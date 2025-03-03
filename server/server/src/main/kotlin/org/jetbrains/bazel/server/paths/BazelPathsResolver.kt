package org.jetbrains.bazel.server.paths

import org.jetbrains.bazel.bazelrunner.utils.BazelInfo
import org.jetbrains.bazel.info.BspTargetInfo.FileLocation
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.exists
import kotlin.io.path.toPath

private const val BAZEL_COMPONENT_SEPARATOR = "/"

class BazelPathsResolver(private val bazelInfo: BazelInfo) {
  private val uris = ConcurrentHashMap<Path, URI>()
  private val paths = ConcurrentHashMap<FileLocation, Path>()

  fun resolveUri(path: Path): URI = uris.computeIfAbsent(path, Path::toUri)

  fun unresolvedWorkspaceRoot(): Path = bazelInfo.workspaceRoot

  fun workspaceRoot(): URI = resolveUri(bazelInfo.workspaceRoot.toAbsolutePath())

  fun resolveUris(fileLocations: List<FileLocation>, shouldFilterExisting: Boolean = false): List<URI> =
    fileLocations
      .map(::resolveUri)
      .filter { !shouldFilterExisting || it.toPath().exists() }

  fun resolvePaths(fileLocations: List<FileLocation>): List<Path> = fileLocations.map(::resolve)

  fun resolveUri(fileLocation: FileLocation): URI = resolveUri(resolve(fileLocation))

  fun resolve(fileLocation: FileLocation): Path = paths.computeIfAbsent(fileLocation, ::doResolve)

  private fun doResolve(fileLocation: FileLocation): Path =
    when {
      isAbsolute(fileLocation) -> resolveAbsolute(fileLocation)
      isMainWorkspaceSource(fileLocation) -> resolveSource(fileLocation)
      isInExternalWorkspace(fileLocation) -> resolveExternal(fileLocation)
      else -> resolveOutput(fileLocation)
    }

  private fun isAbsolute(fileLocation: FileLocation): Boolean {
    val relative = fileLocation.relativePath
    return relative.startsWith("/") && Files.exists(Paths.get(relative))
  }

  private fun resolveAbsolute(fileLocation: FileLocation): Path = Paths.get(fileLocation.relativePath)

  private fun resolveExternal(fileLocation: FileLocation): Path {
    val outputBaseRelativePath = Paths.get(fileLocation.rootExecutionPathFragment, fileLocation.relativePath)
    return resolveExternal(outputBaseRelativePath)
  }

  private fun resolveExternal(outputBaseRelativePath: Path): Path =
    bazelInfo
      .outputBase
      .resolve(outputBaseRelativePath)

  private fun resolveOutput(fileLocation: FileLocation): Path {
    val execRootRelativePath = Paths.get(fileLocation.rootExecutionPathFragment, fileLocation.relativePath)
    return resolveOutput(execRootRelativePath)
  }

  fun resolveOutput(execRootRelativePath: Path): Path =
    when {
      execRootRelativePath.startsWith("external") -> resolveExternal(execRootRelativePath)
      else -> Paths.get(bazelInfo.execRoot).resolve(execRootRelativePath)
    }

  private fun resolveSource(fileLocation: FileLocation): Path = bazelInfo.workspaceRoot.resolve(fileLocation.relativePath)

  private fun isMainWorkspaceSource(fileLocation: FileLocation): Boolean = fileLocation.isSource && !fileLocation.isExternal

  private fun isInExternalWorkspace(fileLocation: FileLocation): Boolean = fileLocation.rootExecutionPathFragment.startsWith("external/")

  fun pathToDirectoryUri(path: String, isWorkspace: Boolean = true): URI {
    val absolutePath =
      if (isWorkspace) {
        relativePathToWorkspaceAbsolute(path)
      } else {
        relativePathToExecRootAbsolute(path)
      }
    return resolveUri(absolutePath)
  }

  fun relativePathToWorkspaceAbsolute(path: String): Path = bazelInfo.workspaceRoot.resolve(path)

  fun relativePathToExecRootAbsolute(path: String): Path = Paths.get(bazelInfo.execRoot, path)

  fun clear() {
    uris.clear()
    paths.clear()
  }

  /**
   * converts a path object to a relative path string with Bazel separator
   */
  fun getWorkspaceRelativePath(path: Path): String =
    bazelInfo.workspaceRoot
      .relativize(path)
      .toString()
      .replace(File.separator, BAZEL_COMPONENT_SEPARATOR)

  fun resolve(path: String): File =
    when {
      Paths.get(path).isAbsolute -> File(path)
      path.startsWith("external/") -> bazelInfo.outputBase.resolve(path).toFile()
      else -> bazelInfo.workspaceRoot.resolve(path).toFile()
    }
}
