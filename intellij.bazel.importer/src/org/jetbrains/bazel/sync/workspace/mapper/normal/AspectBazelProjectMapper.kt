package org.jetbrains.bazel.sync.workspace.mapper.normal

import com.google.common.hash.Hashing
import com.google.devtools.build.lib.view.proto.Deps
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.platform.diagnostic.telemetry.helpers.useWithScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.BzlmodRepoMapping
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.LocalRepositoryMapping
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.commons.RepoMappingDisabled
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.commons.getLocalRepositories
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.info.BspTargetInfo.ArtifactLocation
import org.jetbrains.bazel.info.BspTargetInfo.TargetInfo
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.label.assumeResolved
import org.jetbrains.bazel.performance.bspTracer
import org.jetbrains.bazel.server.label.label
import org.jetbrains.bazel.server.model.generatedSourcesList
import org.jetbrains.bazel.server.model.sourcesList
import org.jetbrains.bazel.sync.workspace.BazelResolvedWorkspace
import org.jetbrains.bazel.sync.workspace.graph.DependencyGraph
import org.jetbrains.bazel.sync.workspace.languages.LanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.LanguagePluginContext
import org.jetbrains.bazel.sync.workspace.languages.LanguagePluginsService
import org.jetbrains.bazel.sync.workspace.languages.scala.ScalaLanguagePlugin
import org.jetbrains.bazel.sync.workspace.mapper.BazelResolvedWorkspaceBuilder
import org.jetbrains.bazel.sync.workspace.targetKind.TargetKindService
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.BuildTargetTag
import org.jetbrains.bsp.protocol.FeatureFlags
import org.jetbrains.bsp.protocol.LibraryItem
import org.jetbrains.bsp.protocol.MavenCoordinates
import org.jetbrains.bsp.protocol.RawBuildTarget
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

internal class AspectBazelProjectMapper(
  private val project: Project,
  private val languagePluginsService: LanguagePluginsService,
  private val bazelPathsResolver: BazelPathsResolver,
  private val mavenCoordinatesResolver: MavenCoordinatesResolver,
) {
  val logger = logger<AspectBazelProjectMapper>()

  private suspend fun <T> measure(description: String, body: suspend () -> T): T =
    bspTracer.spanBuilder(description).useWithScope { body() }

  private data class IntermediateTargetData(
    val label: ResolvedLabel,
    val target: TargetInfo,
    val extraLibraries: Collection<LibraryItem>,
    val librariesFromToolchains: List<LibraryItem>,
    val targetKind: TargetKind,
    val tags: Set<String>,
    val sources: List<SourceItem>,
    val generatorName: String?,
  )

  private fun TargetInfo.toIntermediateData(
    extraLibraries: Map<Label, List<LibraryItem>>,
    librariesFromToolchains: Map<Label, List<LibraryItem>>,
    repoMapping: RepoMapping,
    targetKinds: Map<Label, TargetKind>,
  ): IntermediateTargetData? {
    val label = this.label().assumeResolved()
    val targetKind = targetKinds.getValue(label)
    val languagePlugin = languagePluginsService.getLanguagePlugin(targetKind.languageClasses) ?: return null
    return IntermediateTargetData(
      label = label,
      target = this,
      extraLibraries = extraLibraries[label] ?: emptyList(),
      librariesFromToolchains = librariesFromToolchains[label] ?: emptyList(),
      targetKind = targetKind,
      tags = tagsList.toSet(),
      sources = resolveSourceSet(this, languagePlugin, repoMapping).toList(),
      generatorName = generatorName.takeIf { it.isNotEmpty() },
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
    languagePluginsService.all.forEach { it.prepareSync(project, allTargets, workspaceContext, repoMapping) }
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
                  target.sourcesList.any() && isWorkspaceTarget(target, repoMapping, featureFlags, workspaceContext)
                } == true
              },
            )
        val (targetsToImport, nonWorkspaceTargets) =
          targetsAtDepth.targets.partition {
            isWorkspaceTarget(it, repoMapping, featureFlags, workspaceContext)
          }
        val (jvmDirectDependencies, nonJvmDirectDependencies) = targetsAtDepth.directDependencies.partition { it.javaCommon.jvmTarget }
        val jvmLibraries = (nonWorkspaceTargets + jvmDirectDependencies).associateBy { it.label() }
        (targetsToImport + nonJvmDirectDependencies) to jvmLibraries
      }
    val interfacesAndBinariesFromTargetsToImport =
      measure("Collect interfaces and classes from targets to import") {
        collectInterfacesAndClasses(targetsToImport, repoMapping)
      }
    val outputJarsLibraries =
      measure("Create output jars libraries") {
        calculateOutputJarsLibraries(workspaceContext, targetsToImport, allTargets, repoMapping)
      }
    val annotationProcessorLibraries =
      measure("Create AP libraries") {
        annotationProcessorLibraries(targetsToImport, repoMapping)
      }
    val kotlinStdlibsMapper =
      measure("Create kotlin stdlibs") {
        calculateKotlinStdlibsMapper(targetsToImport, repoMapping)
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
        )
      }
    val librariesFromToolchains =
      concatenateMaps(
        kotlinStdlibsMapper,
        scalaLibrariesMapper,
      )
    val librariesFromDepsAndTargets =
      measure("Libraries from targets and deps") {
        createLibraries(workspaceContext, targetsAsLibraries, repoMapping  ) +
        (librariesFromDeps.values.asSequence() + librariesFromToolchains.values.asSequence())
            .flatten()
            .distinct()
            .associateBy { it.id }
      }
    val targetKinds =
      measure("Target kinds") {
        val targetKindService = TargetKindService.getInstance()
        allTargets.map { (label, target) ->
          label to targetKindService.fromTargetInfo(target)
        }.toMap()
      }
    val extraLibrariesFromJdeps =
      measure("Libraries from jdeps") {
        jdepsLibraries(
          targetsToImport.associateBy { it.label() },
          concatenateMaps(librariesFromDeps, librariesFromToolchains),
          librariesFromDepsAndTargets,
          interfacesAndBinariesFromTargetsToImport,
          repoMapping,
          targetKinds,
        )
      }
    val librariesToImport =
      measure("Merge all libraries") {
        librariesFromDepsAndTargets +
        extraLibrariesFromJdeps.values.flatten().associateBy { it.id }
      }

    val extraLibraries = concatenateMaps(librariesFromDeps, extraLibrariesFromJdeps)

    val targets =
      measure("create intermediate targets") {
        // Use targetsToImport here instead of allTargets here to respect import_depth
        createIntermediateTargetData(targetsToImport, extraLibraries, librariesFromToolchains, repoMapping, targetKinds)
      }

    val rawTargets = measure("create raw targets") { createRawBuildTargets(targets, repoMapping, dependencyGraph) }
    val nonModuleRawTargets = measure("create non module raw targets") {
      val nonModuleTargets =
        createNonModuleTargets(
          allTargets,
          dependencyGraph,
          repoMapping,
          workspaceContext,
          targetKinds,
        )
      nonModuleTargets.map { it.toBuildTarget() }
    }

    val workspaceTargets = rawTargets.associateByTo(mutableMapOf()) { target -> target.id }
    nonModuleRawTargets.forEach { target -> workspaceTargets.putIfAbsent(target.id, target) }
    return BazelResolvedWorkspaceBuilder.build(
      targets = workspaceTargets.values.toList(),
      libraries = librariesToImport.values.toList(),
      hasError = hasError,
    )
  }

  private suspend fun AspectBazelProjectMapper.createIntermediateTargetData(
    targets: List<TargetInfo>,
    extraLibraries: Map<Label, List<LibraryItem>>,
    librariesFromToolchains: Map<Label, List<LibraryItem>>,
    repoMapping: RepoMapping,
    targetKinds: Map<Label, TargetKind>,
  ): List<IntermediateTargetData> =
    withContext(Dispatchers.Default) {
      val tasks = targets.map {
        async {
          it.toIntermediateData(extraLibraries, librariesFromToolchains, repoMapping, targetKinds)
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

  private suspend fun calculateOutputJarsLibraries(
    workspaceContext: WorkspaceContext,
    targetsToImport: List<TargetInfo>,
    allTargets: Map<Label, TargetInfo>,
    repoMapping: RepoMapping,
  ): Map<Label, List<LibraryItem>> {
    val localRepositories = repoMapping.getLocalRepositories()
    return targetsToImport
      .filter { shouldCreateOutputJarsLibrary(it, allTargets) }
      .mapNotNull { target ->
        createLibrary(
          workspaceContext,
          Label.parse(target.key.label + "_output_jars"),
          target,
          onlyOutputJars = true,
          localRepositories
        )?.let { library ->
          target.label() to listOf(library)
        }
      }.toMap()
  }

  private fun shouldCreateOutputJarsLibrary(targetInfo: TargetInfo, allTargets : Map<Label, TargetInfo>) =
    !targetInfo.kind.endsWith("_resources") && targetInfo.javaCommon.jvmTarget &&
      (
        targetInfo.generatedSourcesList.any { it.relativePath.endsWith(".srcjar") } ||
          (targetInfo.sourcesList.any() && !hasKnownJvmSources(targetInfo)) ||
          (targetInfo.sourcesList.none() && targetInfo.kind !in workspaceTargetKinds && !targetInfo.executable) ||
          targetInfo.javaProvider.hasApiGeneratingPlugins ||
          targetInfo.kotlinTargetInfo.exportedCompilerPluginTargetsFromDepsList.any { allTargets.get(Label.parse(it))?.javaProvider?.hasApiGeneratingPlugins ?: false }
        )

  private suspend fun annotationProcessorLibraries(targetsToImport: List<TargetInfo>, repoMapping: RepoMapping): Map<Label, List<LibraryItem>> {
    val localRepositories = repoMapping.getLocalRepositories()
    return targetsToImport
      .filter { it.javaCommon.generatedJarsList.isNotEmpty() }
      .associate { targetInfo ->
        targetInfo.key.label to
          createLibrary(
            id = Label.parse(targetInfo.key.label + "_generated"),
            dependencies = emptyList(),
            jars = targetInfo.javaCommon.generatedJarsList
              .flatMap { it.binaryJarsList }
              .map { bazelPathsResolver.resolve(it, localRepositories) }
              .toSet(),
            sourceJars = targetInfo.javaCommon.generatedJarsList
              .flatMap { it.sourceJarsList }
              .map { bazelPathsResolver.resolve(it, localRepositories) }
              .toSet(),
          )
      }.map { Label.parse(it.key) to listOf(it.value) }
      .toMap()
  }

  private suspend fun calculateKotlinStdlibsMapper(targetsToImport: List<TargetInfo>, repoMapping: RepoMapping): Map<Label, List<LibraryItem>> {
    val projectLevelKotlinStdlibsLibrary = calculateProjectLevelKotlinStdlibsLibrary(targetsToImport, repoMapping)
    val kotlinTargetsIds = targetsToImport.filter { it.hasKotlinTargetInfo() }.map { it.label() }

    return projectLevelKotlinStdlibsLibrary
      ?.let { stdlibsLibrary -> kotlinTargetsIds.associateWith { listOf(stdlibsLibrary) } }
      .orEmpty()
  }

  private suspend fun calculateProjectLevelKotlinStdlibsLibrary(targetsToImport: List<TargetInfo>, repoMapping: RepoMapping): LibraryItem? {
    val kotlinStdlibsJars = calculateProjectLevelKotlinStdlibsJars(targetsToImport, repoMapping)

    // rules_kotlin does not expose source jars for jvm stdlibs, so this is the way they can be retrieved for now
    val inferredSourceJars =
      kotlinStdlibsJars
        .map { it.parent.resolve(it.fileName.toString().replace(".jar", "-sources.jar")) }
        .filter { it.exists() }
        .onEach { if (it.notExists()) logNonExistingFile(it, "[kotlin stdlib]") }
        .toSet()

    return if (kotlinStdlibsJars.isNotEmpty()) {
      createLibrary(
        id = Label.synthetic("rules_kotlin_kotlin-stdlibs"),
        dependencies = emptyList(),
        jars = kotlinStdlibsJars,
        sourceJars = inferredSourceJars,
      )
    } else {
      null
    }
  }

  private fun calculateProjectLevelKotlinStdlibsJars(targetsToImport: List<TargetInfo>, repoMapping: RepoMapping): Set<Path> {
    val localRepositories = repoMapping.getLocalRepositories()
    return targetsToImport
      .filter { it.hasKotlinTargetInfo() }
      .map { it.kotlinTargetInfo.stdlibsList }
      .flatMap { it.resolvePaths(localRepositories) }
      .toSet()
  }

  // TODO: refactor to language-specific logic
  private suspend fun calculateScalaLibrariesMapper(targetsToImport: List<TargetInfo>): Map<Label, List<LibraryItem>> {
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

  private suspend fun calculateProjectLevelScalaSdkLibraries(): Map<Path, LibraryItem> =
    getProjectLevelScalaSdkLibrariesJars().associateWith {
      createLibrary(
        id = Label.synthetic(it.name),
        dependencies = emptyList(),
        jars = setOf(it),
        sourceJars = emptySet(),
      )
    }

  // TODO: refactor to language-specific logic
  private suspend fun calculateProjectLevelScalaTestLibraries(): Map<Path, LibraryItem> {
    val scalaPlugin = languagePluginsService.getLanguagePlugin<ScalaLanguagePlugin>(LanguageClass.SCALA)
    return scalaPlugin.scalaTestJars.values
      .flatten()
      .toSet()
      .associateWith {
        createLibrary(
          id = Label.synthetic(it.name),
          dependencies = emptyList(),
          jars = setOf(it),
          sourceJars = emptySet(),
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
    libraryDependencies: Map<Label, List<LibraryItem>>,
    librariesToImport: Map<Label, LibraryItem>,
    interfacesAndBinariesFromTargetsToImport: Map<Label, Set<Path>>,
    repoMapping: RepoMapping,
    targetKinds: Map<Label, TargetKind>,
  ): Map<Label, List<LibraryItem>> {
    val localRepositories = repoMapping.getLocalRepositories()
    val targetsToJdepsJars =
      getAllJdepsDependencies(targetsToImport, libraryDependencies, librariesToImport, localRepositories, targetKinds)
    val libraryNameToLibraryValueMap = HashMap<Label, LibraryItem>()
    return targetsToJdepsJars.mapValues { target ->
      val interfacesAndBinariesFromTarget =
        interfacesAndBinariesFromTargetsToImport.getOrDefault(target.key, emptySet())
      target.value
        .map { path -> bazelPathsResolver.resolve(path) }
        .filter { it !in interfacesAndBinariesFromTarget }
        .mapNotNull {
          if (shouldSkipJdepsJar(it)) return@mapNotNull null
          val label = syntheticLabel(it)
          libraryNameToLibraryValueMap.getOrPut(label) {
            createLibrary(
              id = label,
              dependencies = emptyList(),
              jars = setOf(it),
              sourceJars = emptySet(),
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
    libraryDependencies: Map<Label, List<LibraryItem>>,
    librariesToImport: Map<Label, LibraryItem>,
    localRepositories: LocalRepositoryMapping,
    targetKinds: Map<Label, TargetKind>,
  ): Map<Label, Set<Path>> {
    val jdepsJars =
      withContext(Dispatchers.IO) {
        targetsToImport
          .filter { targetSupportsJdeps(targetKinds.getValue(it.key)) }
          .map { (label, target) ->
            async {
              label to dependencyJarsFromJdepsFiles(target, localRepositories)
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
                localRepositories,
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
    libraryDependencies: Map<Label, List<LibraryItem>>,
    librariesToImport: Map<Label, LibraryItem>,
    outputJarsFromTransitiveDepsCache: ConcurrentHashMap<Label, Set<Path>>,
    allJdepsJars: Set<Path>,
    localRepositories : LocalRepositoryMapping,
  ): Set<Path> =
    outputJarsFromTransitiveDepsCache.getOrPut(targetOrLibrary) {
      val jarsFromTargets =
        targetsToImport[targetOrLibrary]?.let { getTargetOutputJarsSet(it, localRepositories) + getTargetInterfaceJarsSet(it, localRepositories) }.orEmpty()
      val jarsFromLibraries =
        librariesToImport[targetOrLibrary]?.let { it.jars + it.ijars }.orEmpty()
      val outputJars =
        listOfNotNull(jarsFromTargets, jarsFromLibraries)
          .asSequence()
          .flatten()
          .filter { it in allJdepsJars }
          .toMutableSet()

      val dependencies =
        targetsToImport[targetOrLibrary]?.depsList.orEmpty().map { it.label() } +
        libraryDependencies[targetOrLibrary].orEmpty().map { it.id } +
          librariesToImport[targetOrLibrary]?.dependencies.orEmpty().map { it.label }

      dependencies.flatMapTo(outputJars) { dependency ->
        getJdepsJarsFromTransitiveDependencies(
          dependency,
          targetsToImport,
          libraryDependencies,
          librariesToImport,
          outputJarsFromTransitiveDepsCache,
          allJdepsJars,
          localRepositories,
        )
      }
      outputJars
    }

  private fun dependencyJarsFromJdepsFiles(targetInfo: TargetInfo, localRepositories : LocalRepositoryMapping): Set<Path> =
    targetInfo.javaCommon.jdepsList
      .flatMap { jdeps ->
        val path = bazelPathsResolver.resolve(jdeps, localRepositories)
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

  private fun targetSupportsJdeps(targetInfo: TargetKind): Boolean {
    return setOf(LanguageClass.JAVA, LanguageClass.KOTLIN, LanguageClass.SCALA).containsAll(targetInfo.languageClasses)
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
    targetKinds: Map<Label, TargetKind>,
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
          targetInfo = targetInfo,
          targetKind = targetKinds.getValue(label),
          baseDirectory = bazelPathsResolver.toDirectoryPath(label.assumeResolved(), repoMapping),
        )
      }
  }

  private suspend fun createLibraries(
    workspaceContext: WorkspaceContext,
    targets: Map<Label, TargetInfo>,
    repoMapping: RepoMapping,
  ): Map<Label, LibraryItem> {
    val localRepositories = repoMapping.getLocalRepositories()
    return withContext(Dispatchers.Default) {
      targets
        .map { (targetId, targetInfo) ->
          async {
            createLibrary(
              workspaceContext = workspaceContext,
              label = targetId,
              targetInfo = targetInfo,
              onlyOutputJars = false,
              localRepositories
            )?.let { library ->
              targetId to library
            }
          }
        }.awaitAll()
        .filterNotNull()
        .toMap()
    }
  }

  private fun TargetInfo.containsAnyInternalJars(localRepositories : LocalRepositoryMapping) = javaCommon.jarsList.any { jars ->
    jars.sourceJarsList.any { !bazelPathsResolver.isExternal(it, localRepositories) } && jars.binaryJarsList.any { !bazelPathsResolver.isExternal(it, localRepositories) }
  }

  private suspend fun createLibrary(
    workspaceContext: WorkspaceContext,
    label: Label,
    targetInfo: TargetInfo,
    onlyOutputJars: Boolean,
    localRepositories : LocalRepositoryMapping,
  ): LibraryItem? {
    val outputs = getTargetOutputJarPaths(targetInfo, localRepositories) + getIntellijPluginJars(targetInfo, localRepositories)
    val rawSources = getSourceJarPaths(targetInfo, localRepositories)
    val sources = if (workspaceContext.preferClassJarsOverSourcelessJars) {
      rawSources - outputs
    } else {
      rawSources
    }

    val interfaceJars = getTargetInterfaceJarsSet(targetInfo, localRepositories).toSet()
    val dependencies: List<BspTargetInfo.Dependency> = if (!onlyOutputJars) targetInfo.depsList else emptyList()
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

    return createLibrary(
      id = label,
      dependencies = targetInfo.depsList.map { it.toDependencyLabel() },
      ijars = interfaceJars,
      jars = outputs,
      sourceJars = sources,
      mavenCoordinates = mavenCoordinates,
      containsInternalJars = targetInfo.containsAnyInternalJars(localRepositories),
    )
  }

  private suspend fun createLibrary(
    id: Label,
    dependencies: List<DependencyLabel>,
    ijars: Set<Path> = emptySet(),
    jars: Set<Path>,
    sourceJars: Set<Path>,
    mavenCoordinates: MavenCoordinates? = null,
    containsInternalJars: Boolean = false,
  ): LibraryItem {
    val outputFileHardLinks = BazelOutputFileHardLinks.getInstance(project)
    return LibraryItem(
      id = id,
      dependencies = dependencies,
      ijars = outputFileHardLinks.createOutputFileHardLinks(ijars),
      jars = outputFileHardLinks.createOutputFileHardLinks(jars),
      sourceJars = outputFileHardLinks.createOutputFileHardLinks(sourceJars),
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

  private fun List<ArtifactLocation>.resolvePaths(localRepositories : LocalRepositoryMapping) = map { bazelPathsResolver.resolve(it, localRepositories) }.toSet()

  private fun getTargetOutputJarPaths(targetInfo: TargetInfo, localRepositories : LocalRepositoryMapping) =
    getTargetOutputJarsList(targetInfo, localRepositories)
      .toSet()

  private fun getIntellijPluginJars(targetInfo: TargetInfo, localRepositories : LocalRepositoryMapping): Set<Path> {
    // _repackaged_files is created upon calling repackaged_files in rules_intellij
    if (targetInfo.kind != "_repackaged_files") return emptySet()
    return targetInfo.generatedSourcesList.toList()
      .resolvePaths(localRepositories)
      .filter { it.extension == "jar" }
      .toSet()
  }

  private fun getSourceJarPaths(targetInfo: TargetInfo, localRepositories : LocalRepositoryMapping) =
    targetInfo.javaCommon.jarsList
      .flatMap { it.sourceJarsList }
      .resolvePaths(localRepositories)

  private fun getTargetOutputJarsSet(targetInfo: TargetInfo, localRepositories : LocalRepositoryMapping) = getTargetOutputJarsList(targetInfo, localRepositories).toSet()

  private fun getTargetOutputJarsList(targetInfo: TargetInfo, localRepositories: LocalRepositoryMapping) = targetInfo.javaCommon
    .jarsList
    .flatMap { it.binaryJarsList }
    .map { bazelPathsResolver.resolve(it, localRepositories) }

  private fun getTargetInterfaceJarsSet(targetInfo: TargetInfo, localRepositories : LocalRepositoryMapping) = getTargetInterfaceJarsList(targetInfo, localRepositories).toSet()

  private fun getTargetInterfaceJarsList(targetInfo: TargetInfo, localRepositories : LocalRepositoryMapping) =
    targetInfo.javaCommon.jarsList
      .flatMap { it.interfaceJarsList }
      .map { bazelPathsResolver.resolve(it, localRepositories) }

  private fun collectInterfacesAndClasses(targets: List<TargetInfo>, repoMapping: RepoMapping): Map<Label, Set<Path>> {
    val localRepositories = repoMapping.getLocalRepositories()
    return targets.associate { target ->
        target.label() to
          (getTargetInterfaceJarsList(target, localRepositories) + getTargetOutputJarsList(target, localRepositories))
            .toSet()
      }
  }

  private fun hasKnownJvmSources(targetInfo: TargetInfo) =
    targetInfo.sourcesList.any {
      it.relativePath.endsWith(".java") ||
        it.relativePath.endsWith(".kt") ||
        it.relativePath.endsWith(".scala")
    }

  private fun hasKnownPythonSources(targetInfo: TargetInfo,
                                    workspaceContext: WorkspaceContext) =
    targetInfo.sourcesList.any {
      it.relativePath.endsWith(".py")
    } || targetInfo.pythonTargetInfo?.generatedSourcesList?.isNotEmpty() == true

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
    workspaceContext: WorkspaceContext,
  ): Boolean =
    (
      isTargetTreatedAsInternal(target.label().assumeResolved(), repoMapping) &&
        (
          shouldImportTargetKind(target.kind) ||
            target.javaCommon.jvmTarget &&
            (
              target.depsCount > 0 ||
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
          hasKnownPythonSources(target, workspaceContext)
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
      "_jvm_library_jps",
      "jvm_resources",
      "scala_library",
      "scala_binary",
      "scala_test",
      "intellij_plugin_debug_target",
      "go_proto_library",
      "go_library",
      "go_binary",
      "go_test",
      "py_proto_library",
      "py_library",
      "py_binary",
      "py_test",
    )

  // TODO BAZEL-2208
  // The only language that supports strict deps by default is Java, in Kotlin and Scala strict deps are disabled by default.
  private fun targetSupportsStrictDeps(target: TargetInfo): Boolean =
    target.javaCommon.jvmTarget && !target.hasScalaTargetInfo() && !target.hasKotlinTargetInfo()

  private suspend fun createRawBuildTargets(
    targets: List<IntermediateTargetData>,
    repoMapping: RepoMapping,
    dependencyGraph: DependencyGraph,
  ):List<RawBuildTarget> {
    val localRepositories = repoMapping.getLocalRepositories()
    return withContext(Dispatchers.Default) {
      val tasks =
        targets.map { target ->
          async {
            createRawBuildTarget(
              target,
              repoMapping,
              dependencyGraph,
              localRepositories,
            )
          }
        }

      return@withContext tasks
        .awaitAll()
        .filterNotNull()
    }
  }

  private suspend fun createRawBuildTarget(
    targetData: IntermediateTargetData,
    repoMapping: RepoMapping,
    dependencyGraph: DependencyGraph,
    localRepositories : LocalRepositoryMapping,
  ): RawBuildTarget? {
    val target = targetData.target
    val label = targetData.label
    val resolvedDependencies = resolveDirectDependencies(target)
    // https://youtrack.jetbrains.com/issue/BAZEL-983: extra libraries can override some library versions, so they should be put before
    val directDependencies =
      targetData.extraLibraries.map { DependencyLabel(it.id) } +
      resolvedDependencies +
      targetData.librariesFromToolchains.map { DependencyLabel(it.id) }
    val baseDirectory = bazelPathsResolver.toDirectoryPath(label, repoMapping)
    val languagePlugin = languagePluginsService.getLanguagePlugin(targetData.targetKind.languageClasses) ?: return null
    val resources = resolveResources(target, languagePlugin, localRepositories)

    val targetSources = targetData.sources

    val context = LanguagePluginContext(target, dependencyGraph, repoMapping, targetSources, bazelPathsResolver)
    val data = languagePlugin.createBuildTargetData(context, target, repoMapping)

    return RawBuildTarget(
      id = label,
      dependencies = directDependencies,
      kind = targetData.targetKind,
      sources = languagePlugin.transformSources(targetSources),
      resources = resources,
      baseDirectory = baseDirectory,
      data = data,
      generatorName = targetData.generatorName,
      isManual = BuildTargetTag.MANUAL in targetData.tags,
    )
  }

  private fun resolveDirectDependencies(target: TargetInfo): List<DependencyLabel> =
    target.depsList.map { it.toDependencyLabel() }

  private fun BspTargetInfo.Dependency.toDependencyLabel(): DependencyLabel =
    DependencyLabel(
      label = label(),
      isRuntime = dependencyType == BspTargetInfo.Dependency.DependencyType.RUNTIME,
      exported = exported,
    )

  private fun resolveSourceSet(target: TargetInfo, languagePlugin: LanguagePlugin<*>, repoMapping: RepoMapping): Sequence<SourceItem> {
    val localRepositories = repoMapping.getLocalRepositories()
    val sources =
      target.sourcesList
        .asSequence()
        .map {bazelPathsResolver.resolve(it, localRepositories)}

    val generatedSources =
      target.generatedSourcesList
        .asSequence()
        .map {bazelPathsResolver.resolve(it, localRepositories)}
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

    val extraSources = languagePlugin.calculateAdditionalSources(target, repoMapping)

    return (sourceItems + generatedSourceItems + extraSources)
      .distinctBy { it.path }
      .onEach { if (it.path.notExists()) logNonExistingFile(it.path, target.key.label) }
  }

  private fun logNonExistingFile(file: Path, targetId: String) {
    val message = "target $targetId: $file does not exist."
    logger.warn(message)
  }

  private fun resolveResources(target: TargetInfo, languagePlugin: LanguagePlugin<*>, localRepositories : LocalRepositoryMapping) : List<Path> {
    val resources = bazelPathsResolver.resolvePaths(target.jvmTargetInfo.resourcesList, localRepositories)
    val extraResources = languagePlugin.resolveAdditionalResources(target)
    return (resources.asSequence() + extraResources)
      .distinct()
      .toList()
  }

  private fun NonModuleTarget.toBuildTarget(): RawBuildTarget {
    return RawBuildTarget(
      id = label,
      kind = targetKind,
      baseDirectory = baseDirectory,
      dependencies = emptyList(),
      sources = emptyList(),
      resources = emptyList(),
      data = null,
      generatorName = targetInfo.generatorName.takeIf { it.isNotEmpty() },
      isManual = BuildTargetTag.MANUAL in targetInfo.tagsList,
    )
  }

  private data class NonModuleTarget(
    val label: Label,
    val targetInfo: TargetInfo,
    val targetKind: TargetKind,
    val baseDirectory: Path,
  )
}
