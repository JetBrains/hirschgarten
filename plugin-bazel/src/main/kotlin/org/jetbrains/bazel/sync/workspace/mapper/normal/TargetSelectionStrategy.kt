package org.jetbrains.bazel.sync.workspace.mapper.normal

import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.info.BspTargetInfo.TargetInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.assumeResolved
import org.jetbrains.bazel.server.label.label
import org.jetbrains.bazel.sync.workspace.graph.DependencyGraph
import org.jetbrains.bazel.sync.workspace.languages.LanguagePluginsService
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.FeatureFlags

/**
 * Strategy for selecting which targets to import as modules vs. treat as libraries.
 *
 * Handles:
 * - Partitioning targets between transitive and depth-based selection
 * - Determining workspace targets using language plugins
 * - Computing import depth for normal targets
 * - Filtering non-workspace targets as libraries
 */
class TargetSelectionStrategy(
  private val languagePluginsService: LanguagePluginsService,
  private val repoMapping: RepoMapping,
  private val featureFlags: FeatureFlags,
) {

  data class SelectionResult(
    val targetsToImport: List<TargetInfo>,
    val targetsAsLibraries: Map<Label, TargetInfo>,
  )

  /**
   * Selects targets to import based on root targets, workspace context, and language plugin requirements.
   *
   * @param dependencyGraph Dependency graph containing all targets
   * @param rootTargets Root targets to start selection from
   * @param targets All available targets
   * @param workspaceContext Workspace configuration
   * @param inferLanguages Function to infer languages for a target
   * @param isTargetTreatedAsInternal Function to check if target is internal
   * @return Selection result with targets to import and targets to treat as libraries
   */
  fun selectTargets(
    dependencyGraph: DependencyGraph,
    rootTargets: Set<Label>,
    targets: Map<Label, TargetInfo>,
    workspaceContext: WorkspaceContext,
    inferLanguages: (TargetInfo) -> Set<org.jetbrains.bazel.commons.LanguageClass>,
    isTargetTreatedAsInternal: (org.jetbrains.bazel.label.ResolvedLabel) -> Boolean,
  ): SelectionResult {
    // Partition roots: some languages (Go) require full transitive selection
    val (transitiveRoots, normalRoots) = partitionRootsBySelectionMode(rootTargets, targets, inferLanguages)

    val pluginIsWorkspaceTarget: (TargetInfo) -> Boolean = { target ->
      languagePluginsService.all.any { it.isWorkspaceTarget(target, repoMapping, featureFlags) }
    }

    // Process normal roots with import depth
    val normalAtDepth = computeNormalTargetsAtDepth(
      dependencyGraph,
      normalRoots,
      targets,
      workspaceContext.importDepth,
      inferLanguages,
      isTargetTreatedAsInternal,
      pluginIsWorkspaceTarget,
    )

    val (normalTargetsToImport, nonWorkspaceTargets) = normalAtDepth.targets.partition { targetInfo ->
      pluginIsWorkspaceTarget(targetInfo)
    }

    // Process transitive roots (Go, etc.)
    val transitiveTargetsToImport =
      computeTransitiveTargets(dependencyGraph, transitiveRoots, pluginIsWorkspaceTarget)

    val libraries = (nonWorkspaceTargets + normalAtDepth.directDependencies).associateBy { it.label() }

    return SelectionResult(
      targetsToImport = normalTargetsToImport + transitiveTargetsToImport,
      targetsAsLibraries = libraries,
    )
  }

  private fun partitionRootsBySelectionMode(
    rootTargets: Set<Label>,
    targets: Map<Label, TargetInfo>,
    inferLanguages: (TargetInfo) -> Set<org.jetbrains.bazel.commons.LanguageClass>,
  ): Pair<List<Label>, List<Label>> =
    rootTargets.partition { label ->
      targets[label]?.let { t ->
        val langs = inferLanguages(t)
        val plugin = languagePluginsService.getLanguagePlugin(langs)
        plugin?.requiresTransitiveSelection(t) == true
      } == true
    }

  private fun computeNormalTargetsAtDepth(
    dependencyGraph: DependencyGraph,
    normalRoots: List<Label>,
    targets: Map<Label, TargetInfo>,
    importDepth: Int,
    inferLanguages: (TargetInfo) -> Set<org.jetbrains.bazel.commons.LanguageClass>,
    isTargetTreatedAsInternal: (org.jetbrains.bazel.label.ResolvedLabel) -> Boolean,
    pluginIsWorkspaceTarget: (TargetInfo) -> Boolean,
  ): DependencyGraph.TargetsAtDepth =
    dependencyGraph.allTargetsAtDepth(
      importDepth,
      normalRoots.toSet(),
      isExternalTarget = { !isTargetTreatedAsInternal(it.assumeResolved()) },
      targetSupportsStrictDeps = { id ->
        targets[id]?.let { t ->
          val langs = inferLanguages(t)
          val plugin = languagePluginsService.getLanguagePlugin(langs)
          plugin?.targetSupportsStrictDeps(t) == true
        } == true
      },
      isWorkspaceTarget = { id ->
        targets[id]?.let { target -> pluginIsWorkspaceTarget(target) } == true
      },
    )

  private fun computeTransitiveTargets(
    dependencyGraph: DependencyGraph,
    transitiveRoots: List<Label>,
    pluginIsWorkspaceTarget: (TargetInfo) -> Boolean,
  ): List<TargetInfo> =
    dependencyGraph.allTransitiveTargets(transitiveRoots.toSet()).targets.filter { pluginIsWorkspaceTarget(it) }
}