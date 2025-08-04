package org.jetbrains.bazel.server.sync

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.commons.BazelInfo
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.BazelStatus
import org.jetbrains.bazel.commons.canonicalize
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.logger.BspClientLogger
import org.jetbrains.bazel.performance.bspTracer
import org.jetbrains.bazel.performance.telemetry.useWithScope
import org.jetbrains.bazel.server.bsp.managers.BazelBspAspectsManager
import org.jetbrains.bazel.server.bsp.managers.BazelBspAspectsManagerResult
import org.jetbrains.bazel.server.bsp.managers.BazelBspLanguageExtensionsGenerator
import org.jetbrains.bazel.server.bsp.managers.BazelExternalRulesetsQueryImpl
import org.jetbrains.bazel.server.bsp.managers.BazelToolchainManager
import org.jetbrains.bazel.server.bzlmod.calculateRepoMapping
import org.jetbrains.bazel.server.model.AspectSyncProject
import org.jetbrains.bazel.server.model.PhasedSyncProject
import org.jetbrains.bazel.server.sync.sharding.BazelBuildTargetSharder
import org.jetbrains.bazel.workspacecontext.IllegalTargetsSizeException
import org.jetbrains.bazel.workspacecontext.TargetsSpec
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bazel.workspacecontext.provider.WorkspaceContextProvider
import org.jetbrains.bsp.protocol.FeatureFlags

/** Responsible for querying bazel and constructing Project instance  */
class ProjectResolver(
  private val bazelBspAspectsManager: BazelBspAspectsManager,
  private val bazelToolchainManager: BazelToolchainManager,
  private val bazelBspLanguageExtensionsGenerator: BazelBspLanguageExtensionsGenerator,
  private val workspaceContextProvider: WorkspaceContextProvider,
  private val targetInfoReader: TargetInfoReader,
  private val bazelInfo: BazelInfo,
  private val bazelRunner: BazelRunner,
  private val bazelPathsResolver: BazelPathsResolver,
  private val bspClientLogger: BspClientLogger,
) {
  private suspend fun <T> measured(description: String, f: suspend () -> T): T = bspTracer.spanBuilder(description).useWithScope { f() }

  suspend fun resolve(
    build: Boolean,
    requestedTargetsToSync: List<Label>?,
    phasedSyncProject: PhasedSyncProject?,
    originId: String?,
  ): AspectSyncProject =
    bspTracer.spanBuilder("Resolve project").useWithScope {
      val workspaceContext =
        measured(
          "Reading project view and creating workspace context",
          workspaceContextProvider::readWorkspaceContext,
        )
      val featureFlags = workspaceContextProvider.currentFeatureFlags()

      val repoMapping =
        measured("Calculating external repository mapping") {
          calculateRepoMapping(workspaceContext, bazelRunner, bazelInfo, bspClientLogger)
        }

      val bazelExternalRulesetsQuery =
        BazelExternalRulesetsQueryImpl(
          bazelRunner,
          bazelInfo.isBzlModEnabled,
          bazelInfo.isWorkspaceEnabled,
          bspClientLogger,
          workspaceContext,
          repoMapping,
        )

      val externalRulesetNames =
        measured(
          "Discovering supported external rules",
        ) { bazelExternalRulesetsQuery.fetchExternalRulesetNames() }

      val ruleLanguages =
        measured(
          "Mapping rule names to languages",
        ) {
          bazelBspAspectsManager.calculateRulesetLanguages(
            externalRulesetNames,
            bazelInfo.externalAutoloads,
            workspaceContext,
            featureFlags,
          )
        }

      val toolchains =
        measured(
          "Mapping languages to toolchains",
        ) { ruleLanguages.associateWith { bazelToolchainManager.getToolchain(it, workspaceContext, featureFlags) } }

      measured("Realizing language aspect files from templates") {
        bazelBspAspectsManager.generateAspectsFromTemplates(
          ruleLanguages,
          externalRulesetNames,
          workspaceContext,
          toolchains,
          bazelInfo.release,
          repoMapping,
          featureFlags,
        )
      }

      measured("Generating language extensions file") {
        bazelBspLanguageExtensionsGenerator.generateLanguageExtensions(ruleLanguages, toolchains)
      }

      measured("Run Gazelle target") {
        workspaceContext.gazelleTarget.value?.also { gazelleTarget ->
          runGazelleTarget(workspaceContext, gazelleTarget)
        }
      }

      val targetsToSync =
        requestedTargetsToSync
          ?.let { TargetsSpec(it, emptyList()) } ?: workspaceContext.targets

      val buildAspectResult =
        measured(
          "Building project with aspect",
        ) { buildProjectWithAspect(workspaceContext, featureFlags, build, targetsToSync, phasedSyncProject, originId) }

      val aspectOutputs =
        measured(
          "Reading aspect output paths",
        ) { buildAspectResult.bepOutput.filesByOutputGroupNameTransitive(BSP_INFO_OUTPUT_GROUP) }
      val targets =
        measured(
          "Parsing aspect outputs",
        ) {
          targetInfoReader
            .readTargetMapFromAspectOutputs(aspectOutputs)
            .map { (k, v) ->
              // TODO: make sure we canonicalize everything
              //  (https://youtrack.jetbrains.com/issue/BAZEL-1597/Make-sure-all-labels-in-the-server-are-canonicalized)
              //  also, this can be done in a more efficient way
              //  maybe we can do it in the aspect with some flag or something
              val label = k.canonicalize(repoMapping)
              label to
                v
                  .toBuilder()
                  .apply {
                    id = label.toString()
                    val canonicalizedDependencies =
                      dependenciesBuilderList.map {
                        it
                          .apply {
                            id = Label.parse(it.id).canonicalize(repoMapping).toString()
                          }.build()
                      }
                    clearDependencies()
                    addAllDependencies(canonicalizedDependencies)
                  }.build()
            }.toMap()
        }

      val workspaceName = targets.values.map { it.workspaceName }.firstOrNull() ?: "_main"
      val rootTargets = buildAspectResult.bepOutput.rootTargets()
      return@useWithScope AspectSyncProject(
        workspaceRoot = bazelInfo.workspaceRoot,
        bazelRelease = bazelInfo.release,
        repoMapping = repoMapping,
        workspaceContext = workspaceContext,
        workspaceName = workspaceName,
        hasError = buildAspectResult.isFailure,
        targets = targets,
        rootTargets = rootTargets,
      )
    }

  private suspend fun buildProjectWithAspect(
    workspaceContext: WorkspaceContext,
    featureFlags: FeatureFlags,
    build: Boolean,
    targetsToSync: TargetsSpec,
    phasedSyncProject: PhasedSyncProject?,
    originId: String?,
  ): BazelBspAspectsManagerResult =
    coroutineScope {
      val outputGroups = mutableListOf(BSP_INFO_OUTPUT_GROUP, SYNC_ARTIFACT_OUTPUT_GROUP)
      val languageSpecificOutputGroups = getLanguageSpecificOutputGroups(featureFlags)
      outputGroups.addAll(languageSpecificOutputGroups)
      if (build) {
        outputGroups.add(BUILD_ARTIFACT_OUTPUT_GROUP)
      }
      val nonShardBuild =
        suspend {
          bazelBspAspectsManager
            .fetchFilesFromOutputGroups(
              targetsSpec = targetsToSync,
              aspect = ASPECT_NAME,
              outputGroups = outputGroups,
              shouldLogInvocation = true,
              workspaceContext = workspaceContext,
              originId = originId,
            ).also {
              if (it.status == BazelStatus.OOM_ERROR) {
                bspClientLogger.warn(
                  "Bazel ran out of memory during sync. To mitigate, consider enabling shard sync in your project view file: `shard_sync: true`",
                )
                bspClientLogger.message("---")
              }
            }
        }

      val res =
        if (workspaceContext.shardSync.value) {
          val shardedResult =
            BazelBuildTargetSharder.expandAndShardTargets(
              bazelPathsResolver,
              bazelInfo,
              targetsToSync,
              workspaceContext,
              featureFlags,
              bazelRunner,
              bspClientLogger,
              phasedSyncProject,
            )
          var remainingShardedTargetsSpecs = shardedResult.targets.toTargetsSpecs().toMutableList()
          var shardNumber = 1
          var shardedBuildResult: BazelBspAspectsManagerResult = BazelBspAspectsManagerResult.emptyResult()
          var suggestedTargetShardSize: Int = workspaceContext.targetShardSize.value
          while (remainingShardedTargetsSpecs.isNotEmpty()) {
            ensureActive()
            if (featureFlags.bazelShutDownBeforeShardBuild) {
              // Prevent memory leak by forcing Bazel to shut down before it builds a shard
              // This may cause the build to become slower, but it is necessary, at least before this issue is solved
              // https://github.com/bazelbuild/bazel/issues/19412
              runBazelShutDown(workspaceContext)
            }
            val shardedTargetsSpec = remainingShardedTargetsSpecs.removeFirst()
            val shardName = "shard $shardNumber of ${shardNumber + remainingShardedTargetsSpecs.size}"
            bspClientLogger.message("\nBuilding $shardName ...")
            bspClientLogger.message("Expected remaining shards: ${remainingShardedTargetsSpecs.size}")
            val result =
              bazelBspAspectsManager
                .fetchFilesFromOutputGroups(
                  targetsSpec = shardedTargetsSpec,
                  aspect = ASPECT_NAME,
                  outputGroups = outputGroups,
                  shouldLogInvocation = false,
                  workspaceContext = workspaceContext,
                  originId = originId,
                )
            if (result.isFailure) {
              bspClientLogger.warn("Failed to build $shardName")
            } else {
              bspClientLogger.message("Finished building $shardName")
            }
            if (result.status == BazelStatus.OOM_ERROR) {
              bspClientLogger.warn("Bazel ran out of memory during sync, attempting to halve the target shard size to recover")
              try {
                val halvedTargetsSpec = shardedTargetsSpec.halve()
                suggestedTargetShardSize = halvedTargetsSpec.first().values.size
                bspClientLogger.message(
                  "Retrying with the target shard size of $suggestedTargetShardSize (previously ${shardedTargetsSpec.values.size}) ...",
                )
                remainingShardedTargetsSpecs = remainingShardedTargetsSpecs.flatMap { it.halve() }.toMutableList()
                remainingShardedTargetsSpecs.addAll(0, halvedTargetsSpec)
              } catch (e: IllegalTargetsSizeException) {
                bspClientLogger.error("Cannot split targets further: ${e.message}")
                throw e
              }
            }
            shardedBuildResult = shardedBuildResult.merge(result)

            bspClientLogger.message("---")
            ++shardNumber
          }
          if (suggestedTargetShardSize != workspaceContext.targetShardSize.value) {
            bspClientLogger.message(
              "Bazel ran out of memory during sync. To mitigate, consider setting shard size in your project view file: `target_shard_size: $suggestedTargetShardSize`",
            )
            bspClientLogger.message("---")
          }
          shardedBuildResult
        } else {
          nonShardBuild()
        }

      return@coroutineScope res
    }

  private fun getLanguageSpecificOutputGroups(featureFlags: FeatureFlags): List<String> =
    if (featureFlags.isGoSupportEnabled) {
      listOf(GO_SOURCE_OUTPUT_GROUP)
    } else {
      emptyList()
    }

  private suspend fun runBazelShutDown(workspaceContext: WorkspaceContext) {
    bazelRunner.run {
      val command =
        buildBazelCommand(workspaceContext) {
          shutDown()
        }
      runBazelCommand(command, serverPidFuture = null)
        .waitAndGetResult()
    }
  }

  private suspend fun runGazelleTarget(workspaceContext: WorkspaceContext, gazelleTarget: Label) {
    bazelRunner.run {
      val command =
        buildBazelCommand(workspaceContext) {
          run(gazelleTarget)
        }
      runBazelCommand(command, serverPidFuture = null)
        .waitAndGetResult()
    }
  }

  fun releaseMemory() {
    bazelPathsResolver.clear()
    System.gc()
  }

  companion object {
    private const val ASPECT_NAME = "bsp_target_info_aspect"
    private const val BSP_INFO_OUTPUT_GROUP = "bsp-target-info"

    // this output group is for artifacts which are needed during no-build sync
    private const val SYNC_ARTIFACT_OUTPUT_GROUP = "bsp-sync-artifact"

    // this output group is for artifacts which are only needed during build
    private const val BUILD_ARTIFACT_OUTPUT_GROUP = "bsp-build-artifact"

    // language-specific output groups
    private const val GO_SOURCE_OUTPUT_GROUP = "bazel-sources-go"
  }
}
