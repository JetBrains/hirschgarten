package org.jetbrains.bazel.server.bzlmod

import kotlinx.coroutines.CancellationException
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.bazelrunner.ModuleResolver
import org.jetbrains.bazel.bazelrunner.ShowRepoResult
import org.jetbrains.bazel.commons.BazelInfo
import org.jetbrains.bazel.commons.BzlmodRepoMapping
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.commons.RepoMappingDisabled
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bazel.workspacecontext.externalRepositoriesTreatedAsInternal
import org.jetbrains.bsp.protocol.BazelTaskLogger
import org.jetbrains.bsp.protocol.TaskId
import java.nio.file.Path
import kotlin.collections.plus
import kotlin.io.path.Path

internal val rootRulesToNeededTransitiveRules = mapOf(
  "rules_kotlin" to listOf("rules_java"),
  "rules_scala" to listOf("rules_java"),
)

internal suspend fun calculateRepoNameMappingOnly(
  workspaceContext: WorkspaceContext,
  bazelRunner: BazelRunner,
  bazelInfo: BazelInfo,
  taskLogger: BazelTaskLogger,
  taskId: TaskId,
): RepoMapping {
  if (!bazelInfo.isBzlModEnabled) {
    return RepoMappingDisabled
  }
  val moduleResolver = ModuleResolver(bazelRunner, workspaceContext, taskId)
  val moduleApparentNameToCanonicalName =
    try {
      // empty string is the name of the root module
      moduleResolver.getRepoMappings(listOf("")).get("").orEmpty()
    }
    catch (e: CancellationException) {
      throw e
    }
    catch (e: Exception) {
      taskLogger.error(e.toString())
      return RepoMappingDisabled
    }

  fun coveredByMap(
    repositoryNames: List<String>,
    mapping: Map<String, String>,
  ) = repositoryNames.all { mapping.containsKey(it) }

  val moduleApparentNameToCanonicalNameForNeededTransitiveRules =
    rootRulesToNeededTransitiveRules.filter {
      !coveredByMap(it.value, moduleApparentNameToCanonicalName)
    }
      .keys
      .mapNotNull { moduleApparentNameToCanonicalName[it] }
      .let {
        moduleResolver.getRepoMappings(it)
      }.values
      .reduceOrNull { acc, map -> acc + map }
      .orEmpty()

  return BzlmodRepoMapping(
      mapOf(),
      moduleApparentNameToCanonicalNameForNeededTransitiveRules + moduleApparentNameToCanonicalName,
      mapOf(),
    )
}

internal suspend fun extendRepoMappingByPathInfo(
  nameMapping : RepoMapping,
  workspaceContext: WorkspaceContext,
  bazelRunner: BazelRunner,
  bazelInfo: BazelInfo,
  taskLogger: BazelTaskLogger,
  knownResolved: Map<String, ShowRepoResult?>,
  taskId: TaskId,
): RepoMapping {
  val moduleApparentNameToCanonicalName = (nameMapping as? BzlmodRepoMapping)?.apparentRepoNameToCanonicalName ?: return RepoMappingDisabled
  val knownRepoDefinitions = knownResolved.values.filterNotNull().associateBy { it.name }

  val moduleCanonicalNameToLocalPath = mutableMapOf<String, Path>()
  val moduleResolver = ModuleResolver(bazelRunner, workspaceContext, taskId)

  val (known, unknown) =workspaceContext.externalRepositoriesTreatedAsInternal.partition { name ->
    moduleApparentNameToCanonicalName[name]?.let { knownRepoDefinitions.containsKey(it) } ?: false
  }
  val knownCanonicalNames = known.map { moduleApparentNameToCanonicalName[it] }
  val resolvedModules = moduleResolver.resolveModules(unknown, bazelInfo).result + knownRepoDefinitions.filter { (k,v) -> knownCanonicalNames.contains(k) }

  resolvedModules.forEach { externalRepo, showRepoResult ->
    try {
      when (showRepoResult) {
        is ShowRepoResult.LocalRepository -> moduleCanonicalNameToLocalPath[showRepoResult.name] = Path(showRepoResult.path)
        else -> {
          taskLogger.warn("Tried to import external module $externalRepo, but it was not `local_path_override`: $showRepoResult")
        }
      }
    }
    catch (e: Exception) {
      taskLogger.error(e.toString())
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
      }
      else {
        // See https://bazel.build/external/overview#directory-layout
        bazelInfo.outputBase.resolve("external").resolve(canonicalName)
      }
    moduleCanonicalNameToPath[canonicalName] = repoPath
  }

  return BzlmodRepoMapping(
    moduleCanonicalNameToLocalPath,
    moduleApparentNameToCanonicalName,
    moduleCanonicalNameToPath,
  )
}


internal suspend fun calculateRepoMapping(
  workspaceContext: WorkspaceContext,
  bazelRunner: BazelRunner,
  bazelInfo: BazelInfo,
  taskLogger: BazelTaskLogger,
  taskId: TaskId,
): RepoMapping {
  return extendRepoMappingByPathInfo(
    calculateRepoNameMappingOnly(workspaceContext, bazelRunner, bazelInfo, taskLogger, taskId),
    workspaceContext, bazelRunner, bazelInfo, taskLogger, mapOf(), taskId)
}
