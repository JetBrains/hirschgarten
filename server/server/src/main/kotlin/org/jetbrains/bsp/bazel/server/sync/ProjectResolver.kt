package org.jetbrains.bsp.bazel.server.sync

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import org.eclipse.lsp4j.jsonrpc.CancelChecker
import org.jetbrains.bsp.bazel.bazelrunner.BazelRunner
import org.jetbrains.bsp.bazel.bazelrunner.utils.BazelInfo
import org.jetbrains.bsp.bazel.logger.BspClientLogger
import org.jetbrains.bsp.bazel.server.benchmark.tracer
import org.jetbrains.bsp.bazel.server.benchmark.use
import org.jetbrains.bsp.bazel.server.bsp.managers.BazelBspAspectsManager
import org.jetbrains.bsp.bazel.server.bsp.managers.BazelBspAspectsManagerResult
import org.jetbrains.bsp.bazel.server.bsp.managers.BazelBspFallbackAspectsManager
import org.jetbrains.bsp.bazel.server.bsp.managers.BazelBspLanguageExtensionsGenerator
import org.jetbrains.bsp.bazel.server.bsp.managers.BazelExternalRulesQueryImpl
import org.jetbrains.bsp.bazel.server.bsp.managers.BazelToolchainManager
import org.jetbrains.bsp.bazel.server.model.Project
import org.jetbrains.bsp.bazel.server.paths.BazelPathsResolver
import org.jetbrains.bsp.bazel.workspacecontext.TargetsSpec
import org.jetbrains.bsp.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.bazel.workspacecontext.WorkspaceContextProvider
import org.jetbrains.bsp.bazel.workspacecontext.isRustEnabled

/** Responsible for querying bazel and constructing Project instance  */
class ProjectResolver(
  private val bazelBspAspectsManager: BazelBspAspectsManager,
  private val bazelToolchainManager: BazelToolchainManager,
  private val bazelBspLanguageExtensionsGenerator: BazelBspLanguageExtensionsGenerator,
  private val bazelBspFallbackAspectsManager: BazelBspFallbackAspectsManager,
  private val workspaceContextProvider: WorkspaceContextProvider,
  private val bazelProjectMapper: BazelProjectMapper,
  private val targetInfoReader: TargetInfoReader,
  private val bazelInfo: BazelInfo,
  private val bazelRunner: BazelRunner,
  private val bazelPathsResolver: BazelPathsResolver,
  private val bspClientLogger: BspClientLogger,
) {
  private fun <T> measured(description: String, f: () -> T): T = tracer.spanBuilder(description).use { f() }

  fun resolve(
    cancelChecker: CancelChecker,
    build: Boolean,
    requestedTargetsToSync: List<BuildTargetIdentifier>?,
  ): Project =
    tracer.spanBuilder("Resolve project").use {
      val workspaceContext =
        measured(
          "Reading project view and creating workspace context",
          workspaceContextProvider::currentWorkspaceContext,
        )

      val bazelExternalRulesQuery =
        BazelExternalRulesQueryImpl(bazelRunner, bazelInfo.isBzlModEnabled, workspaceContext.enabledRules, bspClientLogger)

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
        bazelBspAspectsManager.generateAspectsFromTemplates(ruleLanguages, workspaceContext, toolchains)
      }

      measured("Generating language extensions file") {
        bazelBspLanguageExtensionsGenerator.generateLanguageExtensions(ruleLanguages, toolchains)
      }

      val targetsToSync = requestedTargetsToSync?.let { TargetsSpec(it, emptyList()) } ?: workspaceContext.targets

      val buildAspectResult =
        measured(
          "Building project with aspect",
        ) { buildProjectWithAspect(cancelChecker, workspaceContext, build, targetsToSync) }
      val aspectOutputs =
        measured(
          "Reading aspect output paths",
        ) { buildAspectResult.bepOutput.filesByOutputGroupNameTransitive(BSP_INFO_OUTPUT_GROUP) }
      val targets =
        measured(
          "Parsing aspect outputs",
        ) { targetInfoReader.readTargetMapFromAspectOutputs(aspectOutputs) }
      val allTargetNames =
        if (buildAspectResult.isFailure) {
          measured(
            "Fetching all possible target names",
          ) { bazelBspFallbackAspectsManager.getAllPossibleTargets(cancelChecker) }
        } else {
          emptyList()
        }
      val rootTargets = buildAspectResult.bepOutput.rootTargets()
      return measured(
        "Mapping to internal model",
      ) { bazelProjectMapper.createProject(targets, rootTargets.toSet(), allTargetNames, workspaceContext, bazelInfo) }
    }

  private fun buildProjectWithAspect(
    cancelChecker: CancelChecker,
    workspaceContext: WorkspaceContext,
    build: Boolean,
    targetsToSync: TargetsSpec,
  ): BazelBspAspectsManagerResult {
    val outputGroups = mutableListOf(BSP_INFO_OUTPUT_GROUP, ARTIFACTS_OUTPUT_GROUP, RUST_ANALYZER_OUTPUT_GROUP)
    if (build) {
      outputGroups.add(GENERATED_JARS_OUTPUT_GROUP)
    }

    return bazelBspAspectsManager.fetchFilesFromOutputGroups(
      cancelChecker = cancelChecker,
      targetSpecs = targetsToSync,
      aspect = ASPECT_NAME,
      outputGroups = outputGroups.map { if (build) "+$it" else it },
      shouldSyncManualFlags = workspaceContext.allowManualTargetsSync.value,
      isRustEnabled = workspaceContext.isRustEnabled,
    )
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
