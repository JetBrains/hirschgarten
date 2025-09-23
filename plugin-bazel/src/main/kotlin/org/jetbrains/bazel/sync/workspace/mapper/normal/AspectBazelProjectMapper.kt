package org.jetbrains.bazel.sync.workspace.mapper.normal

import com.intellij.openapi.diagnostic.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.BzlmodRepoMapping
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.commons.RepoMappingDisabled
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.Tag
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.info.BspTargetInfo.TargetInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.label.assumeResolved
import org.jetbrains.bazel.performance.bspTracer
import org.jetbrains.bazel.performance.telemetry.useWithScope
import org.jetbrains.bazel.server.label.label
import org.jetbrains.bazel.sync.workspace.BazelResolvedWorkspace
import org.jetbrains.bazel.sync.workspace.BuildTargetCollection
import org.jetbrains.bazel.sync.workspace.graph.DependencyGraph
import org.jetbrains.bazel.sync.workspace.languages.LanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.LanguagePluginContext
import org.jetbrains.bazel.sync.workspace.languages.LanguagePluginsService
import org.jetbrains.bazel.sync.workspace.languages.jvm.JVMPackagePrefixResolver
import org.jetbrains.bazel.sync.workspace.model.BspMappings
import org.jetbrains.bazel.sync.workspace.model.Library
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.BuildTargetTag
import org.jetbrains.bsp.protocol.FeatureFlags
import org.jetbrains.bsp.protocol.LibraryItem
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.jetbrains.bsp.protocol.SourceItem
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.notExists

class AspectBazelProjectMapper(
  private val languagePluginsService: LanguagePluginsService,
  private val featureFlags: FeatureFlags,
  private val bazelPathsResolver: BazelPathsResolver,
  private val targetTagsResolver: TargetTagsResolver,
) {
  val logger = logger<AspectBazelProjectMapper>()

  private suspend fun <T> measure(description: String, body: suspend () -> T): T =
    bspTracer.spanBuilder(description).useWithScope { body() }

  data class IntermediateTargetData(
    val label: ResolvedLabel,
    val target: TargetInfo,
    val extraLibraries: Collection<Library>,
    val tags: Set<Tag>,
    val sources: List<SourceItem>,
    val languages: Set<LanguageClass>,
  )

  fun TargetInfo.toIntermediateData(
    workspaceContext: WorkspaceContext,
    extraLibraries: Map<Label, List<Library>>,
    inferLanguagesFn: (TargetInfo) -> Set<LanguageClass> = ::inferLanguages,
  ): IntermediateTargetData? {
    val languages = inferLanguagesFn(this)
    val languagePlugin = languagePluginsService.getLanguagePlugin(languages) ?: return null
    val tags = targetTagsResolver.resolveTags(this, workspaceContext).toSet()
    val label = this.label().assumeResolved()
    return IntermediateTargetData(
      label = label,
      target = this,
      extraLibraries = extraLibraries[label] ?: emptyList(),
      tags = tags,
      sources = resolveSourceSet(this, languagePlugin),
      languages = languages,
    )
  }

  suspend fun createProject(
    targets: Map<Label, TargetInfo>,
    rootTargets: Set<Label>,
    workspaceContext: WorkspaceContext,
    repoMapping: RepoMapping,
    hasError: Boolean,
  ): BazelResolvedWorkspace {
    languagePluginsService.all.forEach { it.prepareSync(targets.values.asSequence(), workspaceContext) }
    val dependencyGraph = measure("Build dependency tree") {
      DependencyGraph(rootTargets, targets)
    }
    // Cache inferLanguages results for performance since it's called multiple times per target
    val languagesCache = mutableMapOf<TargetInfo, Set<LanguageClass>>()
    val cachedInferLanguages: (TargetInfo) -> Set<LanguageClass> = { target ->
      languagesCache.getOrPut(target) { inferLanguages(target) }
    }
    val selectionResult = measure("Select targets") {
      val strategy = TargetSelectionStrategy(languagePluginsService, repoMapping, featureFlags)
      strategy.selectTargets(
        dependencyGraph,
        rootTargets,
        targets,
        workspaceContext,
        inferLanguages = cachedInferLanguages,
        isTargetTreatedAsInternal = { label -> isTargetTreatedAsInternal(label, repoMapping) },
      )
    }
    val targetsToImport = selectionResult.targetsToImport
    val targetsAsLibraries = selectionResult.targetsAsLibraries
    val interfacesAndBinariesFromTargetsToImport = measure("Collect binary artifacts from plugins for targets to import") {
      targetsToImport.associate { t ->
        val artifacts = languagePluginsService.all.flatMap { it.provideBinaryArtifacts(t) }.toSet()
        t.label() to artifacts
      }
    }

    // New: collect libraries from language plugins (per-target and project-level)
    val pluginPerTargetLibraries = measure("Collect per-target libraries from plugins") {
      languagePluginsService.all.map { it.collectPerTargetLibraries(targetsToImport.asSequence()) }
        .fold(emptyMap<Label, List<Library>>()) { acc, next -> concatenateMaps(acc, next) }
    }
    val pluginProjectLevelLibraries = measure("Collect project-level libraries from plugins") {
      languagePluginsService.all.map { it.collectProjectLevelLibraries(targetsToImport.asSequence()) }
        .fold(emptyMap<Label, Library>()) { acc, next -> acc + next }
    }

    val librariesFromDeps = measure("Merge libraries from deps") {
      pluginPerTargetLibraries
    }
    val librariesFromDepsAndTargets = measure("Libraries from targets and deps") {
      val libsFromNonImportedTargets =
        languagePluginsService.all.map { it.collectLibrariesForNonImportedTargets(targetsAsLibraries.values.asSequence(), repoMapping) }
          .fold(emptyMap<Label, Library>()) { acc, next -> acc + next }
      libsFromNonImportedTargets + (pluginProjectLevelLibraries.values + librariesFromDeps.values.flatten()
        .distinct()).associateBy { it.label }
    } // New: allow plugins to contribute jdeps-derived libraries (JVM-specific), merged with existing jdeps logic
    val pluginJdepsLibraries = measure("Libraries from plugins' jdeps") {
      languagePluginsService.all.map {
        it.collectJdepsLibraries(
          targetsToImport.associateBy { it.label() },
          librariesFromDeps,
          librariesFromDepsAndTargets,
          interfacesAndBinariesFromTargetsToImport,
        )
      }.fold(emptyMap<Label, List<Library>>()) { acc, next -> concatenateMaps(acc, next) }
    }
    val extraLibrariesFromJdeps = measure("Libraries from jdeps") {
      pluginJdepsLibraries
    }
    val librariesToImport = measure("Merge all libraries") {
      librariesFromDepsAndTargets + extraLibrariesFromJdeps.values.flatten().associateBy { it.label }
    }

    val importedIds = targetsToImport.map { it.label() }.toSet()
    val nonModuleTargetIds = (removeDotBazelBspTarget(targets.keys) - librariesToImport.keys - importedIds).toSet()
    val nonModuleCandidates = targets.filterKeys {
      nonModuleTargetIds.contains(it) && isTargetTreatedAsInternal(it.assumeResolved(), repoMapping)
    }
    val nonModuleRawTargets = measure("collect non-module targets from plugins") {
      languagePluginsService.all.flatMap { plugin ->
        plugin.collectNonModuleTargets(nonModuleCandidates, repoMapping, workspaceContext, bazelPathsResolver)
      }
    }

    val extraLibraries = concatenateMaps(librariesFromDeps, extraLibrariesFromJdeps)

    val selectedTargets = measure("create intermediate targets") {
      targetsToImport.mapNotNull { it.toIntermediateData(workspaceContext, extraLibraries, cachedInferLanguages) }.toList()
    }

    val highPrioritySources = if (this.featureFlags.isSharedSourceSupportEnabled) {
      emptySet()
    } else {
      selectedTargets.filter { !it.tags.hasLowSharedSourcesPriority() }.flatMap { it.sources }.map { it.path }.toSet()
    }

    val rawTargets = measure("create raw targets") { createRawBuildTargets(selectedTargets, highPrioritySources, repoMapping, dependencyGraph) }

    return BazelResolvedWorkspace(
      targets = BuildTargetCollection().apply {
        addBuildTargets(rawTargets)
        addNonModuleTargets(nonModuleRawTargets)
      },
      libraries = librariesToImport.values.map {
        LibraryItem(
          id = it.label,
          dependencies = it.dependencies,
          ijars = it.interfaceJars.toList(),
          jars = it.outputs.toList(),
          sourceJars = it.sources.toList(),
          mavenCoordinates = it.mavenCoordinates,
          isFromInternalTarget = it.isFromInternalTarget,
          isLowPriority = it.isLowPriority,
        )
      },
      hasError = hasError,
    )
  }

  private fun <K, V> concatenateMaps(vararg maps: Map<K, List<V>>): Map<K, List<V>> =
    maps.flatMap { it.keys }.distinct().associateWith { key ->
      maps.flatMap { it[key].orEmpty() }
    }

  private fun externalRepositoriesTreatedAsInternal(repoMapping: RepoMapping) = when (repoMapping) {
    is BzlmodRepoMapping -> repoMapping.canonicalRepoNameToLocalPath.keys
    is RepoMappingDisabled -> emptySet()
  }

  private fun isTargetTreatedAsInternal(target: ResolvedLabel, repoMapping: RepoMapping): Boolean =
    target.isMainWorkspace || target.repo.repoName in externalRepositoriesTreatedAsInternal(repoMapping) || target.isGazelleGenerated


  private suspend fun createRawBuildTargets(
    targets: List<IntermediateTargetData>,
    highPrioritySources: Set<Path>,
    repoMapping: RepoMapping,
    dependencyGraph: DependencyGraph,
  ): List<RawBuildTarget> = withContext(Dispatchers.Default) {
    val tasks = targets.map { target -> // async {
      createRawBuildTarget(
        target,
        highPrioritySources,
        repoMapping,
        dependencyGraph,
      ) // }
    }

    return@withContext tasks // .awaitAll()
      .filterNotNull().filterNot { BuildTargetTag.NO_IDE in it.tags }
  }

  private suspend fun createRawBuildTarget(
    targetData: IntermediateTargetData,
    highPrioritySources: Set<Path>,
    repoMapping: RepoMapping,
    dependencyGraph: DependencyGraph,
  ): RawBuildTarget? {
    val target = targetData.target
    val label = targetData.label
    val resolvedDependencies =
      resolveDirectDependencies(target) // https://youtrack.jetbrains.com/issue/BAZEL-983: extra libraries can override some library versions, so they should be put before
    val (extraLibraries, lowPriorityExtraLibraries) = targetData.extraLibraries.partition { !it.isLowPriority }
    val directDependencies = extraLibraries.map { it.label } + resolvedDependencies + lowPriorityExtraLibraries.map { it.label }
    val baseDirectory = bazelPathsResolver.toDirectoryPath(label, repoMapping)
    val languagePlugin = languagePluginsService.getLanguagePlugin(targetData.languages) ?: return null
    val resources = resolveResources(target, languagePlugin)

    val tags = targetData.tags
    val (targetSources, lowPrioritySharedSources) = if (tags.hasLowSharedSourcesPriority()) {
      targetData.sources.partition { it.path !in highPrioritySources }
    } else {
      targetData.sources to emptyList()
    }

    val context = LanguagePluginContext(target, dependencyGraph, repoMapping, bazelPathsResolver)
    val data = languagePlugin.createBuildTargetData(context, target)

    return RawBuildTarget(
      id = label,
      tags = tags.mapNotNull(BspMappings::toBspTag),
      dependencies = directDependencies,
      kind = inferKind(tags, target.kind, targetData.languages),
      sources = targetSources,
      resources = resources,
      baseDirectory = baseDirectory,
      noBuild = Tag.NO_BUILD in tags,
      data = data,
      lowPrioritySharedSources = lowPrioritySharedSources,
    )
  }

  private fun resolveDirectDependencies(target: TargetInfo): List<Label> = target.dependenciesList.map { it.label() }


  private fun inferLanguages(target: TargetInfo): Set<LanguageClass> =
    languagePluginsService.all.filter { it.supportsTarget(target) }.flatMap { it.getSupportedLanguages() }.toSet()

  private fun resolveSourceSet(target: TargetInfo, languagePlugin: LanguagePlugin<*>): List<SourceItem> {
    val sources = target.sourcesList.asSequence().map(bazelPathsResolver::resolve)

    val generatedSources = target.generatedSourcesList.asSequence().map(bazelPathsResolver::resolve).filter { it.extension != "srcjar" }

    val extraSources = languagePlugin.calculateAdditionalSources(target)

    return (sources + extraSources + generatedSources).distinct().onEach { if (it.notExists()) logNonExistingFile(it, target.id) }.map {
      SourceItem(
        path = it,
        generated = false,
        jvmPackagePrefix = (languagePlugin as? JVMPackagePrefixResolver)?.resolveJvmPackagePrefix(it),
      )
    }.toList()
  }

  private fun logNonExistingFile(file: Path, targetId: String) {
    val message = "target $targetId: $file does not exist."
    logger.warn(message)
  }

  private fun resolveResources(target: TargetInfo, languagePlugin: LanguagePlugin<*>): List<Path> =
    languagePlugin.collectResources(target).distinct().toList()

  private fun removeDotBazelBspTarget(targets: Collection<Label>): Collection<Label> = targets.filter {
    it.isMainWorkspace && !it.packagePath.toString().startsWith(".bazelbsp")
  }
  
  private fun Set<Tag>.hasLowSharedSourcesPriority(): Boolean = Tag.IDE_LOW_SHARED_SOURCES_PRIORITY in this

  private fun inferKind(
    tags: Set<Tag>,
    kindString: String,
    languages: Set<LanguageClass>,
  ): TargetKind {
    val ruleType = when {
      tags.contains(Tag.TEST) -> RuleType.TEST
      tags.contains(Tag.APPLICATION) -> RuleType.BINARY
      tags.contains(Tag.LIBRARY) -> RuleType.LIBRARY
      else -> RuleType.UNKNOWN
    }
    return TargetKind(
      kindString = kindString,
      languageClasses = languages,
      ruleType = ruleType,
    )
  }
}
