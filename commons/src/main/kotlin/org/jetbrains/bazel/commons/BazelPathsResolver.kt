package org.jetbrains.bazel.commons

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.info.BspTargetInfo.ArtifactLocation
import org.jetbrains.bazel.label.Canonical
import org.jetbrains.bazel.label.Main
import org.jetbrains.bazel.label.ResolvedLabel
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.Path
import kotlin.io.path.toPath

private const val BAZEL_COMPONENT_SEPARATOR = "/"

@ApiStatus.Internal
class BazelPathsResolver(private val bazelInfo: BazelInfo) {
  private val paths = ConcurrentHashMap<ArtifactLocation, Path>()

  fun workspaceRoot(): Path = bazelInfo.workspaceRoot

  fun resolvePaths(fileLocations: List<ArtifactLocation>): List<Path> = fileLocations.map(::resolve)

  fun resolve(fileLocation: ArtifactLocation): Path = paths.computeIfAbsent(fileLocation, ::doResolve)

  fun resolve(file: BuildEventStreamProtos.File): Path = URI.create(file.uri).toPath()

  private fun doResolve(fileLocation: ArtifactLocation): Path =
    when {
      isAbsolute(fileLocation) -> resolveAbsolute(fileLocation)
      isMainWorkspaceSource(fileLocation) -> resolveSource(fileLocation)
      isInExternalWorkspace(fileLocation) -> resolveExternal(fileLocation)
      else -> resolveOutput(fileLocation)
    }

  private fun isAbsolute(fileLocation: ArtifactLocation): Boolean {
    val relative = fileLocation.relativePath
    return relative.startsWith("/") && Files.exists(Paths.get(relative))
  }

  private fun resolveAbsolute(fileLocation: ArtifactLocation): Path = Paths.get(fileLocation.relativePath)

  private fun resolveExternal(fileLocation: ArtifactLocation): Path {
    val outputBaseRelativePath = Paths.get(fileLocation.rootPath, fileLocation.relativePath)
    return resolveExternal(outputBaseRelativePath)
  }

  private fun resolveExternal(outputBaseRelativePath: Path): Path =
    bazelInfo
      .outputBase
      .resolve(outputBaseRelativePath)

  fun resolveOutput(fileLocation: ArtifactLocation): Path {
    val execRootRelativePath = Paths.get(fileLocation.rootPath, fileLocation.relativePath)
    return resolveOutput(execRootRelativePath)
  }

  fun resolveOutput(execRootRelativePath: Path): Path =
    when {
      execRootRelativePath.startsWith("external") -> resolveExternal(execRootRelativePath)
      else -> bazelInfo.execRoot.resolve(execRootRelativePath)
        // If this path actually resolves to the local workspace, not bazel-out, then resolve it
        .let { runCatching { it.toRealPath() }.getOrDefault(it) }
    }

  private fun resolveSource(fileLocation: ArtifactLocation): Path = bazelInfo.workspaceRoot.resolve(fileLocation.relativePath)

  private fun isMainWorkspaceSource(fileLocation: ArtifactLocation): Boolean = fileLocation.isSource && !fileLocation.isExternal

  private fun isInExternalWorkspace(fileLocation: ArtifactLocation): Boolean = fileLocation.rootPath.startsWith("external/")

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

  fun toDirectoryPath(label: ResolvedLabel, repoMapping: RepoMapping): Path {
    val repoPath = (repoMapping as? BzlmodRepoMapping)?.let { label.toRepoPath(repoMapping) } ?: label.toRepoPathForBazel7()
    return repoPath.resolve(label.packagePath.toString())
  }

  private fun ResolvedLabel.toRepoPath(repoMapping: BzlmodRepoMapping): Path? {
    val canonicalName =
      if (repo is Canonical || repo is Main) {
        repo.repoName
      } else {
        repoMapping.apparentRepoNameToCanonicalName[repo.repoName] ?: return null
      }
    return repoMapping.canonicalRepoNameToPath[canonicalName]
  }

  private fun ResolvedLabel.toRepoPathForBazel7(): Path =
    if (repo is Main) {
      bazelInfo.workspaceRoot
    } else {
      relativePathToExecRootAbsolute(Path("external", repo.repoName, *packagePath.pathSegments.toTypedArray()))
    }
}
