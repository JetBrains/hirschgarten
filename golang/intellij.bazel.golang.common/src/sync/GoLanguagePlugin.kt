package org.jetbrains.bazel.golang.sync

import com.google.devtools.intellij.aspect.Common
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.TargetIdeInfo
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.LocalRepositoryMapping
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.commons.getLocalRepositories
import org.jetbrains.bazel.golang.GoLanguageClass
import org.jetbrains.bazel.server.BazelServerFacade
import org.jetbrains.bazel.sync.workspace.languages.LanguagePlugin
import org.jetbrains.bazel.sync.workspace.snapshot.toWorkspaceTargetKey
import org.jetbrains.bsp.protocol.BazelResolveLocalToRemoteParams
import org.jetbrains.bsp.protocol.BazelResolveLocalToRemoteResult
import org.jetbrains.bsp.protocol.BazelResolveRemoteToLocalParams
import org.jetbrains.bsp.protocol.BazelResolveRemoteToLocalResult
import org.jetbrains.bsp.protocol.BuildTargetData
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.absolute
import kotlin.reflect.KClass

@ApiStatus.Internal
class GoLanguagePlugin: LanguagePlugin {
  override val providedBuildTargetTypes: Set<KClass<out BuildTargetData>>
    get() = setOf(GoBuildTarget::class)

  override fun getSupportedLanguages(): Set<LanguageClass> = setOf(GoLanguageClass.GO)

  override fun collectUsedLanguages(target: TargetIdeInfo): List<LanguageClass> {
    if (target.hasGoTargetInfo())
      return listOf(GoLanguageClass.GO)
    return emptyList()
  }

  override suspend fun mapBuildTargetData(
    server: BazelServerFacade,
    target: TargetIdeInfo,
    repoMapping: RepoMapping,
  ): List<BuildTargetData> {
    if (!target.hasGoTargetInfo()) {
      return emptyList()
    }
    val goTarget = target.goTargetInfo
    val localRepositories = repoMapping.getLocalRepositories()
    return listOf(
      GoBuildTarget(
        sdkHomePath = calculateSdkPath(server, goTarget.sdkHomePath, localRepositories),
        importPath = goTarget.importPath,
        sources = server.outFileHardLinks.createOutputFileHardLinks(
          goTarget.sourcesList.map { server.bazelPathsResolver.resolve(it, localRepositories) },
        ),
        embed = goTarget.embedList.map { it.toWorkspaceTargetKey() },
      ),
    )
  }

  private fun calculateSdkPath(server: BazelServerFacade, sdk: Common.ArtifactLocation?, localRepositories: LocalRepositoryMapping): Path? =
    sdk
      ?.takeUnless { it.relativePath.isNullOrEmpty() }
      ?.let {
        val goBinaryPath = server.bazelPathsResolver.resolve(it, localRepositories)
        goBinaryPath.parent.parent
      }

  companion object {
    private val logger: Logger = logger<GoLanguagePlugin>()

    /**
     * Converts local absolute paths to remote Bazel paths.
     */
    fun resolveLocalToRemote(pathsResolver: BazelPathsResolver, params: BazelResolveLocalToRemoteParams): BazelResolveLocalToRemoteResult {
      val mapping = mutableMapOf<String, String>()
      for (local in params.localPaths) {
        try {
          val localAbs = Paths.get(local).toAbsolutePath().normalize()
          val workspaceRoot = pathsResolver.workspaceRoot()
          if (localAbs.startsWith(workspaceRoot)) {
            // If inside the main workspace, get a relative Bazel path:
            val relative = pathsResolver.getWorkspaceRelativePath(localAbs)
            mapping[local] = relative
          }
          else {
            // If outside, keep absolute path (but unify slashes):
            mapping[local] = localAbs.toString().replace("\\", "/")
          }
        }
        catch (e: Exception) {
          logger.warn("Failed to resolve local path '$local': ${e.message}", e)
          mapping[local] = local
        }
      }
      return BazelResolveLocalToRemoteResult(mapping)
    }

    /**
     * Converts remote Bazel paths to local absolute paths.
     */
    fun resolveRemoteToLocal(pathsResolver: BazelPathsResolver, params: BazelResolveRemoteToLocalParams): BazelResolveRemoteToLocalResult {
      val mapping = mutableMapOf<String, Path>()
      for (remote in params.remotePaths) {
        try {
          val normalizedRemotePath = Path.of(normalizeRemotePath(remote, params.goRoot))
          val localPath = pathsResolver.resolve(normalizedRemotePath).absolute().normalize()
          mapping[remote] = localPath
        }
        catch (e: Exception) {
          logger.warn("Failed to resolve remote path '$remote': ${e.message}", e)
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
}
