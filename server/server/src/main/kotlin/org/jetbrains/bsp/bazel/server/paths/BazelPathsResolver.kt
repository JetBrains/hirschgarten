package org.jetbrains.bsp.bazel.server.paths

import org.jetbrains.bsp.bazel.bazelrunner.utils.BazelInfo
import org.jetbrains.bsp.bazel.info.BspTargetInfo.FileLocation
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.exists
import kotlin.io.path.toPath

private const val BAZEL_COMPONENT_SEPARATOR = "/"

open class BazelPathsResolver(val bazelInfo: BazelInfo) {
  private val uris = ConcurrentHashMap<Path, URI>()
  private val paths = ConcurrentHashMap<FileLocation, Path>()

  protected val buildArtifactDirectories =
    listOf(
      "bazel-bin",
      "bazel-genfiles",
      "bazel-out",
      "bazel-testlogs",
      "bazel-${bazelInfo.workspaceRoot.getLastComponent()}",
    )

  fun resolveUri(path: Path): URI = uris.computeIfAbsent(path, Path::toUri)

  fun resolveUri(fileLocation: FileLocation): URI = resolveUri(resolve(fileLocation))

  fun resolveUri(path: String): URI = resolveUri(resolve(path))

  fun unresolvedWorkspaceRoot(): Path = bazelInfo.workspaceRoot

  fun workspaceRoot(): URI = resolveUri(bazelInfo.workspaceRoot.toAbsolutePath())

  fun resolve(fileLocation: FileLocation): Path = paths.computeIfAbsent(fileLocation, ::doResolve)

  fun resolve(path: String): Path = doResolve(path)

  fun resolveUris(fileLocations: List<FileLocation>, shouldFilterExisting: Boolean = false): List<URI> =
    fileLocations
      .map(::resolveUri)
      .filter { !shouldFilterExisting || it.toPath().exists() }

  fun resolvePaths(fileLocations: List<FileLocation>): List<Path> = fileLocations.map(::resolve)

  private fun doResolve(fileLocation: FileLocation): Path =
    when {
      isAbsolute(fileLocation) -> resolveAbsolute(fileLocation)
      isMainWorkspaceSource(fileLocation) -> resolveSource(fileLocation)
      isInExternalWorkspace(fileLocation) -> resolveExternal(fileLocation)
      else -> resolveOutput(fileLocation)
    }

  private fun doResolve(path: String): Path =
    when {
      isAbsolute(path) -> resolveAbsolute(path)
      isInExternalWorkspace(path) -> resolveExternal(path)
      isArtifact(path) -> resolveOutput(path)
      else -> resolveSource(path)
    }

  private fun resolveAbsolute(fileLocation: FileLocation): Path = Paths.get(fileLocation.relativePath)

  private fun resolveAbsolute(path: String): Path = Paths.get(path)

  private fun resolveExternal(fileLocation: FileLocation): Path {
    val outputBaseRelativePath = Paths.get(fileLocation.rootExecutionPathFragment, fileLocation.relativePath)
    return resolveExternal(outputBaseRelativePath)
  }

  private fun resolveExternal(path: String): Path = resolveExternal(Paths.get(path))

  private fun resolveExternal(outputBaseRelativePath: Path): Path =
    bazelInfo
      .outputBase
      .resolve(outputBaseRelativePath)

  private fun resolveOutput(fileLocation: FileLocation): Path {
    val execRootRelativePath = Paths.get(fileLocation.rootExecutionPathFragment, fileLocation.relativePath)
    return resolveOutput(execRootRelativePath)
  }

  private fun resolveOutput(path: String): Path = resolveOutput(Paths.get(path))

  fun resolveOutput(execRootRelativePath: Path): Path =
    when {
      execRootRelativePath.startsWith("external") -> resolveExternal(execRootRelativePath)
      else -> Paths.get(bazelInfo.execRoot).resolve(execRootRelativePath)
    }

  private fun resolveSource(fileLocation: FileLocation): Path = bazelInfo.workspaceRoot.resolve(fileLocation.relativePath)

  private fun resolveSource(path: String): Path = bazelInfo.workspaceRoot.resolve(path)

  private fun isAbsolute(fileLocation: FileLocation): Boolean {
    val relative = fileLocation.relativePath
    return relative.startsWith("/") && Files.exists(Paths.get(relative))
  }

  protected fun isAbsolute(path: String): Boolean = path.startsWith("/") && Files.exists(Paths.get(path))

  private fun isMainWorkspaceSource(fileLocation: FileLocation): Boolean = fileLocation.isSource && !fileLocation.isExternal

  protected fun isArtifact(path: String): Boolean = buildArtifactDirectories.contains(Paths.get(path).getFirstComponent())

  private fun isInExternalWorkspace(fileLocation: FileLocation): Boolean = fileLocation.rootExecutionPathFragment.startsWith("external/")

  protected fun isInExternalWorkspace(path: String): Boolean = path.startsWith("external/")

  protected fun Path.getLastComponent(): String = getName(count() - 1).toString()

  protected fun Path.getFirstComponent(): String = getName(0).toString()

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
}
