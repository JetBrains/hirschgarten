package org.jetbrains.bazel.commons

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos
import com.google.devtools.intellij.aspect.Common.ArtifactLocation
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.label.Canonical
import org.jetbrains.bazel.label.Main
import org.jetbrains.bazel.label.RepoType
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.label.toPath
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.Path
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.toPath

@ApiStatus.Internal
class BazelPathsResolver(private val bazelInfo: BazelInfo) {
  fun workspaceRoot(): Path = bazelInfo.workspaceRoot

  fun bazelBin(): Path = bazelInfo.bazelBin

  fun resolvePaths(fileLocations: List<ArtifactLocation>, localRepositories: LocalRepositoryMapping): List<Path> =
    fileLocations.map { resolve(it, localRepositories) }

  fun resolve(file: BuildEventStreamProtos.File): Path = URI.create(file.uri).toPath()

  fun isExternal(
    fileLocation: ArtifactLocation,
    localRepositories: LocalRepositoryMapping
  ): Boolean = when {
    fileLocation.rootPath.startsWith("../") || fileLocation.rootPath.startsWith("external/") -> {
      val rootSegments = fileLocation.rootPath.split('/')
      !localRepositories.localRepositories.contains(rootSegments[1])
    }

    fileLocation.relativePath.startsWith("external/") -> {
      val relativeSegments = fileLocation.relativePath.split('/')
      !localRepositories.localRepositories.contains(relativeSegments[1])
    }

    else -> false
  }

  private fun mapLocalRepositories(fileLocation: ArtifactLocation, localRepositories: LocalRepositoryMapping): ArtifactLocation {
    if (!(fileLocation.rootPath.startsWith("../") || fileLocation.rootPath.startsWith("external/"))) return fileLocation
    val rootSegments = fileLocation.rootPath.split('/')
    val localPath: Path = localRepositories.localRepositories[rootSegments[1]] ?: return fileLocation
    // Update relative path, keeping bazel's file-system hierarchy separator
    val newRelativePath = localPath.invariantSeparatorsPathString + "/" + fileLocation.relativePath
    return ArtifactLocation.newBuilder().setRootPath("").setRelativePath(newRelativePath).setIsSource(fileLocation.isSource).build()
  }

  fun resolve(fileLocation: ArtifactLocation, localRepositories: LocalRepositoryMapping): Path {
    val mappedFileLocation = mapLocalRepositories(fileLocation, localRepositories)
    return when {
      isAbsolute(mappedFileLocation) -> resolveAbsolute(mappedFileLocation)
      isMainWorkspaceSource(mappedFileLocation, localRepositories) -> resolveSource(mappedFileLocation)
      isInExternalWorkspace(mappedFileLocation) -> resolveExternal(mappedFileLocation)
      else -> resolveOutput(mappedFileLocation)
    }
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
    }

  private fun resolveSource(fileLocation: ArtifactLocation): Path = bazelInfo.workspaceRoot.resolve(fileLocation.relativePath)

  private fun isMainWorkspaceSource(fileLocation: ArtifactLocation, localRepositories: LocalRepositoryMapping): Boolean =
    fileLocation.isSource && !isExternal(fileLocation, localRepositories)

  private fun isInExternalWorkspace(fileLocation: ArtifactLocation): Boolean = fileLocation.rootPath.startsWith("external/")

  fun relativePathToWorkspaceAbsolute(path: Path): Path = bazelInfo.workspaceRoot.resolve(path)

  fun relativePathToExecRootAbsolute(path: Path): Path = bazelInfo.execRoot.resolve(path)

  /**
   * converts a path object to a relative path string with Bazel separator
   */
  fun getWorkspaceRelativePath(path: Path): String =
    bazelInfo.workspaceRoot
      .relativize(path)
      .invariantSeparatorsPathString

  fun resolve(path: Path): Path =
    when {
      path.isAbsolute -> path
      path.startsWith("external/") -> bazelInfo.outputBase.resolve(path)
      else -> bazelInfo.workspaceRoot.resolve(path)
    }

  fun toDirectoryPath(label: ResolvedLabel, repoMapping: RepoMapping): Path =
    toRepositoryPath(label.repo, repoMapping).resolve(label.packagePath.toPath())

  /**
   * Returns the root directory of the repository of [repo].
   * This is the workspace root for the main repository and the source directory for a local repository.
   * For another external repository, this is its directory under the output base.
   * The path is normalized, because a `local_path_override` path can contain `.` and `..` segments.
   */
  fun toRepositoryPath(repo: RepoType, repoMapping: RepoMapping): Path =
    ((repoMapping as? BzlmodRepoMapping)?.let { repo.toRepoPath(it) } ?: repo.toRepoPathForBazel7()).normalize()

  /**
   * Returns the directory under `bazel-bin` that holds the outputs of the repository of [repo].
   */
  fun toBinRepositoryPath(repo: RepoType, repoMapping: RepoMapping): Path {
    if (repo is Main) return bazelInfo.bazelBin
    val canonicalName = (repoMapping as? BzlmodRepoMapping)?.let { repo.canonicalRepoName(it) } ?: repo.repoName
    return bazelInfo.bazelBin.resolve("external").resolve(canonicalName)
  }

  private fun RepoType.toRepoPath(repoMapping: BzlmodRepoMapping): Path? =
    canonicalRepoName(repoMapping)?.let { repoMapping.canonicalRepoNameToPath[it] }

  private fun RepoType.canonicalRepoName(repoMapping: BzlmodRepoMapping): String? =
    if (this is Canonical || this is Main) {
      repoName
    }
    else {
      repoMapping.apparentRepoNameToCanonicalName[repoName]
    }

  private fun RepoType.toRepoPathForBazel7(): Path =
    if (this is Main) {
      bazelInfo.workspaceRoot
    }
    else {
      relativePathToExecRootAbsolute(Path("external", repoName))
    }
}
