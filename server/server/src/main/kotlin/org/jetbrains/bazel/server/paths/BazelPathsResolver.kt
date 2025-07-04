package org.jetbrains.bazel.server.paths

import org.jetbrains.bazel.bazelrunner.utils.BazelInfo
import org.jetbrains.bazel.info.FileLocation
import org.jetbrains.bazel.label.Canonical
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.bzlmod.BzlmodRepoMapping
import org.jetbrains.bazel.server.bzlmod.RepoMapping
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.Path

private const val BAZEL_COMPONENT_SEPARATOR = "/"

class BazelPathsResolver(private val bazelInfo: BazelInfo) {
  private val paths = ConcurrentHashMap<FileLocation, Path>()

  fun workspaceRoot(): Path = bazelInfo.workspaceRoot

  fun resolvePaths(fileLocations: List<FileLocation>): List<Path> = fileLocations.map(::resolve)

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

  fun resolveOutput(fileLocation: FileLocation): Path {
    val execRootRelativePath = Paths.get(fileLocation.rootExecutionPathFragment, fileLocation.relativePath)
    return resolveOutput(execRootRelativePath)
  }

  fun resolveOutput(execRootRelativePath: Path): Path =
    when {
      execRootRelativePath.startsWith("external") -> resolveExternal(execRootRelativePath)
      else -> bazelInfo.execRoot.resolve(execRootRelativePath)
    }

  private fun resolveSource(fileLocation: FileLocation): Path = bazelInfo.workspaceRoot.resolve(fileLocation.relativePath)

  private fun isMainWorkspaceSource(fileLocation: FileLocation): Boolean = fileLocation.isSource && !fileLocation.isExternal

  private fun isInExternalWorkspace(fileLocation: FileLocation): Boolean = fileLocation.rootExecutionPathFragment.startsWith("external/")

  fun relativePathToWorkspaceAbsolute(path: Path): Path = bazelInfo.workspaceRoot.resolve(path)

  fun relativePathToExecRootAbsolute(path: Path): Path = bazelInfo.execRoot.resolve(path)

  fun clear() {
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

  fun resolve(path: Path): Path =
    when {
      path.isAbsolute -> path
      path.startsWith("external/") -> bazelInfo.outputBase.resolve(path)
      else -> bazelInfo.workspaceRoot.resolve(path)
    }

  // TODO: it's used only in go plugin but I don't feel competent to change it
  fun resolve(path: String): File =
    when {
      Paths.get(path).isAbsolute -> File(path)
      path.startsWith("external/") -> bazelInfo.outputBase.resolve(path).toFile()
      else -> bazelInfo.workspaceRoot.resolve(path).toFile()
    }

  fun toDirectoryPath(label: Label, repoMapping: RepoMapping): Path {
    val repoPath = (repoMapping as? BzlmodRepoMapping)?.let { label.toRepoPath(repoMapping) } ?: label.toRepoPathForBazel7()
    return repoPath.resolve(label.packagePath.toString())
  }

  private fun Label.toRepoPath(repoMapping: BzlmodRepoMapping): Path? {
    val canonicalName =
      if (repo is Canonical || repo.isMain) {
        repo.repoName
      } else {
        repoMapping.apparentRepoNameToCanonicalName[repo.repoName] ?: return null
      }
    return repoMapping.canonicalRepoNameToPath[canonicalName]
  }

  private fun Label.toRepoPathForBazel7(): Path =
    if (repo.isMain) {
      bazelInfo.workspaceRoot
    } else {
      relativePathToExecRootAbsolute(Path("external", repo.repoName, *packagePath.pathSegments.toTypedArray()))
    }
}
