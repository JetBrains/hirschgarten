package org.jetbrains.bsp.bazel.server.sync

import org.eclipse.lsp4j.jsonrpc.CancelChecker
import org.jetbrains.bazel.commons.label.Label
import org.jetbrains.bazel.commons.label.assumeResolved
import org.jetbrains.bsp.bazel.bazelrunner.BazelRunner
import org.jetbrains.bsp.bazel.bazelrunner.utils.BazelInfo
import org.jetbrains.bsp.bazel.commons.BazelStatus
import org.jetbrains.bsp.bazel.logger.BspClientLogger
import org.jetbrains.bsp.bazel.server.benchmark.tracer
import org.jetbrains.bsp.bazel.server.benchmark.useWithScope
import org.jetbrains.bsp.bazel.server.bsp.managers.BazelBspAspectsManager
import org.jetbrains.bsp.bazel.server.bsp.managers.BazelBspAspectsManagerResult
import org.jetbrains.bsp.bazel.server.bsp.managers.BazelBspLanguageExtensionsGenerator
import org.jetbrains.bsp.bazel.server.bsp.managers.BazelExternalRulesQueryImpl
import org.jetbrains.bsp.bazel.server.bsp.managers.BazelLabelExpander
import org.jetbrains.bsp.bazel.server.bsp.managers.BazelToolchainManager
import org.jetbrains.bsp.bazel.server.bzlmod.calculateRepoMapping
import org.jetbrains.bsp.bazel.server.bzlmod.canonicalize
import org.jetbrains.bsp.bazel.server.model.AspectSyncProject
import org.jetbrains.bsp.bazel.server.model.FirstPhaseProject
import org.jetbrains.bsp.bazel.server.paths.BazelPathsResolver
import org.jetbrains.bsp.bazel.server.sync.sharding.BazelBuildTargetSharder
import org.jetbrains.bsp.bazel.workspacecontext.IllegalTargetsSizeException
import org.jetbrains.bsp.bazel.workspacecontext.TargetsSpec
import org.jetbrains.bsp.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.bazel.workspacecontext.WorkspaceContextProvider
import org.jetbrains.bsp.protocol.FeatureFlags

/** Responsible for querying bazel and constructing Project instance  */
class ProjectResolver(
  private val bazelBspAspectsManager: BazelBspAspectsManager,
  private val bazelToolchainManager: BazelToolchainManager,
  private val bazelBspLanguageExtensionsGenerator: BazelBspLanguageExtensionsGenerator,
  private val bazelLabelExpander: BazelLabelExpander,
  private val workspaceContextProvider: WorkspaceContextProvider,
  private val bazelProjectMapper: BazelProjectMapper,
  private val targetInfoReader: TargetInfoReader,
  private val bazelInfo: BazelInfo,
  private val bazelRunner: BazelRunner,
  private val bazelPathsResolver: BazelPathsResolver,
  private val bspClientLogger: BspClientLogger,
  private val featureFlags: FeatureFlags,
) {
  private suspend fun <T> measured(description: String, f: suspend () -> T): T = tracer.spanBuilder(description).useWithScope { f() }

  suspend fun resolve(
    cancelChecker: CancelChecker,
    build: Boolean,
    requestedTargetsToSync: List<Label>?,
    firstPhaseProject: FirstPhaseProject?,
  ): AspectSyncProject =
    tracer.spanBuilder("Resolve project").useWithScope {
      val workspaceContext =
        measured(
          "Reading project view and creating workspace context",
          workspaceContextProvider::currentWorkspaceContext,
        )

      val bazelExternalRulesQuery =
        BazelExternalRulesQueryImpl(
          bazelRunner,
          bazelInfo.isBzlModEnabled,
          bazelInfo.isWorkspaceEnabled,
          workspaceContext.enabledRules,
          bspClientLogger,
        )

      val externalRuleNames =
        measured(
          "Discovering supported external rules",
        ) { bazelExternalRulesQuery.fetchExternalRuleNames(cancelChecker) }

      val ruleLanguages =
        measured(
          "Mapping rule names to languages",
        ) { bazelBspAspectsManager.calculateRuleLanguages(externalRuleNames) }

      val toolchains =
        measured(
          "Mapping languages to toolchains",
        ) { ruleLanguages.associateWith { bazelToolchainManager.getToolchain(it, cancelChecker) } }

      val repoMapping =
        measured("Calculating external repository mapping") {
          calculateRepoMapping(workspaceContext, bazelRunner, bazelInfo, bspClientLogger)
        }

      measured("Realizing language aspect files from templates") {
        bazelBspAspectsManager.generateAspectsFromTemplates(ruleLanguages, workspaceContext, toolchains, bazelInfo.release, repoMapping)
      }

      measured("Generating language extensions file") {
        bazelBspLanguageExtensionsGenerator.generateLanguageExtensions(ruleLanguages, toolchains)
      }

      val targetsToSync =
        requestedTargetsToSync
          ?.let { TargetsSpec(it, emptyList()) } ?: workspaceContext.targets

      val buildAspectResult =
        measured(
          "Building project with aspect",
        ) { buildProjectWithAspect(cancelChecker, workspaceContext, build, targetsToSync, featureFlags, firstPhaseProject) }

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
              //  (https://youtrack.jetbrains.com/issue/BAZEL-1595/Merge-WildcardTargetExpander-and-BazelLabelExpander)
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
      // resolve root targets (expand wildcards)
      val rootTargets =
        measured("Calculating root targets") {
          bazelLabelExpander.getAllPossibleTargets(cancelChecker).map { it.assumeResolved().canonicalize(repoMapping) }.toSet()
        }
      return@useWithScope measured(
        "Mapping to internal model",
      ) { bazelProjectMapper.createProject(targets, rootTargets, workspaceContext, bazelInfo, repoMapping) }
    }

  private suspend fun buildProjectWithAspect(
    cancelChecker: CancelChecker,
    workspaceContext: WorkspaceContext,
    build: Boolean,
    targetsToSync: TargetsSpec,
    featureFlags: FeatureFlags,
    firstPhaseProject: FirstPhaseProject?,
  ): BazelBspAspectsManagerResult {
    val outputGroups = mutableListOf(BSP_INFO_OUTPUT_GROUP, SYNC_ARTIFACT_OUTPUT_GROUP)
    if (build) {
      outputGroups.add(BUILD_ARTIFACT_OUTPUT_GROUP)
    }
    val nonShardBuild =
      suspend {
        bazelBspAspectsManager
          .fetchFilesFromOutputGroups(
            cancelChecker = cancelChecker,
            targetsSpec = targetsToSync,
            aspect = ASPECT_NAME,
            outputGroups = outputGroups,
            shouldSyncManualFlags = workspaceContext.allowManualTargetsSync.value,
            isRustEnabled = featureFlags.isRustSupportEnabled,
            shouldLogInvocation = false,
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
            featureFlags,
            targetsToSync,
            workspaceContext,
            bazelRunner,
            cancelChecker,
            bspClientLogger,
            firstPhaseProject,
          )
        var remainingShardedTargetsSpecs = shardedResult.targets.toTargetsSpecs().toMutableList()
        var shardNumber = 1
        var shardedBuildResult: BazelBspAspectsManagerResult? = null
        var suggestedTargetShardSize = workspaceContext.targetShardSize.value
        while (remainingShardedTargetsSpecs.isNotEmpty()) {
          val shardedTargetsSpec = remainingShardedTargetsSpecs.removeFirst()
          val shardName = "shard $shardNumber of ${shardNumber + remainingShardedTargetsSpecs.size}"
          bspClientLogger.message("\nBuilding $shardName ...")
          bspClientLogger.message("Expected remaining shards: ${remainingShardedTargetsSpecs.size}")
          val result =
            bazelBspAspectsManager
              .fetchFilesFromOutputGroups(
                cancelChecker = cancelChecker,
                targetsSpec = shardedTargetsSpec,
                aspect = ASPECT_NAME,
                outputGroups = outputGroups,
                shouldSyncManualFlags = workspaceContext.allowManualTargetsSync.value,
                isRustEnabled = featureFlags.isRustSupportEnabled,
                shouldLogInvocation = false,
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
              remainingShardedTargetsSpecs = remainingShardedTargetsSpecs.flatMap { it.halve() }.toMutableList()
              remainingShardedTargetsSpecs.addAll(0, halvedTargetsSpec)
            } catch (e: IllegalTargetsSizeException) {
              bspClientLogger.error("Cannot split targets further: ${e.message}")
              throw e
            }
          }
          shardedBuildResult = shardedBuildResult?.merge(result) ?: result

          bspClientLogger.message("---")
          ++shardNumber
        }
        if (suggestedTargetShardSize != workspaceContext.targetShardSize.value) {
          bspClientLogger.message(
            "Bazel ran out of memory during sync. To mitigate, consider setting shard size in your project view file: `target_shard_size: $suggestedTargetShardSize`",
          )
          bspClientLogger.message("---")
        }
        shardedBuildResult!!
      } else {
        nonShardBuild()
      }

    return res
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
  }
}
