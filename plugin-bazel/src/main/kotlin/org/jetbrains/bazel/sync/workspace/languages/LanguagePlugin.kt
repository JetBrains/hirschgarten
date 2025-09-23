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

  /**
   * Helper method for collecting resources with a conditional check.
   * Combines base resources from targetInfo with additional resources.
   *
   * @param targetInfo The target to collect resources from
   * @param pathsResolver Resolver for converting file locations to paths
   * @param hasTargetInfo Predicate to check if target has the appropriate target info
   * @return Sequence of resource paths
   */
  fun collectResourcesWithCheck(
    targetInfo: BspTargetInfo.TargetInfo,
    pathsResolver: BazelPathsResolver,
    hasTargetInfo: (BspTargetInfo.TargetInfo) -> Boolean
  ): Sequence<Path> {
    if (!hasTargetInfo(targetInfo)) return emptySequence()
    val base = targetInfo.resourcesList.asSequence().map(pathsResolver::resolve)
    return base + resolveAdditionalResources(targetInfo)
  }

  /**
   * Lifecycle hook executed before sync begins.
   * Use this to initialize plugin state, resolve defaults, or collect metadata.
   *
   * Example: Python plugin extracts default interpreter and version from targets.
   */
  fun prepareSync(targets: Sequence<BspTargetInfo.TargetInfo>, workspaceContext: WorkspaceContext) {}

  /**
   * Creates language-specific build target data.
   * Should return null if the target is not supported by this plugin.
   *
   * @return Language-specific build target data, or null if not applicable
   */
  suspend fun createBuildTargetData(context: LanguagePluginContext, target: BspTargetInfo.TargetInfo): BuildTarget?

  /**
   * Determines if this plugin can handle a target.
   * Used for plugin selection and target routing.
   *
   * Implementation note: Check target kind or presence of language-specific target info.
   */
  fun supportsTarget(target: BspTargetInfo.TargetInfo): Boolean = false

  /**
   * Determines if a target should be imported as a workspace target (module).
   * Workspace targets get full IDE support, while non-workspace targets are treated as libraries.
   *
   * Implementation note:
   * - Check if target is internal (using [org.jetbrains.bazel.sync.workspace.languages.isInternalTarget])
   * - Consider feature flags for language enablement
   * - Evaluate presence of sources, dependencies, or special kinds (test, binary, etc.)
   *
   * @return true if target should be imported as a module
   */
  fun isWorkspaceTarget(target: BspTargetInfo.TargetInfo, repoMapping: RepoMapping, featureFlags: FeatureFlags): Boolean = false

  /**
   * Indicates whether a target supports strict dependency checking.
   * Affects import depth calculation and dependency selection.
   *
   * Implementation note:
   * - Java: true (pure Java supports strict deps)
   * - Kotlin/Scala: false (mixed language targets don't use strict deps by default)
   * - Other languages: typically false
   */
  fun targetSupportsStrictDeps(target: BspTargetInfo.TargetInfo): Boolean = false

  /**
   * Indicates whether a target requires transitive dependency selection.
   * When true, all transitive dependencies are included regardless of import depth.
   *
   * Implementation note:
   * - Go: true (requires full transitive closure for proper resolution)
   * - JVM languages: false (use import depth mechanism)
   */
  fun requiresTransitiveSelection(target: BspTargetInfo.TargetInfo): Boolean = false

  /**
   * Collects per-target libraries that should be added to a target's dependencies.
   * Examples: annotation processors, compiler plugins, generated code libraries, output jars.
   *
   * Implementation note:
   * - Java: annotation processor libraries, output jars for srcjars/AP-generated code
   * - Kotlin: compiler plugin jars, stdlib references
   * - Scala: SDK and ScalaTest references
   * - Other languages: typically not needed
   *
   * @return Map from target label to list of libraries to add as dependencies
   */
  fun collectPerTargetLibraries(targets: Sequence<BspTargetInfo.TargetInfo>): Map<Label, List<Library>> = emptyMap()

  /**
   * Collects project-level libraries shared across multiple targets.
   * Examples: language standard libraries, testing frameworks, SDK bundles.
   *
   * Implementation note:
   * - Kotlin: stdlib bundle (shared across all Kotlin targets)
   * - Scala: SDK jars and ScalaTest (shared across all Scala targets)
   * - Java: typically not needed (dependencies explicit in BUILD files)
   *
   * @return Map from synthetic label to library definition
   */
  fun collectProjectLevelLibraries(targets: Sequence<BspTargetInfo.TargetInfo>): Map<Label, Library> = emptyMap()

  /**
   * Collects additional libraries from jdeps analysis for JVM languages.
   * Jdeps reveals actual runtime dependencies that may not be explicitly declared.
   *
   * Implementation note:
   * - Java: Implemented (parses jdeps proto files asynchronously)
   * - Kotlin/Scala: Can delegate to Java plugin or implement separately
   * - Non-JVM languages: Not applicable
   *
   * @param targetsToImport Targets being imported as modules
   * @param existingPerTargetLibs Already collected per-target libraries
   * @param allKnownLibraries All libraries from dependencies
   * @param interfacesAndBinaries Binary artifacts to exclude (already provided by targets)
   * @return Map from target label to additional jdeps-derived libraries
   */
  suspend fun collectJdepsLibraries(
    targetsToImport: Map<Label, BspTargetInfo.TargetInfo>,
    existingPerTargetLibs: Map<Label, List<Library>>,
    allKnownLibraries: Map<Label, Library>,
    interfacesAndBinaries: Map<Label, Set<Path>>,
  ): Map<Label, List<Library>> = emptyMap()

  /**
   * Creates library definitions for targets not being imported as modules.
   * These targets are treated as external dependencies.
   *
   * Implementation note:
   * - Java: Creates libraries with interface/binary/source jars
   * - Other JVM languages: Similar to Java
   * - Non-JVM languages: May not need this
   *
   * @return Map from label to library definition
   */
  fun collectLibrariesForNonImportedTargets(
    targets: Sequence<BspTargetInfo.TargetInfo>,
    repoMapping: RepoMapping,
  ): Map<Label, Library> = emptyMap()

  /**
   * Returns binary artifacts produced by a target (e.g., jars, ijars).
   * Used for deduplication in jdeps processing to avoid adding target's own outputs as dependencies.
   *
   * Implementation note:
   * - Java: interface jars and binary jars
   * - Other languages: Typically not implemented
   */
  fun provideBinaryArtifacts(target: BspTargetInfo.TargetInfo): Set<Path> = emptySet()

  /**
   * Allows plugins to provide runnable non-module targets directly.
   * Used for targets that should be runnable but not imported as full modules.
   *
   * Implementation note: Rarely needed, most languages don't implement this.
   */
  fun collectNonModuleTargets(
    targets: Map<Label, BspTargetInfo.TargetInfo>,
    repoMapping: RepoMapping,
    workspaceContext: WorkspaceContext,
    pathsResolver: BazelPathsResolver,
  ): List<RawBuildTarget> = emptyList()
}
