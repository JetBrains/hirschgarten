package org.jetbrains.bsp.bazel.server.bzlmod

import org.jetbrains.bazel.commons.label.Apparent
import org.jetbrains.bazel.commons.label.Canonical
import org.jetbrains.bazel.commons.label.Label
import org.jetbrains.bazel.commons.label.Main
import org.jetbrains.bazel.commons.label.RelativeLabel
import org.jetbrains.bazel.commons.label.ResolvedLabel
import org.jetbrains.bazel.commons.label.SyntheticLabel
import org.jetbrains.bsp.bazel.bazelrunner.BazelRunner
import org.jetbrains.bsp.bazel.bazelrunner.ModuleOutputParser
import org.jetbrains.bsp.bazel.bazelrunner.ModuleResolver
import org.jetbrains.bsp.bazel.bazelrunner.ShowRepoResult
import org.jetbrains.bsp.bazel.bazelrunner.utils.BazelInfo
import org.jetbrains.bsp.bazel.logger.BspClientLogger
import org.jetbrains.bsp.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.bazel.workspacecontext.externalRepositoriesTreatedAsInternal
import java.nio.file.Path
import kotlin.io.path.Path

sealed interface RepoMapping

data class BzlmodRepoMapping(
  val canonicalRepoNameToLocalPath: Map<String, Path>,
  val apparentRepoNameToCanonicalName: Map<String, String>,
  val canonicalRepoNameToPath: Map<String, Path>,
) : RepoMapping

data object RepoMappingDisabled : RepoMapping

fun Label.canonicalize(repoMapping: RepoMapping): Label =
  when (repoMapping) {
    is RepoMappingDisabled -> this
    is BzlmodRepoMapping -> {
      when (this) {
        is ResolvedLabel -> {
          when (this.repo) {
            is Main -> this
            is Canonical -> this
            is Apparent -> {
              val apparentRepoName = this.repoName
              val canonicalRepoName =
                repoMapping.apparentRepoNameToCanonicalName[apparentRepoName] ?: error("No canonical name found for $this")
              this.copy(repo = Canonical(canonicalRepoName))
            }
          }
        }

        is RelativeLabel -> error("Relative label $this cannot be canonicalized")
        is SyntheticLabel -> this
      }
    }
  }

fun calculateRepoMapping(
  workspaceContext: WorkspaceContext,
  bazelRunner: BazelRunner,
  bazelInfo: BazelInfo,
  bspClientLogger: BspClientLogger,
): RepoMapping {
  if (!bazelInfo.isBzlModEnabled) {
    return RepoMappingDisabled
  }
  val moduleResolver = ModuleResolver(bazelRunner, ModuleOutputParser())
  val moduleCanonicalNameToLocalPath = mutableMapOf<String, Path>()
  val moduleApparentNameToCanonicalName =
    try {
      // empty string is the name of the root module
      moduleResolver.getRepoMapping("") { }
    } catch (e: Exception) {
      bspClientLogger.error(e.toString())
      return RepoMappingDisabled
    }

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

  val moduleCanonicalNameToPath = moduleCanonicalNameToLocalPath.toMutableMap()
  for (canonicalName in moduleApparentNameToCanonicalName.values) {
    if (canonicalName == "") {
      moduleCanonicalNameToPath[canonicalName] = bazelInfo.workspaceRoot
      continue
    }
    val localPath = moduleCanonicalNameToLocalPath[canonicalName]
    val repoPath =
      if (localPath != null) {
        bazelInfo.workspaceRoot.resolve(localPath)
      } else {
        // See https://bazel.build/external/overview#directory-layout
        bazelInfo.outputBase.resolve("external").resolve(canonicalName)
      }
    moduleCanonicalNameToPath[canonicalName] = repoPath
  }

  return BzlmodRepoMapping(moduleCanonicalNameToLocalPath, moduleApparentNameToCanonicalName, moduleCanonicalNameToPath)
}
