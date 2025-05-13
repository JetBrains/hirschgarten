package org.jetbrains.bazel.server.bzlmod

import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.bazelrunner.ModuleOutputParser
import org.jetbrains.bazel.bazelrunner.ModuleResolver
import org.jetbrains.bazel.bazelrunner.ShowRepoResult
import org.jetbrains.bazel.bazelrunner.utils.BazelInfo
import org.jetbrains.bazel.label.ApparentLabel
import org.jetbrains.bazel.label.Canonical
import org.jetbrains.bazel.label.CanonicalLabel
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.SyntheticLabel
import org.jetbrains.bazel.logger.BspClientLogger
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bazel.workspacecontext.externalRepositoriesTreatedAsInternal
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
        is ApparentLabel -> {
              val apparentRepoName = this.repoName
              val canonicalRepoName =
                repoMapping.apparentRepoNameToCanonicalName[apparentRepoName] ?: error("No canonical name found for $this")
              CanonicalLabel.fromParts(canonicalRepoName, this.packagePath, this.target)
            }
        is CanonicalLabel -> this
        is SyntheticLabel -> this
      }
    }
  }

val rootRulesToNeededTransitiveRules = mapOf("rules_kotlin" to listOf("rules_java"))

suspend fun calculateRepoMapping(
  workspaceContext: WorkspaceContext,
  bazelRunner: BazelRunner,
  bazelInfo: BazelInfo,
  bspClientLogger: BspClientLogger,
): RepoMapping {
  if (!bazelInfo.isBzlModEnabled) {
    return RepoMappingDisabled
  }
  val moduleResolver = ModuleResolver(bazelRunner, ModuleOutputParser(), workspaceContext)
  val moduleCanonicalNameToLocalPath = mutableMapOf<String, Path>()
  val moduleApparentNameToCanonicalName =
    try {
      // empty string is the name of the root module
      moduleResolver.getRepoMapping("")
    } catch (e: Exception) {
      bspClientLogger.error(e.toString())
      return RepoMappingDisabled
    }

  val moduleApparentNameToCanonicalNameForNeededTransitiveRules =
    rootRulesToNeededTransitiveRules.keys
      .mapNotNull { moduleApparentNameToCanonicalName[it] }
      .map { moduleResolver.getRepoMapping(it) }
      .reduceOrNull { acc, map -> acc + map }
      .orEmpty()

  for (externalRepo in workspaceContext.externalRepositoriesTreatedAsInternal) {
    try {
      val showRepoResult = moduleResolver.resolveModule(externalRepo)
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

  return BzlmodRepoMapping(
    moduleCanonicalNameToLocalPath,
    moduleApparentNameToCanonicalNameForNeededTransitiveRules + moduleApparentNameToCanonicalName,
    moduleCanonicalNameToPath,
  )
}
