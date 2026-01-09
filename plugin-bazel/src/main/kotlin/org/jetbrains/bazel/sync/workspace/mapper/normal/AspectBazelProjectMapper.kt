package org.jetbrains.bazel.sync.workspace.mapper.normal

import com.google.common.hash.Hashing
import com.google.devtools.build.lib.view.proto.Deps
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
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
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.info.BspTargetInfo.FileLocation
import org.jetbrains.bazel.info.BspTargetInfo.TargetInfo
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.label.assumeResolved
import org.jetbrains.bazel.performance.bspTracer
import org.jetbrains.bazel.performance.telemetry.useWithScope
import org.jetbrains.bazel.server.label.label
import org.jetbrains.bazel.sync.workspace.BazelResolvedWorkspace
import org.jetbrains.bazel.sync.workspace.graph.DependencyGraph
import org.jetbrains.bazel.sync.workspace.languages.LanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.LanguagePluginContext
import org.jetbrains.bazel.sync.workspace.languages.LanguagePluginsService
import org.jetbrains.bazel.sync.workspace.languages.scala.ScalaLanguagePlugin
import org.jetbrains.bazel.sync.workspace.model.BspMappings
import org.jetbrains.bazel.sync.workspace.model.Library
import org.jetbrains.bazel.sync.workspace.model.NonModuleTarget
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.BuildTargetTag
import org.jetbrains.bsp.protocol.FeatureFlags
import org.jetbrains.bsp.protocol.LibraryItem
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.jetbrains.bsp.protocol.ResourceItem
import org.jetbrains.bsp.protocol.SourceItem
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.inputStream
import kotlin.io.path.name
import kotlin.io.path.notExists

class AspectBazelProjectMapper(
  private val project: Project,
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
    val tags = targetTagsResolver.resolveTags(this).toSet()
    val label = this.label().assumeResolved()
    return IntermediateTargetData(
      label = label,
      target = this,
      extraLibraries = extraLibraries[label] ?: emptyList(),
      tags = tags,
      sources = resolveSourceSet(this, languagePlugin).toList(),
      languages = languages,
    )
  }

  suspend fun createProject(
    allTargets: Map<Label, TargetInfo>,
    rootTargets: Set<Label>,
    workspaceContext: WorkspaceContext,
    featureFlags: FeatureFlags,
    repoMapping: RepoMapping,
    hasError: Boolean,
  ): BazelResolvedWorkspace {
    languagePluginsService.all.forEach { it.prepareSync(project, allTargets.values.asSequence(), workspaceContext) }
    val dependencyGraph =
      measure("Build dependency tree") {
        DependencyGraph(rootTargets, allTargets)
      }
    val (targetsToImport, targetsAsLibraries) =
      measure("Select targets") {
        val targetsAtDepth =
          dependencyGraph
            .allTargetsAtDepth(
              workspaceContext.importDepth,
              isExternalTarget = { !isTargetTreatedAsInternal(it.assumeResolved(), repoMapping) },
              targetSupportsStrictDeps = { id -> allTargets[id]?.let { targetSupportsStrictDeps(it) } == true },
              isWorkspaceTarget = { id ->
                allTargets[id]?.let { target ->
                  target.sourcesCount > 0 && isWorkspaceTarget(target, repoMapping, featureFlags)
                } == true
              },
            )
        val (targetsToImport, nonWorkspaceTargets) =
          targetsAtDepth.targets.partition {
            isWorkspaceTarget(it, repoMapping, featureFlags)
          }
        val (jvmDirectDependencies, nonJvmDirectDependencies) = targetsAtDepth.directDependencies.partition { it.hasJvmTargetInfo() }
        val jvmLibraries = (nonWorkspaceTargets + jvmDirectDependencies).associateBy { it.label() }
        (targetsToImport + nonJvmDirectDependencies).asSequence() to jvmLibraries
      }
    val interfacesAndBinariesFromTargetsToImport =
      measure("Collect interfaces and classes from targets to import") {
        collectInterfacesAndClasses(targetsToImport)
      }
    val outputJarsLibraries =
      measure("Create output jars libraries") {
        calculateOutputJarsLibraries(workspaceContext, targetsToImport)
      }
    val annotationProcessorLibraries =
      measure("Create AP libraries") {
        annotationProcessorLibraries(targetsToImport)
      }
    val kotlinStdlibsMapper =
      measure("Create kotlin stdlibs") {
        calculateKotlinStdlibsMapper(targetsToImport, dependencyGraph)
      }
    val kotlincPluginLibrariesMapper =
      measure("Create kotlinc plugin libraries") {
        calculateKotlincPluginLibrariesMapper(targetsToImport)
      }
    val scalaLibrariesMapper =
      measure("Create scala libraries") {
        calculateScalaLibrariesMapper(targetsToImport)
      }
    val librariesFromDeps =
      measure("Merge libraries from deps") {
        concatenateMaps(
          outputJarsLibraries,
          annotationProcessorLibraries,
          kotlinStdlibsMapper,
          kotlincPluginLibrariesMapper,
          scalaLibrariesMapper,
        )
      }
    val librariesFromDepsAndTargets =
      measure("Libraries from targets and deps") {
        createLibraries(workspaceContext, targetsAsLibraries) +
          librariesFromDeps.values
            .flatten()
            .distinct()
            .associateBy { it.label }
      }
    val extraLibrariesFromJdeps =
      measure("Libraries from jdeps") {
        jdepsLibraries(
          targetsToImport.associateBy { it.label() },
          librariesFromDeps,
          librariesFromDepsAndTargets,
          interfacesAndBinariesFromTargetsToImport,
        )
      }
    val librariesToImport =
      measure("Merge all libraries") {
        librariesFromDepsAndTargets +
          extraLibrariesFromJdeps.values.flatten().associateBy { it.label }
      }

    val extraLibraries = concatenateMaps(librariesFromDeps, extraLibrariesFromJdeps)

    val targets =
      measure("create intermediate targets") {
        // Use targetsToImport here instead of allTargets here to respect import_depth
        createIntermediateTargetData(workspaceContext, targetsToImport, extraLibraries)
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
    val nonModuleRawTargets = measure("create non module raw targets") {
      val nonModuleTargets =
        createNonModuleTargets(
          allTargets,
          dependencyGraph,
          repoMapping,
          workspaceContext,
        )
      nonModuleTargets.map { it.toBuildTarget() }
    }

    val workspaceTargets = rawTargets.associateByTo(mutableMapOf()) { target -> target.id }
    nonModuleRawTargets.forEach { target -> workspaceTargets.putIfAbsent(target.id, target) }
    return BazelResolvedWorkspace(
      targets = workspaceTargets.values.toList(),
      libraries =
        librariesToImport.values.map {
          LibraryItem(
            id = it.label,
            dependencies = it.dependencies,
            ijars = it.interfaceJars.toList(),
            jars = it.outputs.toList(),
            sourceJars = it.sources.toList(),
            mavenCoordinates = it.mavenCoordinates,
            containsInternalJars = it.containsInternalJars,
            isLowPriority = it.isLowPriority,
          )
        },
      hasError = hasError,
    )
  }

  private suspend fun AspectBazelProjectMapper.createIntermediateTargetData(
    workspaceContext: WorkspaceContext,
    targets: Sequence<TargetInfo>,
    extraLibraries: Map<Label, List<Library>>,
  ): List<IntermediateTargetData> =
    withContext(Dispatchers.Default) {
      val tasks = targets.map {
        async {
          it.toIntermediateData(workspaceContext, extraLibraries)
        }
      }

      return@withContext tasks
        .toList()
        .awaitAll()
        .filterNotNull()
    }

  private fun <K, V> concatenateMaps(vararg maps: Map<K, List<V>>): Map<K, List<V>> =
    maps
      .flatMap { it.keys }
      .distinct()
      .associateWith { key ->
        maps.flatMap { it[key].orEmpty() }
      }

  private fun calculateOutputJarsLibraries(
    workspaceContext: WorkspaceContext,
    targetsToImport: Sequence<TargetInfo>,
  ): Map<Label, List<Library>> =
    targetsToImport
      .filter { shouldCreateOutputJarsLibrary(it) }
      .mapNotNull { target ->
        createLibrary(
          workspaceContext,
          Label.parse(target.id + "_output_jars"),
          target,
          onlyOutputJars = true,
          containsInternalJars = true,
        )?.let { library ->
          target.label() to listOf(library)
        }
      }.toMap()

  private fun shouldCreateOutputJarsLibrary(targetInfo: TargetInfo) =
    !targetInfo.kind.endsWith("_resources") && targetInfo.hasJvmTargetInfo() &&
      (
        targetInfo.generatedSourcesList.any { it.relativePath.endsWith(".srcjar") } ||
          (targetInfo.sourcesList.isNotEmpty() && !hasKnownJvmSources(targetInfo)) ||
          (targetInfo.sourcesList.isEmpty() && targetInfo.kind !in workspaceTargetKinds && !targetInfo.executable) ||
          targetInfo.jvmTargetInfo.hasApiGeneratingPlugins
        )

  private fun annotationProcessorLibraries(targetsToImport: Sequence<TargetInfo>): Map<Label, List<Library>> =
    targetsToImport
      .filter { it.jvmTargetInfo.generatedJarsList.isNotEmpty() }
      .associate { targetInfo ->
        targetInfo.id to
          Library(
            label = Label.parse(targetInfo.id + "_generated"),
            outputs =
              targetInfo.jvmTargetInfo.generatedJarsList
                .flatMap { it.binaryJarsList }
                .map { bazelPathsResolver.resolve(it) }
                .toSet(),
            sources =
              targetInfo.jvmTargetInfo.generatedJarsList
                .flatMap { it.sourceJarsList }
                .map { bazelPathsResolver.resolve(it) }
                .toSet(),
            dependencies = emptyList(),
            interfaceJars = emptySet(),
          )
      }.map { Label.parse(it.key) to listOf(it.value) }
      .toMap()

  private fun targetsDependingOnKotlinStdlib(targetsToImport: Sequence<TargetInfo>, dependencyGraph: DependencyGraph): Set<Label> {
    val toProcess = ArrayDeque(targetsToImport.filter { it.kotlinTargetInfo != null }.map { it.label() }.toList())
    val visited = toProcess.toMutableSet()
    while (toProcess.isNotEmpty()) {
      val info = toProcess.removeFirst()
      for (reverseDep in dependencyGraph.getReverseDependenciesInfo(info)) {
        val reverseDepLabel = reverseDep.label()
        if (reverseDepLabel in visited) continue
        if (!reverseDep.hasJvmTargetInfo()) continue
        toProcess.add(reverseDepLabel)
        visited.add(reverseDepLabel)
      }
    }
    return visited
  }

  private fun calculateKotlinStdlibsMapper(
    targetsToImport: Sequence<TargetInfo>,
    dependencyGraph: DependencyGraph,
  ): Map<Label, List<Library>> {
    val projectLevelKotlinStdlibsLibrary = calculateProjectLevelKotlinStdlibsLibrary(targetsToImport)
    val kotlinDependentTargetsIds = targetsDependingOnKotlinStdlib(targetsToImport, dependencyGraph)

    return projectLevelKotlinStdlibsLibrary
      ?.let { stdlibsLibrary -> kotlinDependentTargetsIds.associateWith { listOf(stdlibsLibrary) } }
      .orEmpty()
  }

  private fun calculateProjectLevelKotlinStdlibsLibrary(targetsToImport: Sequence<TargetInfo>): Library? {
    val kotlinStdlibsJars = calculateProjectLevelKotlinStdlibsJars(targetsToImport)

    // rules_kotlin does not expose source jars for jvm stdlibs, so this is the way they can be retrieved for now
    val inferredSourceJars =
      kotlinStdlibsJars
        .map { it.parent.resolve(it.fileName.toString().replace(".jar", "-sources.jar")) }
        .onEach { if (it.notExists()) logNonExistingFile(it, "[kotlin stdlib]") }
        .toSet()

    return if (kotlinStdlibsJars.isNotEmpty()) {
      Library(
        label = Label.synthetic("rules_kotlin_kotlin-stdlibs"),
        outputs = kotlinStdlibsJars,
        sources = inferredSourceJars,
        dependencies = emptyList(),
        // https://youtrack.jetbrains.com/issue/BAZEL-2284/NotNull-not-applicable-to-type-use#focus=Comments-27-12502660.0-0
        // Make sure that if the user provides Kotlin stdlib in deps then it overrides the one inferred from rules_kotlin
        isLowPriority = true,
      )
    } else {
      null
    }
  }

  private fun calculateProjectLevelKotlinStdlibsJars(targetsToImport: Sequence<TargetInfo>): Set<Path> =
    targetsToImport
      .filter { it.hasKotlinTargetInfo() }
      .map { it.kotlinTargetInfo.stdlibsList }
      .flatMap { it.resolvePaths() }
      .toSet()

  private fun calculateKotlincPluginLibrariesMapper(targetsToImport: Sequence<TargetInfo>): Map<Label, List<Library>> =
    targetsToImport
      .filter { it.hasKotlinTargetInfo() && it.kotlinTargetInfo.kotlincPluginInfosList.isNotEmpty() }
      .associate {
        val pluginClasspaths =
          it.kotlinTargetInfo.kotlincPluginInfosList
            .flatMap { it.pluginJarsList }
            .map { bazelPathsResolver.resolve(it) }
            .distinct()
        Pair(
          it.label(),
          pluginClasspaths.map { classpath ->
            Library(
              label = Label.synthetic(classpath.name),
              outputs = setOf(classpath),
              sources = emptySet(),
              dependencies = emptyList(),
            )
          },
        )
      }

  // TODO: refactor to language-specific logic
  private fun calculateScalaLibrariesMapper(targetsToImport: Sequence<TargetInfo>): Map<Label, List<Library>> {
    val projectLevelScalaSdkLibraries = calculateProjectLevelScalaSdkLibraries()
    val projectLevelScalaTestLibraries = calculateProjectLevelScalaTestLibraries()
    val scalaTargets = targetsToImport.filter { it.hasScalaTargetInfo() }.map { it.label() }
    val scalaPlugin = languagePluginsService.getLanguagePlugin<ScalaLanguagePlugin>(LanguageClass.SCALA)
    return scalaTargets.associateWith {
      val sdkLibraries =
        scalaPlugin.scalaSdks[it]
          ?.compilerJars
          ?.mapNotNull {
            projectLevelScalaSdkLibraries[it]
          }.orEmpty()
      val testLibraries =
        scalaPlugin.scalaTestJars[it]
          ?.mapNotNull {
            projectLevelScalaTestLibraries[it]
          }.orEmpty()

      (sdkLibraries + testLibraries).distinct()
    }
  }

  private fun calculateProjectLevelScalaSdkLibraries(): Map<Path, Library> =
    getProjectLevelScalaSdkLibrariesJars().associateWith {
      Library(
        label = Label.synthetic(it.name),
        outputs = setOf(it),
        sources = emptySet(),
        dependencies = emptyList(),
      )
    }

  // TODO: refactor to language-specific logic
  private fun calculateProjectLevelScalaTestLibraries(): Map<Path, Library> {
    val scalaPlugin = languagePluginsService.getLanguagePlugin<ScalaLanguagePlugin>(LanguageClass.SCALA)
    return scalaPlugin.scalaTestJars.values
      .flatten()
      .toSet()
      .associateWith {
        Library(
          label = Label.synthetic(it.name),
          outputs = setOf(it),
          sources = emptySet(),
          dependencies = emptyList(),
        )
      }
  }

  // TODO: refactor to language-specific logic
  private fun getProjectLevelScalaSdkLibrariesJars(): Set<Path> {
    val scalaPlugin = languagePluginsService.getLanguagePlugin<ScalaLanguagePlugin>(LanguageClass.SCALA)
    return scalaPlugin.scalaSdks.values
      .toSet()
      .flatMap {
        it.compilerJars
      }.toSet()
  }

  /**
   * In some cases, the jar dependencies of a target might be injected by bazel or rules and not are not
   * available via `deps` field of a target. For this reason, we read JavaOutputInfo's jdeps file and
   * filter out jars that have not been included in the target's `deps` list.
   *
   * The old Bazel Plugin performs similar step here
   * https://github.com/bazelbuild/intellij/blob/b68ec8b33aa54ead6d84dd94daf4822089b3b013/java/src/com/google/idea/blaze/java/sync/importer/BlazeJavaWorkspaceImporter.java#L256
   */
  private suspend fun jdepsLibraries(
    targetsToImport: Map<Label, TargetInfo>,
    libraryDependencies: Map<Label, List<Library>>,
    librariesToImport: Map<Label, Library>,
    interfacesAndBinariesFromTargetsToImport: Map<Label, Set<Path>>,
  ): Map<Label, List<Library>> {
    val targetsToJdepsJars =
      getAllJdepsDependencies(targetsToImport, libraryDependencies, librariesToImport)
    val libraryNameToLibraryValueMap = HashMap<Label, Library>()
    return targetsToJdepsJars.mapValues { target ->
      val interfacesAndBinariesFromTarget =
        interfacesAndBinariesFromTargetsToImport.getOrDefault(target.key, emptySet())
      target.value
        .map { path -> bazelPathsResolver.resolve(path) }
        .filter { it !in interfacesAndBinariesFromTarget }
        .mapNotNull {
          if (shouldSkipJdepsJar(it)) return@mapNotNull null
          val label = syntheticLabel(it)
          libraryNameToLibraryValueMap.computeIfAbsent(label) { _ ->
            Library(
              label = label,
              dependencies = emptyList(),
              interfaceJars = emptySet(),
              outputs = setOf(it),
              sources = emptySet(),
            )
          }
        }
    }
  }

  // See https://github.com/bazel-contrib/rules_jvm_external/issues/786
  private fun shouldSkipJdepsJar(jar: Path): Boolean =
    jar.name.startsWith("header_") && jar.resolveSibling("processed_${jar.name.substring(7)}").exists()

  private suspend fun getAllJdepsDependencies(
    targetsToImport: Map<Label, TargetInfo>,
    libraryDependencies: Map<Label, List<Library>>,
    librariesToImport: Map<Label, Library>,
  ): Map<Label, Set<Path>> {
    val jdepsJars =
      withContext(Dispatchers.IO) {
        targetsToImport.values
          .filter { targetSupportsJdeps(it) }
          .map { target ->
            async {
              target.label() to dependencyJarsFromJdepsFiles(target)
            }
          }.awaitAll()
      }.filter { it.second.isNotEmpty() }.toMap()

    val allJdepsJars =
      jdepsJars.values
        .asSequence()
        .flatten()
        .toSet()

    return withContext(Dispatchers.Default) {
      val outputJarsFromTransitiveDepsCache = ConcurrentHashMap<Label, Set<Path>>()
      jdepsJars
        .map { (targetLabel, jarsFromJdeps) ->
          async {
            val transitiveJdepsJars =
              getJdepsJarsFromTransitiveDependencies(
                targetLabel,
                targetsToImport,
                libraryDependencies,
                librariesToImport,
                outputJarsFromTransitiveDepsCache,
                allJdepsJars,
              )
            targetLabel to jarsFromJdeps - transitiveJdepsJars
          }
        }.awaitAll()
        .toMap()
        .filterValues { it.isNotEmpty() }
    }
  }

  private fun getJdepsJarsFromTransitiveDependencies(
    targetOrLibrary: Label,
    targetsToImport: Map<Label, TargetInfo>,
    libraryDependencies: Map<Label, List<Library>>,
    librariesToImport: Map<Label, Library>,
    outputJarsFromTransitiveDepsCache: ConcurrentHashMap<Label, Set<Path>>,
    allJdepsJars: Set<Path>,
  ): Set<Path> =
    outputJarsFromTransitiveDepsCache.getOrPut(targetOrLibrary) {
      val jarsFromTargets =
        targetsToImport[targetOrLibrary]?.let { getTargetOutputJarsSet(it) + getTargetInterfaceJarsSet(it) }.orEmpty()
      val jarsFromLibraries =
        librariesToImport[targetOrLibrary]?.let { it.outputs + it.interfaceJars }.orEmpty()
      val outputJars =
        listOfNotNull(jarsFromTargets, jarsFromLibraries)
          .asSequence()
          .flatten()
          .filter { it in allJdepsJars }
          .toMutableSet()

      val dependencies =
        targetsToImport[targetOrLibrary]?.dependenciesList.orEmpty().map { it.label() } +
          libraryDependencies[targetOrLibrary].orEmpty().map { it.label } +
          librariesToImport[targetOrLibrary]?.dependencies.orEmpty().map { it.label }

      dependencies.flatMapTo(outputJars) { dependency ->
        getJdepsJarsFromTransitiveDependencies(
          dependency,
          targetsToImport,
          libraryDependencies,
          librariesToImport,
          outputJarsFromTransitiveDepsCache,
          allJdepsJars,
        )
      }
      outputJars
    }

  private fun dependencyJarsFromJdepsFiles(targetInfo: TargetInfo): Set<Path> =
    targetInfo.jvmTargetInfo.jdepsList
      .flatMap { jdeps ->
        val path = bazelPathsResolver.resolve(jdeps)
        if (path.toFile().exists()) {
          val dependencyList =
            path.inputStream().use {
              Deps.Dependencies.parseFrom(it).dependencyList
            }
          dependencyList
            .asSequence()
            .filter { it.isRelevant() }
            .map { bazelPathsResolver.resolveOutput(Paths.get(it.path)) }
            .toList()
        } else {
          emptySet()
        }
      }.toSet()

  /**
   * Similar to what was done in the Google's Bazel plugin in JdepsFileReader#relevantDep,
   * we should only include deps that are actually used by the compiler
   */
  private fun Deps.Dependency.isRelevant() = kind in sequenceOf(Deps.Dependency.Kind.EXPLICIT, Deps.Dependency.Kind.IMPLICIT)

  private fun targetSupportsJdeps(targetInfo: TargetInfo): Boolean {
    val languages = inferLanguages(targetInfo)
    return setOf(LanguageClass.JAVA, LanguageClass.KOTLIN, LanguageClass.SCALA).containsAll(languages)
  }

  private val replacementRegex = "[^0-9a-zA-Z]".toRegex()

  private fun syntheticLabel(lib: Path): Label {
    val shaOfPath =
      Hashing
        .sha256()
        .hashString(lib.toString(), StandardCharsets.UTF_8)
        .toString()
        .take(7) // just in case of a conflict in filename
    return Label.synthetic(
      lib
        .fileName
        .toString()
        .replace(replacementRegex, "-") + "-" + shaOfPath,
    )
  }

  private fun createNonModuleTargets(
    allTargets: Map<Label, TargetInfo>,
    dependencyGraph: DependencyGraph,
    repoMapping: RepoMapping,
    workspaceContext: WorkspaceContext,
  ): List<NonModuleTarget> {
    val allTargetsAtDepth = dependencyGraph.allTargetsAtDepth(
      workspaceContext.importDepth,
      isExternalTarget = { false },
      targetSupportsStrictDeps = { true },
      isWorkspaceTarget = { true },  // Take everything, including non-workspace targets
    ).targets.map { it.label() }

    return allTargetsAtDepth
      .filter { label -> isTargetTreatedAsInternal(label.assumeResolved(), repoMapping) }
      .filter { label -> label.packagePath.pathSegments.firstOrNull() != Constants.DOT_BAZELBSP_DIR_NAME }
      .map { label ->
        val targetInfo = allTargets.getValue(label)
        NonModuleTarget(
          label = label,
          tags = targetTagsResolver.resolveTags(targetInfo),
          baseDirectory = bazelPathsResolver.toDirectoryPath(label.assumeResolved(), repoMapping),
          kindString = targetInfo.kind,
        )
      }
  }

  private suspend fun createLibraries(
    workspaceContext: WorkspaceContext,
    targets: Map<Label, TargetInfo>,
  ): Map<Label, Library> =
    withContext(Dispatchers.Default) {
      targets
        .map { (targetId, targetInfo) ->
          async {
            createLibrary(
              workspaceContext = workspaceContext,
              label = targetId,
              targetInfo = targetInfo,
              onlyOutputJars = false,
              containsInternalJars = targetInfo.containsAnyInternalJars(),
            )?.let { library ->
              targetId to library
            }
          }
        }.awaitAll()
        .filterNotNull()
        .toMap()
    }

  private fun TargetInfo.containsAnyInternalJars() = jvmTargetInfo.jarsList.any { jars ->
    jars.sourceJarsList.any { !it.isExternal } && jars.binaryJarsList.any { !it.isExternal }
  }

  private fun createLibrary(
    workspaceContext: WorkspaceContext,
    label: Label,
    targetInfo: TargetInfo,
    onlyOutputJars: Boolean,
    containsInternalJars: Boolean,
  ): Library? {
    val outputs = getTargetOutputJarPaths(targetInfo) + getIntellijPluginJars(targetInfo)
    val rawSources = getSourceJarPaths(targetInfo);
    val sources = if (workspaceContext.preferClassJarsOverSourcelessJars) {
      rawSources - outputs
    } else {
      rawSources
    }

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
      dependencies = targetInfo.dependenciesList.map { it.toDependencyLabel() },
      interfaceJars = interfaceJars,
      mavenCoordinates = mavenCoordinates,
      containsInternalJars = containsInternalJars,
    )
  }

  private fun shouldCreateLibrary(
    dependencies: List<BspTargetInfo.Dependency>,
    outputs: Collection<Path>,
    interfaceJars: Collection<Path>,
    sources: Collection<Path>,
  ): Boolean = dependencies.isNotEmpty() || !outputs.isEmptyJarList() || !interfaceJars.isEmptyJarList() || !sources.isEmptyJarList()

  private fun Collection<Path>.isEmptyJarList(): Boolean = isEmpty() || singleOrNull()?.name == "empty.jar"

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
      "resourcegroup",
    )

  // TODO BAZEL-2208
  // The only language that supports strict deps by default is Java, in Kotlin and Scala strict deps are disabled by default.
  private fun targetSupportsStrictDeps(target: TargetInfo): Boolean =
    target.hasJvmTargetInfo() && !target.hasScalaTargetInfo() && !target.hasKotlinTargetInfo()

  private suspend fun createRawBuildTargets(
    targets: List<IntermediateTargetData>,
    highPrioritySources: Set<Path>,
    repoMapping: RepoMapping,
    dependencyGraph: DependencyGraph,
  ): List<RawBuildTarget> =
    withContext(Dispatchers.Default) {
      val tasks =
        targets.map { target ->
          async {
            createRawBuildTarget(
              target,
              highPrioritySources,
              repoMapping,
              dependencyGraph,
            )
          }
        }

      return@withContext tasks
        .awaitAll()
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
    val directDependencies = extraLibraries.toDependencyLabels() + resolvedDependencies + lowPriorityExtraLibraries.toDependencyLabels()
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

    val context = LanguagePluginContext(target, dependencyGraph, repoMapping, targetData.sources, bazelPathsResolver)
    val data = languagePlugin.createBuildTargetData(context, target)

    return RawBuildTarget(
      id = label,
      tags = tags.mapNotNull(BspMappings::toBspTag),
      dependencies = directDependencies,
      kind = inferKind(tags, target.kind, targetData.languages),
      sources = languagePlugin.transformSources(targetSources),
      resources = resources,
      baseDirectory = baseDirectory,
      noBuild = Tag.NO_BUILD in tags,
      data = data,
      lowPrioritySharedSources = lowPrioritySharedSources,
    )
  }

  private fun resolveDirectDependencies(target: TargetInfo): List<DependencyLabel> =
    target.dependenciesList.map { it.toDependencyLabel() }

  private fun BspTargetInfo.Dependency.toDependencyLabel(): DependencyLabel =
    DependencyLabel(
      label = label(),
      isRuntime = dependencyType == BspTargetInfo.Dependency.DependencyType.RUNTIME,
      exported = exported,
    )

  private fun List<Library>.toDependencyLabels(): List<DependencyLabel> = this.map { DependencyLabel(it.label) }

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
      if (target.hasUltimateTargetInfo()) {
        add(LanguageClass.ULTIMATE)
      }
      languagesFromKinds[target.kind]?.let {
        addAll(it)
      }
    }

  private fun resolveSourceSet(target: TargetInfo, languagePlugin: LanguagePlugin<*>): Sequence<SourceItem> {
    val sources =
      target.sourcesList
        .asSequence()
        .map(bazelPathsResolver::resolve)

    val generatedSources =
      target.generatedSourcesList
        .asSequence()
        .map(bazelPathsResolver::resolve)
        .filter { it.extension != "srcjar" }

    val sourceItems = sources.map {
      SourceItem(
        path = bazelPathsResolver.resolve(it),
        generated = false
      )
    }

    val generatedSourceItems = generatedSources.map {
      SourceItem(
        path = bazelPathsResolver.resolve(it),
        generated = true
      )
    }

    val extraSources = languagePlugin.calculateAdditionalSources(target)

    return (sourceItems + generatedSourceItems + extraSources)
      .distinctBy { it.path }
      .onEach { if (it.path.notExists()) logNonExistingFile(it.path, target.id) }
  }

  private fun logNonExistingFile(file: Path, targetId: String) {
    val message = "target $targetId: $file does not exist."
    logger.warn(message)
  }

  private fun resolveResources(target: TargetInfo, languagePlugin: LanguagePlugin<*>): List<ResourceItem> {
    val resources = bazelPathsResolver
      .resolvePaths(target.resourcesList)
      .asSequence()
      .plus(languagePlugin.resolveAdditionalResources(target))
      .distinct()
      .map { ResourceItem.File(it) }
    val resourceTargets = target.resourceTargetsList
      .asSequence()
      .map { ResourceItem.Target(Label.parse(it)) }
      .distinct()
    return (resources + resourceTargets).toList()
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
