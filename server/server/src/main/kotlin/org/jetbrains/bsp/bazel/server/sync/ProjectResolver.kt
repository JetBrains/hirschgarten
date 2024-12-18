package org.jetbrains.bsp.bazel.server.sync

import org.eclipse.lsp4j.jsonrpc.CancelChecker
import org.jetbrains.bsp.bazel.bazelrunner.BazelRunner
import org.jetbrains.bsp.bazel.bazelrunner.utils.BazelInfo
import org.jetbrains.bsp.bazel.logger.BspClientLogger
import org.jetbrains.bsp.bazel.server.benchmark.tracer
import org.jetbrains.bsp.bazel.server.benchmark.useWithScope
import org.jetbrains.bsp.bazel.server.bsp.managers.BazelBspAspectsManager
import org.jetbrains.bsp.bazel.server.bsp.managers.BazelBspAspectsManagerResult
import org.jetbrains.bsp.bazel.server.bsp.managers.BazelBspLanguageExtensionsGenerator
import org.jetbrains.bsp.bazel.server.bsp.managers.BazelExternalRulesQueryImpl
import org.jetbrains.bsp.bazel.server.bsp.managers.BazelLabelExpander
import org.jetbrains.bsp.bazel.server.bsp.managers.BazelToolchainManager
import org.jetbrains.bsp.bazel.server.bzlmod.RepoMapping
import org.jetbrains.bsp.bazel.server.bzlmod.canonicalize
import org.jetbrains.bsp.bazel.server.model.Label
import org.jetbrains.bsp.bazel.server.model.Project
import org.jetbrains.bsp.bazel.server.model.label
import org.jetbrains.bsp.bazel.server.paths.BazelPathsResolver
import org.jetbrains.bsp.bazel.server.sync.sharding.BazelBuildTargetSharder
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
  private val repoMapping: RepoMapping,
) {
  private suspend fun <T> measured(description: String, f: suspend () -> T): T = tracer.spanBuilder(description).useWithScope { f() }

  suspend fun resolve(
    cancelChecker: CancelChecker,
    build: Boolean,
    requestedTargetsToSync: List<Label>?,
  ): Project =
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

      measured("Realizing language aspect files from templates") {
        bazelBspAspectsManager.generateAspectsFromTemplates(ruleLanguages, workspaceContext, toolchains, bazelInfo.release)
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
        ) { buildProjectWithAspect(cancelChecker, workspaceContext, build, targetsToSync, featureFlags) }

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
          bazelLabelExpander.getAllPossibleTargets(cancelChecker).map { it.canonicalize(repoMapping) }.toSet()
        }
      return@useWithScope measured(
        "Mapping to internal model",
      ) { bazelProjectMapper.createProject(targets, rootTargets, workspaceContext, bazelInfo) }
    }

  private suspend fun buildProjectWithAspect(
    cancelChecker: CancelChecker,
    workspaceContext: WorkspaceContext,
    build: Boolean,
    targetsToSync: TargetsSpec,
    featureFlags: FeatureFlags,
  ): BazelBspAspectsManagerResult {
    val outputGroups = mutableListOf(BSP_INFO_OUTPUT_GROUP, ARTIFACTS_OUTPUT_GROUP, RUST_ANALYZER_OUTPUT_GROUP)
    if (build) {
      outputGroups.add(GENERATED_JARS_OUTPUT_GROUP)
    }

    val nonShardBuild =
      suspend {
        bazelBspAspectsManager.fetchFilesFromOutputGroups(
          cancelChecker = cancelChecker,
          targetsSpec = targetsToSync,
          aspect = ASPECT_NAME,
          outputGroups = outputGroups,
          shouldSyncManualFlags = workspaceContext.allowManualTargetsSync.value,
          isRustEnabled = featureFlags.isRustSupportEnabled,
          shouldLogInvocation = false,
          bspClientLogger = bspClientLogger,
        )
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
          )
        val shardedTargetsSpecs = shardedResult.targets.toTargetsSpecs()
        val shardedSize = shardedTargetsSpecs.size

        if (shardedSize <= 1) {
          // fall back to non-sharded build when sharding does not have effects
          nonShardBuild()
        } else {
          shardedTargetsSpecs
            .mapIndexed { idx, shardedTargetsSpec ->
              val shardName = "shard ${idx + 1} of $shardedSize"
              bspClientLogger.message("\nBuilding $shardName ...")
              bazelBspAspectsManager
                .fetchFilesFromOutputGroups(
                  cancelChecker = cancelChecker,
                  targetsSpec = shardedTargetsSpec,
                  aspect = ASPECT_NAME,
                  outputGroups = outputGroups,
                  shouldSyncManualFlags = workspaceContext.allowManualTargetsSync.value,
                  isRustEnabled = featureFlags.isRustSupportEnabled,
                  shouldLogInvocation = false,
                  bspClientLogger = bspClientLogger,
                ).also {
                  if (it.isFailure) {
                    bspClientLogger.message("Failed to build $shardName")
                  } else {
                    bspClientLogger.message("Finished building $shardName")
                  }
                  bspClientLogger.message("---")
                }
            }.reduce { acc, result -> acc.merge(result) }
        }
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
    private const val ARTIFACTS_OUTPUT_GROUP = "external-deps-resolve"
    private const val GENERATED_JARS_OUTPUT_GROUP = "generated-jars-resolve"
    private const val RUST_ANALYZER_OUTPUT_GROUP = "rust_analyzer_crate_spec"
  }
}
