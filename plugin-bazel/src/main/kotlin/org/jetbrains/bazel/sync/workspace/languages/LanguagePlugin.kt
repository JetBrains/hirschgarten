package org.jetbrains.bazel.sync.workspace.languages

import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.model.Library
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.FeatureFlags
import org.jetbrains.bsp.protocol.RawBuildTarget
import java.nio.file.Path

interface LanguagePlugin<BuildTarget : BuildTargetData> {
  fun getSupportedLanguages(): Set<LanguageClass>

  // Additional sources and resources (extension points used by the core)
  fun calculateAdditionalSources(targetInfo: BspTargetInfo.TargetInfo): Sequence<Path> = emptySequence()

  fun resolveAdditionalResources(targetInfo: BspTargetInfo.TargetInfo): Sequence<Path> = emptySequence()

  // New: Allow plugins to fully provide resources (defaults to just additional resources)
  fun collectResources(targetInfo: BspTargetInfo.TargetInfo): Sequence<Path> = resolveAdditionalResources(targetInfo)

  // Lifecycle hook executed before sync begins
  fun prepareSync(targets: Sequence<BspTargetInfo.TargetInfo>, workspaceContext: WorkspaceContext) {}

  // Target data produced by the language plugin
  suspend fun createBuildTargetData(context: LanguagePluginContext, target: BspTargetInfo.TargetInfo): BuildTarget?

  // New: Selection & ownership hooks
  fun supportsTarget(target: BspTargetInfo.TargetInfo): Boolean = false

  // New: Per-language workspace selection decision (core falls back to legacy behavior if none claim)
  fun isWorkspaceTarget(target: BspTargetInfo.TargetInfo, repoMapping: RepoMapping, featureFlags: FeatureFlags): Boolean = false

  // New: Strict-deps decision (e.g., true for plain Java)
  fun targetSupportsStrictDeps(target: BspTargetInfo.TargetInfo): Boolean = false

  // New: Import-depth/selection policy override (e.g., Go wants full transitive)
  fun requiresTransitiveSelection(target: BspTargetInfo.TargetInfo): Boolean = false

  // New: Library aggregation hooks
  // Per-target extra libraries (e.g., APs, kotlinc plugins, output jars libraries, etc.)
  fun collectPerTargetLibraries(targets: Sequence<BspTargetInfo.TargetInfo>): Map<Label, List<Library>> = emptyMap()

  // Project-level libraries (e.g., Kotlin stdlib bundle, Scala SDK/ScalaTest)
  fun collectProjectLevelLibraries(targets: Sequence<BspTargetInfo.TargetInfo>): Map<Label, Library> = emptyMap()

  // New: jdeps-based enrichment for JVM languages
  fun collectJdepsLibraries(
    targetsToImport: Map<Label, BspTargetInfo.TargetInfo>,
    existingPerTargetLibs: Map<Label, List<Library>>,
    allKnownLibraries: Map<Label, Library>,
    interfacesAndBinaries: Map<Label, Set<Path>>,
  ): Map<Label, List<Library>> = emptyMap()

  // New: libraries for non-imported targets (e.g., binary/interface/source jars of targets treated as libraries)
  fun collectLibrariesForNonImportedTargets(
    targets: Sequence<BspTargetInfo.TargetInfo>,
    repoMapping: RepoMapping,
  ): Map<Label, Library> = emptyMap()

  // New: binary artifacts provided by a target (e.g., ijars + jars), used for deduping in jdeps
  fun provideBinaryArtifacts(target: BspTargetInfo.TargetInfo): Set<Path> = emptySet()

  // New: allow plugins to provide non-module runnable targets directly
  fun collectNonModuleTargets(
    targets: Map<Label, BspTargetInfo.TargetInfo>,
    repoMapping: RepoMapping,
    workspaceContext: WorkspaceContext,
    pathsResolver: BazelPathsResolver,
  ): List<RawBuildTarget> = emptyList()
}
