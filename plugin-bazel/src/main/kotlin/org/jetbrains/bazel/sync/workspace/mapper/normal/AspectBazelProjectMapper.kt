package org.jetbrains.bazel.sync.workspace.mapper.normal

import com.intellij.openapi.diagnostic.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.BzlmodRepoMapping
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.commons.RepoMappingDisabled
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.Tag
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.info.BspTargetInfo.FileLocation
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
import org.jetbrains.bazel.sync.workspace.model.NonModuleTarget
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
  private val mavenCoordinatesResolver: MavenCoordinatesResolver,
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
  ): IntermediateTargetData? {
    val languages = inferLanguages(this)
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
    featureFlags: FeatureFlags,
    repoMapping: RepoMapping,
    hasError: Boolean,
  ): BazelResolvedWorkspace {
    languagePluginsService.all.forEach { it.prepareSync(targets.values.asSequence(), workspaceContext) }
    val dependencyGraph =
      measure("Build dependency tree") {
        DependencyGraph(rootTargets, targets)
      }
    val (targetsToImport, targetsAsLibraries) =
      measure("Select targets") {
        // the import depth mechanism does not apply for certain languages (e.g., Go)
        // plugins can require transitive selection for their roots
        val (transitiveRoots, normalRoots) = rootTargets.partition { label ->
          targets[label]?.let { t ->
            val langs = inferLanguages(t)
            val plugin = languagePluginsService.getLanguagePlugin(langs)
            plugin?.requiresTransitiveSelection(t) == true
          } == true
        }
        val normalAtDepth =
          dependencyGraph
            .allTargetsAtDepth(
              workspaceContext.importDepth,
              normalRoots.toSet(),
              isExternalTarget = { !isTargetTreatedAsInternal(it.assumeResolved(), repoMapping) },
              targetSupportsStrictDeps = { id ->
                targets[id]?.let { t ->
                  val langs = inferLanguages(t)
                  val plugin = languagePluginsService.getLanguagePlugin(langs)
                  plugin?.targetSupportsStrictDeps(t) == true
                } == true
              },
              isWorkspaceTarget = { id ->
                targets[id]?.let { target ->
                  target.sourcesCount > 0 && isWorkspaceTarget(target, repoMapping, featureFlags)
                } == true
              },
            )
        val (normalTargetsToImport, nonWorkspaceTargets) =
          normalAtDepth.targets.partition {
            isWorkspaceTarget(it, repoMapping, featureFlags)
          }
        val transitiveTargetsToImport =
          dependencyGraph
            .allTransitiveTargets(transitiveRoots.toSet())
            .targets
            .filter { isWorkspaceTarget(it, repoMapping, featureFlags) }
        val libraries = (nonWorkspaceTargets + normalAtDepth.directDependencies).associateBy { it.label() }
        (normalTargetsToImport + transitiveTargetsToImport).asSequence() to libraries
      }
    val interfacesAndBinariesFromTargetsToImport =
      measure("Collect interfaces and classes from targets to import") {
        collectInterfacesAndClasses(targetsToImport)
      }

    // New: collect libraries from language plugins (per-target and project-level)
    val pluginPerTargetLibraries =
      measure("Collect per-target libraries from plugins") {
        languagePluginsService.all
          .map { it.collectPerTargetLibraries(targetsToImport) }
          .fold(emptyMap<Label, List<Library>>()) { acc, next -> concatenateMaps(acc, next) }
      }
    val pluginProjectLevelLibraries =
      measure("Collect project-level libraries from plugins") {
        languagePluginsService.all
          .map { it.collectProjectLevelLibraries(targetsToImport) }
          .fold(emptyMap<Label, Library>()) { acc, next -> acc + next }
      }

    val librariesFromDeps =
      measure("Merge libraries from deps") {
        pluginPerTargetLibraries
      }
    val librariesFromDepsAndTargets =
      measure("Libraries from targets and deps") {
        createLibraries(targetsAsLibraries, repoMapping) +
          (
            pluginProjectLevelLibraries.values +
              librariesFromDeps.values
                .flatten()
                .distinct()
          ).associateBy { it.label }
      }
    // New: allow plugins to contribute jdeps-derived libraries (JVM-specific), merged with existing jdeps logic
    val pluginJdepsLibraries =
      measure("Libraries from plugins' jdeps") {
        languagePluginsService.all
          .map {
            it.collectJdepsLibraries(
              targetsToImport.associateBy { it.label() },
              librariesFromDeps,
              librariesFromDepsAndTargets,
              interfacesAndBinariesFromTargetsToImport,
            )
          }
          .fold(emptyMap<Label, List<Library>>()) { acc, next -> concatenateMaps(acc, next) }
      }
    val extraLibrariesFromJdeps =
      measure("Libraries from jdeps") {
        pluginJdepsLibraries
      }
    val librariesToImport =
      measure("Merge all libraries") {
        librariesFromDepsAndTargets +
          extraLibrariesFromJdeps.values.flatten().associateBy { it.label }
      }

    val nonModuleTargetIds =
      (removeDotBazelBspTarget(targets.keys) - librariesToImport.keys).toSet()
    val nonModuleTargets =
      createNonModuleTargets(
        targets.filterKeys {
          nonModuleTargetIds.contains(it) &&
            isTargetTreatedAsInternal(it.assumeResolved(), repoMapping)
        },
        repoMapping,
        workspaceContext,
      )

    val extraLibraries = concatenateMaps(librariesFromDeps, extraLibrariesFromJdeps)

    val targets =
      measure("create intermediate targets") {
        targets.values.mapNotNull { it.toIntermediateData(workspaceContext, extraLibraries) }.toList()
      }

    val highPrioritySources =
      if (this.featureFlags.isSharedSourceSupportEnabled) {
        emptySet()
      } else {
        targets
          .filter { !it.tags.hasLowSharedSourcesPriority() }
          .flatMap { it.sources }
          .map { it.path }
          .toSet()
      }

    val rawTargets = measure("create raw targets") { createRawBuildTargets(targets, highPrioritySources, repoMapping, dependencyGraph) }
    val nonModuleRawTargets =
      measure("create non module raw targets") {
        nonModuleTargets
          .map { it.toBuildTarget() }
          .filter { it.kind.isExecutable }
      }

    return BazelResolvedWorkspace(
      targets =
        BuildTargetCollection().apply {
          addBuildTargets(rawTargets)
          addNonModuleTargets(nonModuleRawTargets)
        },
      libraries =
        librariesToImport.values.map {
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
    maps
      .flatMap { it.keys }
      .distinct()
      .associateWith { key ->
        maps.flatMap { it[key].orEmpty() }
      }



















  private fun createNonModuleTargets(
    targets: Map<Label, TargetInfo>,
    repoMapping: RepoMapping,
    workspaceContext: WorkspaceContext,
  ): List<NonModuleTarget> =
    targets
      .map { (label, targetInfo) ->
        NonModuleTarget(
          label = label,
          tags = targetTagsResolver.resolveTags(targetInfo, workspaceContext),
          baseDirectory = bazelPathsResolver.toDirectoryPath(label.assumeResolved(), repoMapping),
          kindString = targetInfo.kind,
        )
      }

  private suspend fun createLibraries(targets: Map<Label, TargetInfo>, repoMapping: RepoMapping): Map<Label, Library> =
    withContext(Dispatchers.Default) {
      targets
        .map { (targetId, targetInfo) ->
          async {
            createLibrary(
              label = targetId,
              targetInfo = targetInfo,
              onlyOutputJars = false,
              isInternalTarget = isTargetTreatedAsInternal(targetId.assumeResolved(), repoMapping),
            )?.let { library ->
              targetId to library
            }
          }
        }.awaitAll()
        .filterNotNull()
        .toMap()
    }

  private fun createLibrary(
    label: Label,
    targetInfo: TargetInfo,
    onlyOutputJars: Boolean,
    isInternalTarget: Boolean,
  ): Library? {
    val outputs = getTargetOutputJarPaths(targetInfo) + getIntellijPluginJars(targetInfo)
    val sources = getSourceJarPaths(targetInfo)
    val interfaceJars = getTargetInterfaceJarsSet(targetInfo).toSet()
    val dependencies: List<BspTargetInfo.Dependency> = if (!onlyOutputJars) targetInfo.dependenciesList else emptyList()
    if (!shouldCreateLibrary(
        dependencies = dependencies,
        outputs = outputs,
        sources = sources,
        interfaceJars = interfaceJars,
      )
    ) {
      return null
    }

    val mavenCoordinates =
      if (!onlyOutputJars) {
        outputs.firstOrNull()?.let { outputJar ->
          mavenCoordinatesResolver.resolveMavenCoordinates(label, outputJar)
        }
      } else {
        null
      }

    return Library(
      label = label,
      outputs = outputs,
      sources = sources,
      dependencies = targetInfo.dependenciesList.map { Label.parse(it.id) },
      interfaceJars = interfaceJars,
      mavenCoordinates = mavenCoordinates,
      isFromInternalTarget = isInternalTarget,
    )
  }

  private fun shouldCreateLibrary(
    dependencies: List<BspTargetInfo.Dependency>,
    outputs: Collection<Path>,
    interfaceJars: Collection<Path>,
    sources: Collection<Path>,
  ): Boolean = dependencies.isNotEmpty() || !outputs.isEmptyJarList() || !interfaceJars.isEmptyJarList() || !sources.isEmptyJarList()

  private fun Collection<Path>.isEmptyJarList(): Boolean = isEmpty() || singleOrNull()?.fileName?.toString() == "empty.jar"

  private fun List<FileLocation>.resolvePaths() = map { bazelPathsResolver.resolve(it) }.toSet()

  private fun getTargetOutputJarPaths(targetInfo: TargetInfo) =
    getTargetOutputJarsList(targetInfo)
      .toSet()

  private fun getIntellijPluginJars(targetInfo: TargetInfo): Set<Path> {
    // _repackaged_files is created upon calling repackaged_files in rules_intellij
    if (targetInfo.kind != "_repackaged_files") return emptySet()
    return targetInfo.generatedSourcesList
      .resolvePaths()
      .filter { it.extension == "jar" }
      .toSet()
  }

  private fun getSourceJarPaths(targetInfo: TargetInfo) =
    targetInfo.jvmTargetInfo.jarsList
      .flatMap { it.sourceJarsList }
      .resolvePaths()

  private fun getTargetOutputJarsSet(targetInfo: TargetInfo) = getTargetOutputJarsList(targetInfo).toSet()

  private fun getTargetOutputJarsList(targetInfo: TargetInfo) =
    targetInfo.jvmTargetInfo.jarsList
      .flatMap { it.binaryJarsList }
      .map { bazelPathsResolver.resolve(it) }

  private fun getTargetInterfaceJarsSet(targetInfo: TargetInfo) = getTargetInterfaceJarsList(targetInfo).toSet()

  private fun getTargetInterfaceJarsList(targetInfo: TargetInfo) =
    targetInfo.jvmTargetInfo.jarsList
      .flatMap { it.interfaceJarsList }
      .map { bazelPathsResolver.resolve(it) }

  private fun collectInterfacesAndClasses(targets: Sequence<TargetInfo>) =
    targets
      .associate { target ->
        target.label() to
          (getTargetInterfaceJarsList(target) + getTargetOutputJarsList(target))
            .toSet()
      }

  private fun hasKnownJvmSources(targetInfo: TargetInfo) =
    targetInfo.sourcesList.any {
      it.relativePath.endsWith(".java") ||
        it.relativePath.endsWith(".kt") ||
        it.relativePath.endsWith(".scala")
    }

  private fun hasKnownPythonSources(targetInfo: TargetInfo) =
    targetInfo.sourcesList.any {
      it.relativePath.endsWith(".py")
    } ||
      targetInfo.pythonTargetInfo.isCodeGenerator

  private fun hasKnownGoSources(targetInfo: TargetInfo) =
    targetInfo.sourcesList.any {
      it.relativePath.endsWith(".go")
    }

  private fun externalRepositoriesTreatedAsInternal(repoMapping: RepoMapping) =
    when (repoMapping) {
      is BzlmodRepoMapping -> repoMapping.canonicalRepoNameToLocalPath.keys
      is RepoMappingDisabled -> emptySet()
    }

  private fun isTargetTreatedAsInternal(target: ResolvedLabel, repoMapping: RepoMapping): Boolean =
    target.isMainWorkspace || target.repo.repoName in externalRepositoriesTreatedAsInternal(repoMapping) || target.isGazelleGenerated

  // TODO https://youtrack.jetbrains.com/issue/BAZEL-1303
  private fun isWorkspaceTarget(
    target: TargetInfo,
    repoMapping: RepoMapping,
    featureFlags: FeatureFlags,
  ): Boolean =
    (
      isTargetTreatedAsInternal(target.label().assumeResolved(), repoMapping) &&
        (
          shouldImportTargetKind(target.kind) ||
            target.hasJvmTargetInfo() &&
            (
              target.dependenciesCount > 0 ||
                hasKnownJvmSources(target)
            )
        )
    ) ||
      (
        featureFlags.isGoSupportEnabled &&
          target.hasGoTargetInfo() &&
          hasKnownGoSources(target) ||
          featureFlags.isPythonSupportEnabled &&
          target.hasPythonTargetInfo() &&
          hasKnownPythonSources(target)
      ) ||
      target.hasProtobufTargetInfo()

  private fun shouldImportTargetKind(kind: String): Boolean = kind in workspaceTargetKinds

  private val workspaceTargetKinds =
    setOf(
      "java_library",
      "java_binary",
      "java_test",
      "kt_jvm_library",
      "kt_jvm_binary",
      "kt_jvm_test",
      "jvm_library",
      "jvm_binary",
      "jvm_resources",
      "scala_library",
      "scala_binary",
      "scala_test",
      "intellij_plugin_debug_target",
      "go_proto_library",
      "go_library",
      "go_binary",
      "go_test",
    )


  private suspend fun createRawBuildTargets(
    targets: List<IntermediateTargetData>,
    highPrioritySources: Set<Path>,
    repoMapping: RepoMapping,
    dependencyGraph: DependencyGraph,
  ): List<RawBuildTarget> =
    withContext(Dispatchers.Default) {
      val tasks =
        targets.map { target ->
          // async {
          createRawBuildTarget(
            target,
            highPrioritySources,
            repoMapping,
            dependencyGraph,
          )
          // }
        }

      return@withContext tasks
        // .awaitAll()
        .filterNotNull()
        .filterNot { BuildTargetTag.NO_IDE in it.tags }
    }

  private suspend fun createRawBuildTarget(
    targetData: IntermediateTargetData,
    highPrioritySources: Set<Path>,
    repoMapping: RepoMapping,
    dependencyGraph: DependencyGraph,
  ): RawBuildTarget? {
    val target = targetData.target
    val label = targetData.label
    val resolvedDependencies = resolveDirectDependencies(target)
    // https://youtrack.jetbrains.com/issue/BAZEL-983: extra libraries can override some library versions, so they should be put before
    val (extraLibraries, lowPriorityExtraLibraries) = targetData.extraLibraries.partition { !it.isLowPriority }
    val directDependencies = extraLibraries.map { it.label } + resolvedDependencies + lowPriorityExtraLibraries.map { it.label }
    val baseDirectory = bazelPathsResolver.toDirectoryPath(label, repoMapping)
    val languagePlugin = languagePluginsService.getLanguagePlugin(targetData.languages) ?: return null
    val resources = resolveResources(target, languagePlugin)

    val tags = targetData.tags
    val (targetSources, lowPrioritySharedSources) =
      if (tags.hasLowSharedSourcesPriority()) {
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

  // TODO: this is a re-creation of `Language.allOfKind`. To be removed when this logic is merged with client-side
  private val languagesFromKinds: Map<String, Set<LanguageClass>> =
    mapOf(
      "java_library" to setOf(LanguageClass.JAVA),
      "java_binary" to setOf(LanguageClass.JAVA),
      "java_test" to setOf(LanguageClass.JAVA),
      // a workaround to register this target type as Java module in IntelliJ IDEA
      "intellij_plugin_debug_target" to setOf(LanguageClass.JAVA),
      "kt_jvm_library" to setOf(LanguageClass.JAVA, LanguageClass.KOTLIN),
      "kt_jvm_binary" to setOf(LanguageClass.JAVA, LanguageClass.KOTLIN),
      "kt_jvm_test" to setOf(LanguageClass.JAVA, LanguageClass.KOTLIN),
      "scala_library" to setOf(LanguageClass.JAVA, LanguageClass.SCALA),
      "scala_binary" to setOf(LanguageClass.JAVA, LanguageClass.SCALA),
      "scala_test" to setOf(LanguageClass.JAVA, LanguageClass.SCALA),
      // rules_jvm from IntelliJ monorepo
      "jvm_library" to setOf(LanguageClass.JAVA, LanguageClass.KOTLIN),
      "jvm_binary" to setOf(LanguageClass.JAVA, LanguageClass.KOTLIN),
      "jvm_resources" to setOf(LanguageClass.JAVA, LanguageClass.KOTLIN),
      "go_binary" to setOf(LanguageClass.GO),
      "go_test" to setOf(LanguageClass.GO),
      "go_library" to setOf(LanguageClass.GO),
      "go_source" to setOf(LanguageClass.GO),
      "py_binary" to setOf(LanguageClass.PYTHON),
      "py_test" to setOf(LanguageClass.PYTHON),
      "py_library" to setOf(LanguageClass.PYTHON),
    )

  private fun inferLanguages(target: TargetInfo): Set<LanguageClass> =
    buildSet {
      if (target.hasProtobufTargetInfo()) {
        add(LanguageClass.PROTOBUF)
      }
      // TODO It's a hack preserved from before TargetKind refactorking, to be removed
      if (target.hasJvmTargetInfo()) {
        add(LanguageClass.JAVA)
      }
      if (target.hasPythonTargetInfo()) {
        add(LanguageClass.PYTHON)
      }
      if (target.hasGoTargetInfo()) {
        add(LanguageClass.GO)
      }
      languagesFromKinds[target.kind]?.let {
        addAll(it)
      }
    }

  private fun resolveSourceSet(target: TargetInfo, languagePlugin: LanguagePlugin<*>): List<SourceItem> {
    val sources =
      target.sourcesList
        .asSequence()
        .map(bazelPathsResolver::resolve)

    val generatedSources =
      target.generatedSourcesList
        .asSequence()
        .map(bazelPathsResolver::resolve)
        .filter { it.extension != "srcjar" }

    val extraSources = languagePlugin.calculateAdditionalSources(target)

    return (sources + extraSources + generatedSources)
      .distinct()
      .onEach { if (it.notExists()) logNonExistingFile(it, target.id) }
      .map {
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
    languagePlugin
      .collectResources(target)
      .distinct()
      .toList()

  private fun removeDotBazelBspTarget(targets: Collection<Label>): Collection<Label> =
    targets.filter {
      it.isMainWorkspace && !it.packagePath.toString().startsWith(".bazelbsp")
    }

  private fun NonModuleTarget.toBuildTarget(): RawBuildTarget {
    val tags = tags.mapNotNull(BspMappings::toBspTag)
    val buildTarget =
      RawBuildTarget(
        id = label,
        tags = tags,
        kind = inferKind(this.tags, kindString, emptySet()),
        baseDirectory = baseDirectory,
        dependencies = emptyList(),
        sources = emptyList(),
        resources = emptyList(),
        data = null,
      )
    return buildTarget
  }

  private fun Set<Tag>.hasLowSharedSourcesPriority(): Boolean = Tag.IDE_LOW_SHARED_SOURCES_PRIORITY in this

  private fun inferKind(
    tags: Set<Tag>,
    kindString: String,
    languages: Set<LanguageClass>,
  ): TargetKind {
    val ruleType =
      when {
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
