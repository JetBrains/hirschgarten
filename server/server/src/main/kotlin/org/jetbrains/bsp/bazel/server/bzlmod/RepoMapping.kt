package org.jetbrains.bsp.bazel.server.bzlmod

import org.jetbrains.bsp.bazel.bazelrunner.BazelRunner
import org.jetbrains.bsp.bazel.bazelrunner.ModuleOutputParser
import org.jetbrains.bsp.bazel.bazelrunner.ModuleResolver
import org.jetbrains.bsp.bazel.bazelrunner.ShowRepoResult
import org.jetbrains.bsp.bazel.logger.BspClientLogger
import org.jetbrains.bsp.bazel.server.model.Label
import org.jetbrains.bsp.bazel.workspacecontext.WorkspaceContextProvider
import org.jetbrains.bsp.bazel.workspacecontext.externalRepositoriesTreatedAsInternal
import java.nio.file.Path
import kotlin.io.path.Path

sealed interface RepoMapping

data class BzlmodRepoMapping(
  val moduleCanonicalNameToLocalPath: Map<String, Path>,
  val moduleApparentNameToCanonicalName: Map<String, String>,
) : RepoMapping

data object RepoMappingDisabled : RepoMapping

fun Label.canonicalize(repoMapping: RepoMapping): Label {
  if (!this.isApparent) {
    return this
  }

  when (repoMapping) {
    is RepoMappingDisabled -> return this
    is BzlmodRepoMapping -> {
      val apparentRepoName = this.repoName
      val canonicalRepoName = repoMapping.moduleApparentNameToCanonicalName[apparentRepoName] ?: error("No canonical name found for $this")
      return Label.parse("@@$canonicalRepoName//$targetPathAndName")
    }
  }
}

fun calculateRepoMapping(
  workspaceContextProvider: WorkspaceContextProvider,
  bazelRunner: BazelRunner,
  isBzlmod: Boolean,
  bspClientLogger: BspClientLogger,
): RepoMapping {
  if (!isBzlmod) {
    return RepoMappingDisabled
  }
  val workspaceContext = workspaceContextProvider.currentWorkspaceContext()
  val moduleResolver = ModuleResolver(bazelRunner, ModuleOutputParser())
  val moduleCanonicalNameToLocalPath = mutableMapOf<String, Path>()
  // empty string is the name of the root module
  val moduleApparentNameToCanonicalName = moduleResolver.getRepoMapping("") { }
  for (externalRepo in workspaceContext.externalRepositoriesTreatedAsInternal) {
    try {
      val showRepoResult = moduleResolver.resolveModule(externalRepo) {}
      when (showRepoResult) {
        is ShowRepoResult.LocalRepository -> moduleCanonicalNameToLocalPath[showRepoResult.name] = Path(showRepoResult.path)
        else -> {
          bspClientLogger.warn("Tried to import external module $externalRepo, but it was not `local_path_override`: $showRepoResult")
        }
      }
    } catch (e: Exception) {
      bspClientLogger.error(e.toString())
    }
  }

  return BzlmodRepoMapping(moduleCanonicalNameToLocalPath, moduleApparentNameToCanonicalName)
}
