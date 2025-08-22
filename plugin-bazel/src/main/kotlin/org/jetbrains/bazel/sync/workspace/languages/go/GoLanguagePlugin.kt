package org.jetbrains.bazel.sync.workspace.languages.go

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.languages.LanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.LanguagePluginContext
import org.jetbrains.bsp.protocol.BazelResolveLocalToRemoteParams
import org.jetbrains.bsp.protocol.BazelResolveLocalToRemoteResult
import org.jetbrains.bsp.protocol.BazelResolveRemoteToLocalParams
import org.jetbrains.bsp.protocol.BazelResolveRemoteToLocalResult
import org.jetbrains.bsp.protocol.GoBuildTarget
import java.nio.file.Path
import java.nio.file.Paths

class GoLanguagePlugin(private val bazelPathsResolver: BazelPathsResolver) : LanguagePlugin<GoBuildTarget> {
  private val logger: Logger = logger<GoLanguagePlugin>()

  override fun getSupportedLanguages(): Set<LanguageClass> = setOf(LanguageClass.GO)

  override fun resolveExtraSources(targetInfo: BspTargetInfo.TargetInfo): Sequence<Path> {
    if (!targetInfo.hasGoTargetInfo()) return emptySequence()
    return targetInfo.goTargetInfo.generatedSourcesList
      .asSequence()
      .mapNotNull { bazelPathsResolver.resolve(it) }
  }

  override suspend fun createBuildTargetData(context: LanguagePluginContext, target: BspTargetInfo.TargetInfo): GoBuildTarget? {
    if (!target.hasGoTargetInfo()) {
      return null
    }
    val goTarget = target.goTargetInfo
    return GoBuildTarget(
      sdkHomePath = calculateSdkPath(goTarget.sdkHomePath),
      importPath = goTarget.importPath,
      generatedSources = goTarget.generatedSourcesList.mapNotNull { bazelPathsResolver.resolve(it) },
      generatedLibraries = goTarget.generatedLibrariesList.mapNotNull { bazelPathsResolver.resolve(it) },
      libraryLabels = goTarget.libraryLabelsList.mapNotNull { Label.parseOrNull(it) },
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
        logger.warn("Failed to resolve local path '$local': ${e.message}", e)
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
        logger.warn("Failed to resolve remote path '$remote': ${e.message}", e)
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
