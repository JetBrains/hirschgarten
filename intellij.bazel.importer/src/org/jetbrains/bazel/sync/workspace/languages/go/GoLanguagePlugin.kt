package org.jetbrains.bazel.sync.workspace.languages.go

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.LocalRepositoryMapping
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.commons.getLocalRepositories
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.info.BspTargetInfo.TargetInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.createOutputFileHardLinks
import org.jetbrains.bazel.sync.workspace.graph.DependencyGraph
import org.jetbrains.bazel.sync.workspace.languages.LanguagePlugin
import org.jetbrains.bsp.protocol.BazelResolveLocalToRemoteParams
import org.jetbrains.bsp.protocol.BazelResolveLocalToRemoteResult
import org.jetbrains.bsp.protocol.BazelResolveRemoteToLocalParams
import org.jetbrains.bsp.protocol.BazelResolveRemoteToLocalResult
import org.jetbrains.bsp.protocol.BazelServerFacade
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.GoBuildTarget
import java.nio.file.Path
import java.nio.file.Paths

@ApiStatus.Internal
class GoLanguagePlugin: LanguagePlugin {
  override fun getSupportedLanguages(): Set<LanguageClass> = setOf(LanguageClass.GO)
  override fun createProjectMapper(project: Project, server: BazelServerFacade) = Mapper(server)

  class Mapper(private val server: BazelServerFacade) : LanguagePlugin.Mapper {

    override suspend fun createBuildTargetData(
      target: TargetInfo,
      targetsToImport: Map<Label, TargetInfo>,
      graph: DependencyGraph,
      repoMapping: RepoMapping,
    ): List<BuildTargetData> {
      if (!target.hasGoTargetInfo()) {
        return emptyList()
      }
      val goTarget = target.goTargetInfo
      val localRepositories = repoMapping.getLocalRepositories()
      return listOf(
        GoBuildTarget(
          sdkHomePath = calculateSdkPath(goTarget.sdkHomePath, localRepositories),
          importPath = goTarget.importPath,
          generatedSources = server.outFileHardLinks.createOutputFileHardLinks(
            goTarget.generatedSourcesList.map { server.bazelPathsResolver.resolve(it, localRepositories) },
          ),
          generatedLibraries = server.outFileHardLinks.createOutputFileHardLinks(
            goTarget.generatedLibrariesList.map { server.bazelPathsResolver.resolve(it, localRepositories) },
          ),
          libraryLabels = goTarget.libraryLabelsList.mapNotNull { Label.parseOrNull(it) },
        ),
      )
    }

    private fun calculateSdkPath(sdk: BspTargetInfo.ArtifactLocation?, localRepositories: LocalRepositoryMapping): Path? =
      sdk
        ?.takeUnless { it.relativePath.isNullOrEmpty() }
        ?.let {
          val goBinaryPath = server.bazelPathsResolver.resolve(it, localRepositories)
          goBinaryPath.parent.parent
        }
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
      val mapping = mutableMapOf<String, String>()
      for (remote in params.remotePaths) {
        try {
          val normalizedRemotePath = normalizeRemotePath(remote, params.goRoot)
          val localFile = pathsResolver.resolve(normalizedRemotePath)
          val localPath = localFile.absoluteFile.normalize().path
          mapping[remote] = localPath
        }
        catch (e: Exception) {
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
}
