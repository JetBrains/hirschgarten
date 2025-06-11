package org.jetbrains.bazel.server.sync.languages.go

import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.logger.BspClientLogger
import org.jetbrains.bazel.server.paths.BazelPathsResolver
import org.jetbrains.bazel.server.sync.languages.LanguagePlugin
import org.jetbrains.bsp.protocol.BazelResolveLocalToRemoteParams
import org.jetbrains.bsp.protocol.BazelResolveLocalToRemoteResult
import org.jetbrains.bsp.protocol.BazelResolveRemoteToLocalParams
import org.jetbrains.bsp.protocol.BazelResolveRemoteToLocalResult
import org.jetbrains.bsp.protocol.GoBuildTarget
import org.jetbrains.bsp.protocol.RawBuildTarget
import java.nio.file.Path
import java.nio.file.Paths

class GoLanguagePlugin(private val bazelPathsResolver: BazelPathsResolver, private val logger: BspClientLogger? = null) :
  LanguagePlugin<GoModule>() {
  override fun applyModuleData(moduleData: GoModule, buildTarget: RawBuildTarget) {
    val goBuildTarget =
      with(moduleData) {
        GoBuildTarget(
          sdkHomePath = sdkHomePath,
          importPath = importPath,
          generatedLibraries = generatedLibraries.distinct(),
          generatedSources = generatedSources.distinct(),
          libraryLabels = libraryLabels,
        )
      }

    buildTarget.data = goBuildTarget
  }

  override fun calculateAdditionalSources(targetInfo: BspTargetInfo.TargetInfo): List<BspTargetInfo.FileLocation> {
    if (!targetInfo.hasGoTargetInfo()) return listOf()
    return targetInfo.goTargetInfo.generatedSourcesList
  }

  override fun resolveModule(targetInfo: BspTargetInfo.TargetInfo): GoModule? {
    if (!targetInfo.hasGoTargetInfo()) return null
    val goTargetInfo = targetInfo.goTargetInfo
    return GoModule(
      sdkHomePath = calculateSdkPath(goTargetInfo.sdkHomePath),
      importPath = goTargetInfo.importPath,
      generatedSources = goTargetInfo.generatedSourcesList.mapNotNull { bazelPathsResolver.resolve(it) },
      generatedLibraries = goTargetInfo.generatedLibrariesList.mapNotNull { bazelPathsResolver.resolve(it) },
      libraryLabels = goTargetInfo.libraryLabelsList.mapNotNull { Label.parseOrNull(it) },
    )
  }

  private fun calculateSdkPath(sdk: BspTargetInfo.FileLocation?): Path? =
    sdk
      ?.takeUnless { it.relativePath.isNullOrEmpty() }
      ?.let {
        val goBinaryPath = bazelPathsResolver.resolve(it)
        goBinaryPath.parent.parent
      }

  /**
   * Converts local absolute paths to remote Bazel paths.
   */
  fun resolveLocalToRemote(params: BazelResolveLocalToRemoteParams): BazelResolveLocalToRemoteResult {
    val mapping = mutableMapOf<String, String>()
    for (local in params.localPaths) {
      try {
        val localAbs = Paths.get(local).toAbsolutePath().normalize()
        val workspaceRoot = bazelPathsResolver.workspaceRoot()
        if (localAbs.startsWith(workspaceRoot)) {
          // If inside the main workspace, get a relative Bazel path:
          val relative = bazelPathsResolver.getWorkspaceRelativePath(localAbs)
          mapping[local] = relative
        } else {
          // If outside, keep absolute path (but unify slashes):
          mapping[local] = localAbs.toString().replace("\\", "/")
        }
      } catch (e: Exception) {
        logger?.warn("Failed to resolve local path '$local': ${e.message}", e)
        mapping[local] = local
      }
    }
    return BazelResolveLocalToRemoteResult(mapping)
  }

  /**
   * Converts remote Bazel paths to local absolute paths.
   */
  fun resolveRemoteToLocal(params: BazelResolveRemoteToLocalParams): BazelResolveRemoteToLocalResult {
    val mapping = mutableMapOf<String, String>()
    for (remote in params.remotePaths) {
      try {
        val normalizedRemotePath = normalizeRemotePath(remote, params.goRoot)
        val localFile = bazelPathsResolver.resolve(normalizedRemotePath)
        val localPath = localFile.absoluteFile.normalize().path
        mapping[remote] = localPath
      } catch (e: Exception) {
        logger?.warn("Failed to resolve remote path '$remote': ${e.message}", e)
        mapping[remote] = "" // fallback
      }
    }
    return BazelResolveRemoteToLocalResult(mapping)
  }

  private fun normalizeRemotePath(path: String, goRoot: String): String =
    when {
      path.startsWith("/build/work/") ->
        afterNthSlash(path, 5)
      path.startsWith("/tmp/go-build-release/buildroot/") ->
        afterNthSlash(path, 4)
      path.startsWith("GOROOT/") -> {
        val suffix = afterNthSlash(path, 1)
        goRoot.removeSuffix("/") + "/" + suffix
      }
      else -> path
    }

  /**
   * Returns the substring after the Nth slash.
   * If the path has fewer than N slashes, returns the original path.
   */
  private fun afterNthSlash(path: String, n: Int): String {
    var index = 0
    repeat(n) {
      index = path.indexOf('/', index) + 1
      if (index == 0) {
        return path
      }
    }
    return if (index < path.length) path.substring(index) else ""
  }
}
