package org.jetbrains.bazel.server.bzlmod

import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.bazelrunner.ModuleOutputParser
import org.jetbrains.bazel.bazelrunner.ModuleResolver
import org.jetbrains.bazel.bazelrunner.ShowRepoResult
import org.jetbrains.bazel.commons.BazelInfo
import org.jetbrains.bazel.commons.BzlmodRepoMapping
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.commons.RepoMappingDisabled
import org.jetbrains.bazel.logger.BspClientLogger
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bazel.workspacecontext.externalRepositoriesTreatedAsInternal
import java.nio.file.Path
import kotlin.io.path.Path

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

  val apparentRepoNameToCanonicalName =
    BidirectionalMap
      .getTypedInstance<String, String>()
      .apply { putAll(moduleApparentNameToCanonicalNameForNeededTransitiveRules + moduleApparentNameToCanonicalName) }

  return BzlmodRepoMapping(
    moduleCanonicalNameToLocalPath,
    apparentRepoNameToCanonicalName,
    moduleCanonicalNameToPath,
  )
}
