package org.jetbrains.bazel.server.sync

import com.intellij.aspect.lib.Aspects
import com.intellij.aspect.lib.OutputGroups
import com.intellij.aspect.lib.Rules
import com.intellij.build.events.MessageEvent
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.platform.diagnostic.telemetry.helpers.useWithScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.bazelrunner.ModuleResolver
import org.jetbrains.bazel.bazelrunner.ShowRepoResult
import org.jetbrains.bazel.commons.BazelInfo
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.BazelStatus
import org.jetbrains.bazel.commons.BzlmodRepoMapping
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.commons.RepoMappingDisabled
import org.jetbrains.bazel.commons.TargetCollection
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.label.Canonical
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.languages.projectview.ProjectView
import org.jetbrains.bazel.languages.projectview.gazelleTarget
import org.jetbrains.bazel.languages.projectview.shardSync
import org.jetbrains.bazel.languages.projectview.targetShardSize
import org.jetbrains.bazel.languages.projectview.targets
import org.jetbrains.bazel.languages.starlark.repomapping.externalRepositoriesTreatedAsInternal
import org.jetbrains.bazel.performance.bspTracer
import org.jetbrains.bazel.progress.syncConsole
import org.jetbrains.bazel.server.bsp.managers.BazelBspAspectsManager
import org.jetbrains.bazel.server.bsp.managers.BazelBspAspectsManagerResult
import org.jetbrains.bazel.server.bsp.managers.BazelExternalRulesetsQueryImpl
import org.jetbrains.bazel.server.bzlmod.calculateRepoNameMappingOnly
import org.jetbrains.bazel.server.bzlmod.extendRepoMappingByPathInfo
import org.jetbrains.bazel.server.model.AspectSyncProject
import org.jetbrains.bazel.server.sync.sharding.BazelBuildTargetSharder
import org.jetbrains.bsp.protocol.BazelTaskEventsHandler
import org.jetbrains.bsp.protocol.TaskId
import org.jetbrains.bsp.protocol.asLogger
import java.nio.file.Path
import kotlin.io.path.Path

internal class IllegalTargetsSizeException(message: String) : Exception(message)

private fun TargetCollection.halve(): List<TargetCollection> {
  if (values.size <= 1) {
    throw IllegalTargetsSizeException("Cannot split target collection with ${values.size} targets")
  }
  val mid = values.size / 2
  return listOf(
    TargetCollection(values.subList(0, mid), excludedValues),
    TargetCollection(values.subList(mid, values.size), excludedValues),
  )
}

/** Responsible for querying bazel and constructing Project instance  */
@ApiStatus.Internal
class ProjectResolver(
  private val bazelBspAspectsManager: BazelBspAspectsManager,
  private val projectView: ProjectView,
  private val bazelInfo: BazelInfo,
  private val bazelRunner: BazelRunner,
  private val bazelPathsResolver: BazelPathsResolver,
  private val taskEventsHandler: BazelTaskEventsHandler,
  private val project: Project,
) {
  private suspend fun <T> measured(description: String, f: suspend () -> T): T = bspTracer.spanBuilder(description).useWithScope { f() }

  internal suspend fun resolve(
    build: Boolean,
    requestedTargetsToSync: List<Label>?,
    allTargets: List<Label>?, /* all known targets, if any, from first phase */
    taskId: TaskId,
  ): AspectSyncProject {
    return bspTracer.spanBuilder("Resolve project").useWithScope {
      val buildAspectResult = buildProjectWithAspectAndSetup(build, requestedTargetsToSync, allTargets, taskId)
      val repoMapping = buildAspectResult.first
      val aspectResult = buildAspectResult.second

      val configurations = fetchConfigurationsFromAnalysisCache(projectView, bazelRunner, taskId)
        .onFailure { logger.warn("`bazel config` invocation failed, falling back to BEP configurations", it) }
        .getOrElse { aspectResult.bepOutput.configurations.values } // fallback to BEP configurations

      val aspectOutputs = extractAspectOutputPaths(aspectResult)
      val targets =
        measured(
          "Parsing aspect outputs",
        ) {
          TargetInfoReader(taskEventsHandler.asLogger(taskId))
            .readTargetMapFromAspectOutputs(aspectOutputs)
        }

      val newRepoMapping = when (repoMapping) {
        is RepoMappingDisabled -> RepoMappingDisabled
        is BzlmodRepoMapping -> {
          // If we discovered new repositories in the transitive dependencies, verify if some of
          // them are local repositories and update our mapping to local paths accordingly.
          // Additionally, for those newly discovered local repositories, update the path to
          // point to the source tree (rather than the output map).
          val involvedRepos = targets.keys.mapNotNull { (it.label as? ResolvedLabel)?.repo as? Canonical }.distinct()
          val needsPath = involvedRepos
            .filter { !(repoMapping.canonicalRepoNameToLocalPath.contains(it.repoName)) }
            .map { it.toString() }
          val extraRepositoryDescriptions =
            ModuleResolver(bazelRunner, projectView, taskId).resolveModules(needsPath, bazelInfo).result
          val extraPaths = extraRepositoryDescriptions.map { (name, description) ->
            when (description) {
              is ShowRepoResult.LocalRepository -> mapOf(description.name to Path(description.path))
              else -> mapOf()
            }
          }.reduceOrNull { acc, map -> acc + map }
            .orEmpty()
          val extraPathsResolved = extraPaths.mapValues { (_, path) -> bazelInfo.workspaceRoot.resolve(path) }
          BzlmodRepoMapping(
            repoMapping.canonicalRepoNameToLocalPath + extraPaths,
            repoMapping.apparentRepoNameToCanonicalName,
            repoMapping.canonicalRepoNameToPath + extraPathsResolved,
          )
        }
      }


      val workspaceName = targets.values.firstOrNull()?.workspaceName ?: "_main"
      val rootTargets = aspectResult.bepOutput.rootTargets()

      return@useWithScope AspectSyncProject(
        workspaceRoot = bazelInfo.workspaceRoot,
        bazelRelease = bazelInfo.release,
        repoMapping = newRepoMapping,
        workspaceName = workspaceName,
        hasError = aspectResult.isFailure,
        targets = targets,
        rootTargets = rootTargets,
        configurations = configurations.associateBy { it.id },
      )
    }
  }

  private suspend fun buildProjectWithAspectAndSetup(
    build: Boolean,
    requestedTargetsToSync: List<Label>?,
    allTargets: List<Label>?, /* all known targets, if any, from first phase */
    taskId: TaskId,
  ): Pair<RepoMapping, BazelBspAspectsManagerResult> {
    val repoMappingOnly =
      measured("Calculating external repository mapping") {
        calculateRepoNameMappingOnly(projectView, bazelRunner, bazelInfo, taskEventsHandler.asLogger(taskId), taskId)
      }

    val bazelExternalRulesetsQuery =
      BazelExternalRulesetsQueryImpl(
        taskId,
        bazelRunner,
        bazelInfo.isBzlModEnabled,
        bazelInfo.isWorkspaceEnabled,
        taskEventsHandler,
        projectView,
        repoMappingOnly,
      )

    val externalRulesetNames =
      measured(
        "Discovering supported external rules",
      ) { bazelExternalRulesetsQuery.fetchExternalRulesetNames() }

    val mapping = (repoMappingOnly as? BzlmodRepoMapping)?.apparentRepoNameToCanonicalName

    val externalRepos = mapping?.let { mapping ->
      externalRulesetNames.mapNotNull { repoName -> mapping[repoName] }.filter { it != "" }.map { "@@" + it }
    } ?: emptyList()

    val extraDefinitionsNeeded =
      projectView.externalRepositoriesTreatedAsInternal.map { repoName -> mapping?.get(repoName)?.let { "@@" + it } ?: repoName }

    val repoDefinitionsWithWarnings =
      measured("Looking up definitions of external rules") {
        ModuleResolver(bazelRunner, projectView, taskId).resolveModules(externalRepos + extraDefinitionsNeeded, bazelInfo)
      }

    repoDefinitionsWithWarnings.warnings.forEach {
      project.syncConsole.addDiagnosticMessage(
        taskId, null, 0, 0, it, null,
        MessageEvent.Kind.WARNING,
      )
    }

    val repoDefinitions = repoDefinitionsWithWarnings.result

    val repoMapping = extendRepoMappingByPathInfo(
      repoMappingOnly,
      projectView,
      bazelRunner,
      bazelInfo,
      taskEventsHandler.asLogger(taskId),
      repoDefinitions,
      taskId,
    )

    val ruleLanguages =
      measured(
        "Mapping rule names to languages",
      ) {
        bazelBspAspectsManager.calculateRulesetLanguages(
          externalRulesetNames,
          repoDefinitions,
          bazelInfo.externalAutoloads,
        )
      }

    ruleLanguages.forEach {
      it.language.deprecated?.let {
        project.syncConsole.addDiagnosticMessage(
          taskId, null, 0, 0, it, null,
          MessageEvent.Kind.WARNING,
        )
      }
    }

    measured("Realizing language aspect files from templates") {
      bazelBspAspectsManager.deployIntelliJAspect(
        ruleLanguages,
        bazelInfo.release,
        repoMapping,
      )
    }

    measured("Run Gazelle target") {
      projectView.gazelleTarget?.also { gazelleTarget ->
        runGazelleTarget(projectView, gazelleTarget, taskId)
      }
    }

    val targetsToSync =
      requestedTargetsToSync
        ?.let { TargetCollection(it, emptyList()) } ?: TargetCollection.fromExcludableList(projectView.targets)

    val syncLanguages = ruleLanguages.map { it.language.aspectLanguage }.toSet()

    val buildAspectResult =
      measured(
        "Building project with aspect",
      ) { buildProjectWithAspect(projectView, syncLanguages, build, targetsToSync, allTargets, taskId) }

    return Pair(repoMapping, buildAspectResult)
  }

  private suspend fun buildProjectWithAspect(
    projectView: ProjectView,
    languages: Set<Rules>,
    build: Boolean,
    targetsToSync: TargetCollection,
    allTargets: List<Label>?, /* all known targets, if any, from first phase */
    taskId: TaskId,
  ): BazelBspAspectsManagerResult =
    coroutineScope {
      val aspects = Aspects.forRules(languages).map { it.toString() }
      val taskLogger = taskEventsHandler.asLogger(taskId)
      val outputGroups = mutableListOf(OutputGroups.INFO.groupName, OutputGroups.SYNC.groupName)
      if (build) {
        outputGroups.add(OutputGroups.BUILD.groupName)
      }
      val nonShardBuild =
        suspend {
          bazelBspAspectsManager
            .fetchFilesFromOutputGroups(
              targetsSpec = targetsToSync,
              aspects = aspects,
              outputGroups = outputGroups,
              projectView = projectView,
              taskId = taskId,
            ).also {
              if (it.status == BazelStatus.OOM_ERROR) {
                taskLogger.warn("Bazel ran out of memory during sync. To mitigate, consider enabling shard sync in your project view file: `shard_sync: true`")
                taskLogger.message("---")
              }
            }
        }

      val res =
        if (projectView.shardSync) {
          val shardedResult =
            BazelBuildTargetSharder.expandAndShardTargets(
              bazelPathsResolver,
              targetsToSync,
              projectView,
              bazelRunner,
              taskLogger,
              allTargets,
            )
          var remainingShardedTargetsSpecs = shardedResult.targets.toTargetCollections().toMutableList()
          var shardNumber = 1
          var shardedBuildResult: BazelBspAspectsManagerResult = BazelBspAspectsManagerResult.emptyResult()
          var suggestedTargetShardSize: Int = projectView.targetShardSize
          while (remainingShardedTargetsSpecs.isNotEmpty()) {
            ensureActive()
            if (BazelFeatureFlags.shutDownBeforeShardBuild) {
              // Prevent memory leak by forcing Bazel to shut down before it builds a shard
              // This may cause the build to become slower, but it is necessary, at least before this issue is solved
              // https://github.com/bazelbuild/bazel/issues/19412
              runBazelShutDown(projectView, taskId)
            }
            val shardedTargetsSpec = remainingShardedTargetsSpecs.removeFirst()
            val shardName = "shard $shardNumber of ${shardNumber + remainingShardedTargetsSpecs.size}"
            taskLogger.message("\nBuilding $shardName ...")
            taskLogger.message("Expected remaining shards: ${remainingShardedTargetsSpecs.size}")
            val result =
              bazelBspAspectsManager
                .fetchFilesFromOutputGroups(
                  targetsSpec = shardedTargetsSpec,
                  aspects = aspects,
                  outputGroups = outputGroups,
                  projectView = projectView,
                  taskId = taskId,
                )
            if (result.isFailure) {
              taskLogger.warn("Failed to build $shardName")
            }
            else {
              taskLogger.message("Finished building $shardName")
            }
            if (result.status == BazelStatus.OOM_ERROR) {
              taskLogger.warn("Bazel ran out of memory during sync, attempting to halve the target shard size to recover")
              try {
                val halvedTargetsSpec = shardedTargetsSpec.halve()
                suggestedTargetShardSize = halvedTargetsSpec.first().values.size
                taskLogger.message(
                  "Retrying with the target shard size of $suggestedTargetShardSize (previously ${shardedTargetsSpec.values.size}) ...",
                )
                remainingShardedTargetsSpecs = remainingShardedTargetsSpecs.flatMap { it.halve() }.toMutableList()
                remainingShardedTargetsSpecs.addAll(0, halvedTargetsSpec)
              }
              catch (e: IllegalTargetsSizeException) {
                taskLogger.error("Cannot split targets further: ${e.message}")
                throw e
              }
            }
            // As bazel starts the naming of the namedSetOfFiles freshly (starting from "0") on each run, we have to distinguish
            // them somehow; we do this by encoding the shard number in their names.
            val runResult = result.renameNamedSets(shardNumber)
            shardedBuildResult = shardedBuildResult.merge(runResult)

            taskLogger.message("---")
            ++shardNumber
          }
          if (suggestedTargetShardSize != projectView.targetShardSize) {
            taskLogger.message("Bazel ran out of memory during sync. To mitigate, consider setting shard size in your project view file: `target_shard_size: $suggestedTargetShardSize`")
            taskLogger.message("---")
          }
          shardedBuildResult
        }
        else {
          nonShardBuild()
        }

      return@coroutineScope res
    }

  private suspend fun runBazelShutDown(projectView: ProjectView, taskId: TaskId) {
    bazelRunner.run {
      val command =
        buildBazelCommand(projectView) {
          shutDown()
        }
      runBazelCommand(command, taskId)
        .waitAndGetResult()
    }
  }

  private suspend fun runGazelleTarget(projectView: ProjectView, gazelleTarget: Label, taskId: TaskId) {
    bazelRunner.run {
      val command =
        buildBazelCommand(projectView) {
          run(gazelleTarget)
        }
      runBazelCommand(command, taskId)
        .waitAndGetResult()
    }
  }

  internal suspend fun extractAspectOutputPaths(buildAspectResult: BazelBspAspectsManagerResult): Set<Path> =
    measured(
      "Reading aspect output paths",
    ) { buildAspectResult.bepOutput.filesByOutputGroupNameTransitive(OutputGroups.INFO.groupName) }

  companion object {
    private val logger = logger<ProjectResolver>()
  }
}
