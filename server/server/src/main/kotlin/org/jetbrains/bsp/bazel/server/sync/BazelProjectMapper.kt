package org.jetbrains.bsp.bazel.server.sync

import com.google.common.hash.Hashing
import com.google.devtools.build.lib.view.proto.Deps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.commons.label.Label
import org.jetbrains.bazel.commons.label.ResolvedLabel
import org.jetbrains.bazel.commons.label.assumeResolved
import org.jetbrains.bsp.bazel.bazelrunner.utils.BazelInfo
import org.jetbrains.bsp.bazel.info.BspTargetInfo
import org.jetbrains.bsp.bazel.info.BspTargetInfo.FileLocation
import org.jetbrains.bsp.bazel.info.BspTargetInfo.TargetInfo
import org.jetbrains.bsp.bazel.logger.BspClientLogger
import org.jetbrains.bsp.bazel.server.benchmark.tracer
import org.jetbrains.bsp.bazel.server.benchmark.useWithScope
import org.jetbrains.bsp.bazel.server.bzlmod.BzlmodRepoMapping
import org.jetbrains.bsp.bazel.server.bzlmod.RepoMapping
import org.jetbrains.bsp.bazel.server.bzlmod.RepoMappingDisabled
import org.jetbrains.bsp.bazel.server.dependencygraph.DependencyGraph
import org.jetbrains.bsp.bazel.server.label.label
import org.jetbrains.bsp.bazel.server.model.AspectSyncProject
import org.jetbrains.bsp.bazel.server.model.GoLibrary
import org.jetbrains.bsp.bazel.server.model.Language
import org.jetbrains.bsp.bazel.server.model.Library
import org.jetbrains.bsp.bazel.server.model.Module
import org.jetbrains.bsp.bazel.server.model.NonModuleTarget
import org.jetbrains.bsp.bazel.server.model.SourceSet
import org.jetbrains.bsp.bazel.server.model.SourceWithData
import org.jetbrains.bsp.bazel.server.model.Tag
import org.jetbrains.bsp.bazel.server.paths.BazelPathsResolver
import org.jetbrains.bsp.bazel.server.sync.languages.LanguagePlugin
import org.jetbrains.bsp.bazel.server.sync.languages.LanguagePluginsService
import org.jetbrains.bsp.bazel.server.sync.languages.android.KotlinAndroidModulesMerger
import org.jetbrains.bsp.bazel.server.sync.languages.rust.RustModule
import org.jetbrains.bsp.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.FeatureFlags
import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.inputStream
import kotlin.io.path.name
import kotlin.io.path.notExists
import kotlin.io.path.toPath

class BazelProjectMapper(
  private val languagePluginsService: LanguagePluginsService,
  private val bazelPathsResolver: BazelPathsResolver,
  private val targetTagsResolver: TargetTagsResolver,
  private val mavenCoordinatesResolver: MavenCoordinatesResolver,
  private val kotlinAndroidModulesMerger: KotlinAndroidModulesMerger,
  private val bspClientLogger: BspClientLogger,
  private val featureFlags: FeatureFlags,
) {
  private suspend fun <T> measure(description: String, body: suspend () -> T): T = tracer.spanBuilder(description).useWithScope { body() }

  private suspend fun <T> measureIf(
    description: String,
    predicate: () -> Boolean,
    ifFalse: T,
    body: suspend () -> T,
  ): T =
    if (predicate()) {
      measure(description, body)
    } else {
      ifFalse
    }

  suspend fun createProject(
    targets: Map<Label, TargetInfo>,
    rootTargets: Set<Label>,
    workspaceContext: WorkspaceContext,
    bazelInfo: BazelInfo,
    repoMapping: RepoMapping,
  ): AspectSyncProject {
    languagePluginsService.prepareSync(targets.values.asSequence())
    val dependencyGraph =
      measure("Build dependency tree") {
        DependencyGraph(rootTargets, targets)
      }
    val targetsToImport =
      measure("Select targets") {
        selectTargetsToImport(workspaceContext, rootTargets, dependencyGraph, repoMapping)
      }
    val interfacesAndBinariesFromTargetsToImport =
      measure("Collect interfaces and classes from targets to import") {
        collectInterfacesAndClasses(targetsToImport)
      }
    val targetsAsLibraries =
      measure("Targets as libraries") {
        val libraries = targets - targetsToImport.map { it.label() }.toSet()
        val usedLibraries = dependencyGraph.filterUsedLibraries(libraries, targetsToImport)
        usedLibraries
      }
    val outputJarsLibraries =
      measure("Create output jars libraries") {
        calculateOutputJarsLibraries(targetsToImport)
      }
    val annotationProcessorLibraries =
      measure("Create AP libraries") {
        annotationProcessorLibraries(targetsToImport)
      }
    val kotlinStdlibsMapper =
      measure("Create kotlin stdlibs") {
        calculateKotlinStdlibsMapper(targetsToImport)
      }
    val kotlincPluginLibrariesMapper =
      measure("Create kotlinc plugin libraries") {
        calculateKotlincPluginLibrariesMapper(targetsToImport)
      }
    val scalaLibrariesMapper =
      measure("Create scala libraries") {
        calculateScalaLibrariesMapper(targetsToImport)
      }
    val androidLibrariesMapper =
      measure("Create android libraries") {
        calculateAndroidLibrariesMapper(targetsToImport)
      }
    val goLibrariesMapper =
      measure("Create go libraries") {
        calculateGoLibrariesMapper(targetsToImport)
      }
    val librariesFromTransitiveCompileTimeJars =
      measure("Libraries from transitive compile-time jars") {
        createLibrariesFromTransitiveCompileTimeJars(
          targetsToImport,
          workspaceContext,
          interfacesAndBinariesFromTargetsToImport,
          targets,
        )
      }
    val librariesFromDeps =
      measure("Merge libraries from deps") {
        concatenateMaps(
          outputJarsLibraries,
          annotationProcessorLibraries,
          kotlinStdlibsMapper,
          kotlincPluginLibrariesMapper,
          scalaLibrariesMapper,
          androidLibrariesMapper,
        )
      }
    val librariesFromDepsAndTargets =
      measure("Libraries from targets and deps") {
        createLibraries(targetsAsLibraries) +
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
    val workspaceRoot = bazelPathsResolver.workspaceRoot()
    val modulesFromBazel =
      measure("Create modules") {
        createModules(
          targetsToImport,
          dependencyGraph,
          concatenateMaps(
            librariesFromDeps,
            extraLibrariesFromJdeps,
            librariesFromTransitiveCompileTimeJars,
          ),
        )
      }
    val mergedModulesFromBazel =
      measure("Merge Kotlin Android modules") {
        kotlinAndroidModulesMerger.mergeKotlinAndroidModules(modulesFromBazel)
      }
    val librariesToImport =
      measure("Merge all libraries") {
        librariesFromDepsAndTargets +
          extraLibrariesFromJdeps.values.flatten().associateBy { it.label } +
          librariesFromTransitiveCompileTimeJars.values.flatten().associateBy { it.label }
      }
    val goLibrariesToImport =
      measureIf(
        description = "Merge all Go libraries",
        predicate = { featureFlags.isGoSupportEnabled },
        ifFalse = emptyMap(),
      ) {
        goLibrariesMapper.values
          .flatten()
          .distinct()
          .associateBy { it.label } +
          createGoLibraries(targetsAsLibraries)
      }
    val invalidTargets =
      measure("Save invalid target labels") {
        removeDotBazelBspTarget(rootTargets) - targetsToImport.map { it.label() }.toSet()
      }
    val rustExternalTargetsToImport =
      measureIf(
        description = "Select external Rust targets",
        predicate = { featureFlags.isRustSupportEnabled },
        ifFalse = emptySequence(),
      ) {
        selectRustExternalTargetsToImport(rootTargets, dependencyGraph, repoMapping)
      }
    val rustExternalModules =
      measureIf(
        description = "Create Rust external modules",
        predicate = { featureFlags.isRustSupportEnabled },
        ifFalse = emptySequence(),
      ) {
        createRustExternalModules(rustExternalTargetsToImport, dependencyGraph, librariesFromDeps)
      }
    val allModules = mergedModulesFromBazel + rustExternalModules

    val nonModuleTargetIds =
      (removeDotBazelBspTarget(targets.keys) - allModules.map { it.label }.toSet() - librariesToImport.keys).toSet()
    val nonModuleTargets =
      createNonModuleTargets(
        targets.filterKeys {
          nonModuleTargetIds.contains(it) &&
            isTargetTreatedAsInternal(it.assumeResolved(), repoMapping)
        },
      )

    return AspectSyncProject(
      workspaceRoot = workspaceRoot,
      bazelRelease = bazelInfo.release,
      modules = allModules.toList(),
      libraries = librariesToImport,
      goLibraries = goLibrariesToImport,
      invalidTargets = invalidTargets,
      nonModuleTargets = nonModuleTargets,
      repoMapping = repoMapping,
    )
  }

  private fun <K, V> concatenateMaps(vararg maps: Map<K, List<V>>): Map<K, List<V>> =
    maps
      .flatMap { it.keys }
      .distinct()
      .associateWith { key ->
        maps.flatMap { it[key].orEmpty() }
      }

  private fun calculateOutputJarsLibraries(targetsToImport: Sequence<TargetInfo>): Map<Label, List<Library>> =
    targetsToImport
      .filter { shouldCreateOutputJarsLibrary(it) }
      .mapNotNull { target ->
        createLibrary(Label.parse(target.id + "_output_jars"), target, onlyOutputJars = true)?.let { library ->
          target.label() to listOf(library)
        }
      }.toMap()

  private fun shouldCreateOutputJarsLibrary(targetInfo: TargetInfo) =
    targetInfo.generatedSourcesList.any { it.relativePath.endsWith(".srcjar") } ||
      targetInfo.hasJvmTargetInfo() &&
      !hasKnownJvmSources(targetInfo)

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
                .map { bazelPathsResolver.resolveUri(it) }
                .toSet(),
            sources =
              targetInfo.jvmTargetInfo.generatedJarsList
                .flatMap { it.sourceJarsList }
                .map { bazelPathsResolver.resolveUri(it) }
                .toSet(),
            dependencies = emptyList(),
            interfaceJars = emptySet(),
          )
      }.map { Label.parse(it.key) to listOf(it.value) }
      .toMap()

  private fun calculateKotlinStdlibsMapper(targetsToImport: Sequence<TargetInfo>): Map<Label, List<Library>> {
    val projectLevelKotlinStdlibsLibrary = calculateProjectLevelKotlinStdlibsLibrary(targetsToImport)
    val kotlinTargetsIds = targetsToImport.filter { it.hasKotlinTargetInfo() }.map { it.label() }

    return projectLevelKotlinStdlibsLibrary
      ?.let { stdlibsLibrary -> kotlinTargetsIds.associateWith { listOf(stdlibsLibrary) } }
      .orEmpty()
  }

  private fun calculateProjectLevelKotlinStdlibsLibrary(targetsToImport: Sequence<TargetInfo>): Library? {
    val kotlinStdlibsJars = calculateProjectLevelKotlinStdlibsJars(targetsToImport)

    // rules_kotlin does not expose source jars for jvm stdlibs, so this is the way they can be retrieved for now
    val inferredSourceJars =
      kotlinStdlibsJars
        .map { it.toPath() }
        .map { it.parent.resolve(it.fileName.toString().replace(".jar", "-sources.jar")) }
        .filter { it.exists() }
        .map { it.toUri() }
        .toSet()

    return if (kotlinStdlibsJars.isNotEmpty()) {
      Library(
        label = Label.synthetic("rules_kotlin_kotlin-stdlibs"),
        outputs = kotlinStdlibsJars,
        sources = inferredSourceJars,
        dependencies = emptyList(),
      )
    } else {
      null
    }
  }

  private fun calculateProjectLevelKotlinStdlibsJars(targetsToImport: Sequence<TargetInfo>): Set<URI> =
    targetsToImport
      .filter { it.hasKotlinTargetInfo() }
      .map { it.kotlinTargetInfo.stdlibsList }
      .flatMap { it.resolveUris() }
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
              outputs = setOf(classpath.toUri()),
              sources = emptySet(),
              dependencies = emptyList(),
            )
          },
        )
      }

  private fun calculateScalaLibrariesMapper(targetsToImport: Sequence<TargetInfo>): Map<Label, List<Library>> {
    val projectLevelScalaSdkLibraries = calculateProjectLevelScalaSdkLibraries()
    val projectLevelScalaTestLibraries = calculateProjectLevelScalaTestLibraries()
    val scalaTargets = targetsToImport.filter { it.hasScalaTargetInfo() }.map { it.label() }
    return scalaTargets.associateWith {
      val sdkLibraries =
        languagePluginsService.scalaLanguagePlugin.scalaSdks[it]
          ?.compilerJars
          ?.mapNotNull {
            projectLevelScalaSdkLibraries[it]
          }.orEmpty()
      val testLibraries =
        languagePluginsService.scalaLanguagePlugin.scalaTestJars[it]
          ?.mapNotNull {
            projectLevelScalaTestLibraries[it]
          }.orEmpty()

      (sdkLibraries + testLibraries).distinct()
    }
  }

  private fun calculateProjectLevelScalaSdkLibraries(): Map<URI, Library> =
    getProjectLevelScalaSdkLibrariesJars().associateWith {
      Library(
        label = Label.synthetic(Paths.get(it).name),
        outputs = setOf(it),
        sources = emptySet(),
        dependencies = emptyList(),
      )
    }

  private fun calculateProjectLevelScalaTestLibraries(): Map<URI, Library> =
    languagePluginsService.scalaLanguagePlugin.scalaTestJars.values
      .flatten()
      .toSet()
      .associateWith {
        Library(
          label = Label.synthetic(Paths.get(it).name),
          outputs = setOf(it),
          sources = emptySet(),
          dependencies = emptyList(),
        )
      }

  private fun getProjectLevelScalaSdkLibrariesJars(): Set<URI> =
    languagePluginsService.scalaLanguagePlugin.scalaSdks.values
      .toSet()
      .flatMap {
        it.compilerJars
      }.toSet()

  private fun calculateAndroidLibrariesMapper(targetsToImport: Sequence<TargetInfo>): Map<Label, List<Library>> =
    targetsToImport
      .mapNotNull { target ->
        val aidlLibrary = createAidlLibrary(target) ?: return@mapNotNull null
        target.label() to listOf(aidlLibrary)
      }.toMap()

  private fun createAidlLibrary(target: TargetInfo): Library? {
    if (!target.hasAndroidTargetInfo()) return null
    val androidTargetInfo = target.androidTargetInfo
    if (!androidTargetInfo.hasAidlBinaryJar()) return null

    val libraryLabel = Label.parse(target.id + "_aidl")
    if (target.sourcesList.isEmpty()) {
      // Bazel doesn't create the AIDL jar if there's no sources, since it'd be the same as the output jar
      return null
    }

    val outputs = listOf(target.androidTargetInfo.aidlBinaryJar).resolveUris()
    val sources =
      if (target.androidTargetInfo.hasAidlSourceJar()) {
        listOf(target.androidTargetInfo.aidlSourceJar).resolveUris()
      } else {
        emptySet()
      }
    return Library(
      label = libraryLabel,
      outputs = outputs,
      sources = sources,
      dependencies = emptyList(),
      interfaceJars = emptySet(),
    )
  }

  private fun calculateGoLibrariesMapper(targetsToImport: Sequence<TargetInfo>): Map<Label, List<GoLibrary>> =
    targetsToImport
      .mapNotNull { target ->
        if (!target.hasGoTargetInfo()) return@mapNotNull null
        val label = target.label()
        val libraries =
          target.goTargetInfo.generatedLibrariesList.map {
            GoLibrary(
              label = label,
              goImportPath = target.goTargetInfo.importpath,
              goRoot = bazelPathsResolver.resolve(it).parent.toUri(),
            )
          }
        label to libraries
      }.toMap()

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
    interfacesAndBinariesFromTargetsToImport: Map<Label, Set<URI>>,
  ): Map<Label, List<Library>> {
    val targetsToJdepsJars = getAllJdepsDependencies(targetsToImport, libraryDependencies, librariesToImport)
    val libraryNameToLibraryValueMap = HashMap<Label, Library>()
    return targetsToJdepsJars.mapValues { target ->
      val interfacesAndBinariesFromTarget =
        interfacesAndBinariesFromTargetsToImport.getOrDefault(target.key, emptySet())
      target.value
        .map { path -> bazelPathsResolver.resolveUri(path) }
        .filter { uri -> uri !in interfacesAndBinariesFromTarget }
        .map { uri ->
          val label = syntheticLabel(uri)
          libraryNameToLibraryValueMap.computeIfAbsent(label) { _ ->
            Library(
              label = label,
              dependencies = emptyList(),
              interfaceJars = emptySet(),
              outputs = setOf(uri),
              sources = emptySet(),
            )
          }
        }
    }
  }

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
        librariesToImport[targetOrLibrary]?.let { it.outputs + it.interfaceJars }.orEmpty().map { Paths.get(it) }
      val outputJars =
        listOfNotNull(jarsFromTargets, jarsFromLibraries)
          .asSequence()
          .flatten()
          .filter { it in allJdepsJars }
          .toMutableSet()

      val dependencies =
        targetsToImport[targetOrLibrary]?.dependenciesList.orEmpty().map { it.label() } +
          libraryDependencies[targetOrLibrary].orEmpty().map { it.label } +
          librariesToImport[targetOrLibrary]?.dependencies.orEmpty()

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
    return setOf(Language.JAVA, Language.KOTLIN, Language.SCALA, Language.ANDROID).containsAll(languages)
  }

  private val replacementRegex = "[^0-9a-zA-Z]".toRegex()

  private fun syntheticLabel(lib: URI): Label {
    val shaOfPath =
      Hashing
        .sha256()
        .hashString(lib.toString(), StandardCharsets.UTF_8)
        .toString()
        .take(7) // just in case of a conflict in filename
    return Label.synthetic(
      Paths
        .get(lib)
        .fileName
        .toString()
        .replace(replacementRegex, "-") + "-" + shaOfPath,
    )
  }

  private fun createNonModuleTargets(targets: Map<Label, TargetInfo>): List<NonModuleTarget> =
    targets
      .map { (label, targetInfo) ->
        NonModuleTarget(
          label = label,
          languages = inferLanguages(targetInfo),
          tags = targetTagsResolver.resolveTags(targetInfo),
          baseDirectory = label.assumeResolved().toDirectoryUri(),
        )
      }

  private fun createLibraries(targets: Map<Label, TargetInfo>): Map<Label, Library> =
    targets
      .asSequence()
      .mapNotNull { (targetId, targetInfo) ->
        createLibrary(targetId, targetInfo)?.let { library ->
          targetId to library
        }
      }.toMap()

  private fun createLibrary(
    label: Label,
    targetInfo: TargetInfo,
    onlyOutputJars: Boolean = false,
  ): Library? {
    val outputs = getTargetOutputJarUris(targetInfo) + getAndroidAarUris(targetInfo) + getIntellijPluginJars(targetInfo)
    val sources = getSourceJarUris(targetInfo)
    val interfaceJars = getTargetInterfaceJarsSet(targetInfo).map { it.toUri() }.toSet()
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
    )
  }

  private fun shouldCreateLibrary(
    dependencies: List<BspTargetInfo.Dependency>,
    outputs: Collection<URI>,
    interfaceJars: Collection<URI>,
    sources: Collection<URI>,
  ): Boolean = dependencies.isNotEmpty() || !outputs.isEmptyJarList() || !interfaceJars.isEmptyJarList() || !sources.isEmptyJarList()

  private fun Collection<URI>.isEmptyJarList(): Boolean = isEmpty() || singleOrNull()?.toPath()?.name == "empty.jar"

  private fun createGoLibraries(targets: Map<Label, TargetInfo>): Map<Label, GoLibrary> =
    targets
      .mapValues { (targetId, targetInfo) ->
        createGoLibrary(targetId, targetInfo)
      }.filterValues {
        it.isGoLibrary()
      }

  private fun GoLibrary.isGoLibrary(): Boolean = !goImportPath.isNullOrEmpty() && goRoot.toString().isNotEmpty()

  private fun createGoLibrary(label: Label, targetInfo: TargetInfo): GoLibrary =
    GoLibrary(
      label = label,
      goImportPath = targetInfo.goTargetInfo?.importpath,
      goRoot = getGoRootUri(targetInfo),
    )

  private fun createLibrariesFromTransitiveCompileTimeJars(
    targetsToImport: Sequence<TargetInfo>,
    workspaceContext: WorkspaceContext,
    interfacesAndClassesFromTargetsToImport: Map<Label, Set<URI>>,
    targetsMap: Map<Label, TargetInfo>,
  ): Map<Label, List<Library>> =
    if (workspaceContext.experimentalAddTransitiveCompileTimeJars.value) {
      val explicitCompileTimeInterfaces = calculateExplicitCompileTimeInterfaces(targetsToImport, targetsMap)
      val res = HashMap<Label, Library>()
      targetsToImport.associate { targetInfo ->
        val interfacesAndBinariesFromTarget =
          interfacesAndClassesFromTargetsToImport.getOrDefault(targetInfo.label(), emptySet())
        val explicitCompileTimeInterfacesFromTarget =
          explicitCompileTimeInterfaces.getOrDefault(targetInfo.label(), emptySet())
        targetInfo.label() to
          targetInfo.jvmTargetInfo.transitiveCompileTimeJarsList
            .map { bazelPathsResolver.resolve(it).toUri() }
            .filter {
              it !in interfacesAndBinariesFromTarget &&
                it in explicitCompileTimeInterfacesFromTarget
            }.map { uri ->
              val label = syntheticLabel(uri)
              res.computeIfAbsent(label) {
                Library(
                  label = label,
                  outputs = setOf(uri),
                  sources = setOf(),
                  dependencies = listOf(),
                )
              }
            }
      }
    } else {
      emptyMap()
    }

  private fun calculateExplicitCompileTimeInterfaces(targets: Sequence<TargetInfo>, targetsMap: Map<Label, TargetInfo>) =
    targets.associate { target ->
      target.label() to
        target.dependenciesList
          .asSequence()
          .mapNotNull { targetsMap[it.label()] }
          .flatMap { getTargetInterfaceJarsList(it) }
          .map { it.toUri() }
          .toSet()
    }

  private fun List<FileLocation>.resolveUris() = map { bazelPathsResolver.resolve(it).toUri() }.toSet()

  private fun getTargetOutputJarUris(targetInfo: TargetInfo) =
    getTargetOutputJarsList(targetInfo)
      .map { it.toUri() }
      .toSet()

  private fun getAndroidAarUris(targetInfo: TargetInfo): Set<URI> {
    if (!targetInfo.hasAndroidAarImportInfo()) return emptySet()
    val androidAarImportInfo = targetInfo.androidAarImportInfo

    val result = mutableSetOf<URI>()
    result += bazelPathsResolver.resolve(androidAarImportInfo.manifest).toUri()
    if (androidAarImportInfo.hasResourceFolder()) {
      result += bazelPathsResolver.resolve(androidAarImportInfo.resourceFolder).resolve("res").toUri()
    }
    if (androidAarImportInfo.hasRTxt()) {
      result += bazelPathsResolver.resolve(targetInfo.androidAarImportInfo.rTxt).toUri()
    }
    return result
  }

  private fun getIntellijPluginJars(targetInfo: TargetInfo): Set<URI> {
    // _repackaged_files is created upon calling repackaged_files in rules_intellij
    if (targetInfo.kind != "_repackaged_files") return emptySet()
    return targetInfo.generatedSourcesList
      .resolveUris()
      .filter { it.path.endsWith(".jar") }
      .toSet()
  }

  private fun getSourceJarUris(targetInfo: TargetInfo) =
    targetInfo.jvmTargetInfo.jarsList
      .flatMap { it.sourceJarsList }
      .resolveUris()

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

  private fun getGoRootUri(targetInfo: TargetInfo): URI = targetInfo.label().assumeResolved().toDirectoryUri()

  private fun selectRustExternalTargetsToImport(
    rootTargets: Set<Label>,
    graph: DependencyGraph,
    repoMapping: RepoMapping,
  ): Sequence<TargetInfo> =
    graph
      .allTargetsAtDepth(-1, rootTargets)
      .asSequence()
      .filter { !isWorkspaceTarget(it, repoMapping) && isRustTarget(it) }

  private fun selectTargetsToImport(
    workspaceContext: WorkspaceContext,
    rootTargets: Set<Label>,
    graph: DependencyGraph,
    repoMapping: RepoMapping,
  ): Sequence<TargetInfo> =
    graph
      .allTargetsAtDepth(
        workspaceContext.importDepth.value,
        rootTargets,
      ).filter { isWorkspaceTarget(it, repoMapping) }
      .asSequence()

  private fun collectInterfacesAndClasses(targets: Sequence<TargetInfo>) =
    targets
      .associate { target ->
        target.label() to
          (getTargetInterfaceJarsList(target) + getTargetOutputJarsList(target))
            .map { it.toUri() }
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
    }

  private fun hasKnownGoSources(targetInfo: TargetInfo) =
    targetInfo.sourcesList.any {
      it.relativePath.endsWith(".go")
    }

  private fun hasKnownRustSources(targetInfo: TargetInfo) =
    targetInfo.sourcesList.any {
      it.relativePath.endsWith(".rs")
    }

  private fun externalRepositoriesTreatedAsInternal(repoMapping: RepoMapping) =
    when (repoMapping) {
      is BzlmodRepoMapping -> repoMapping.canonicalRepoNameToLocalPath.keys
      is RepoMappingDisabled -> emptySet()
    }

  private fun isTargetTreatedAsInternal(target: ResolvedLabel, repoMapping: RepoMapping): Boolean =
    target.isMainWorkspace || target.repo.repoName in externalRepositoriesTreatedAsInternal(repoMapping)

  // TODO https://youtrack.jetbrains.com/issue/BAZEL-1303
  private fun isWorkspaceTarget(target: TargetInfo, repoMapping: RepoMapping): Boolean =
    isTargetTreatedAsInternal(target.label().assumeResolved(), repoMapping) &&
      (
        shouldImportTargetKind(target.kind) ||
          target.hasJvmTargetInfo() &&
          hasKnownJvmSources(target) ||
          featureFlags.isPythonSupportEnabled &&
          target.hasPythonTargetInfo() &&
          hasKnownPythonSources(target) ||
          featureFlags.isGoSupportEnabled &&
          target.hasGoTargetInfo() &&
          hasKnownGoSources(target) ||
          featureFlags.isRustSupportEnabled &&
          target.hasRustCrateInfo() &&
          hasKnownRustSources(target)
      )

  private fun shouldImportTargetKind(kind: String): Boolean = kind in workspaceTargetKinds

  private val workspaceTargetKinds =
    setOf(
      "java_library",
      "java_binary",
      "java_test",
      "kt_jvm_library",
      "kt_jvm_binary",
      "kt_jvm_test",
      "scala_library",
      "scala_binary",
      "scala_test",
      "rust_test",
      "rust_doc",
      "rust_doc_test",
      "android_library",
      "android_binary",
      "android_local_test",
      "kt_android_library",
      "kt_android_local_test",
      "intellij_plugin_debug_target",
      "go_library",
      "go_binary",
      "go_test",
    )

  private fun isRustTarget(target: TargetInfo): Boolean = target.hasRustCrateInfo()

  private suspend fun createModules(
    targetsToImport: Sequence<TargetInfo>,
    dependencyGraph: DependencyGraph,
    generatedLibraries: Map<Label, Collection<Library>>,
  ): List<Module> =
    withContext(Dispatchers.Default) {
      targetsToImport
        .toList()
        .map {
          async {
            createModule(
              it,
              dependencyGraph,
              generatedLibraries[it.label()].orEmpty(),
            )
          }
        }.awaitAll()
        .filterNot { it.tags.contains(Tag.NO_IDE) }
    }

  private fun createModule(
    target: TargetInfo,
    dependencyGraph: DependencyGraph,
    extraLibraries: Collection<Library>,
  ): Module {
    val label = target.label().assumeResolved()
    // extra libraries can override some library versions, so they should be put before
    val directDependencies = extraLibraries.map { it.label } + resolveDirectDependencies(target)
    val languages = inferLanguages(target)
    val tags = targetTagsResolver.resolveTags(target)
    val baseDirectory = label.toDirectoryUri()
    val languagePlugin = languagePluginsService.getPlugin(languages)
    val sourceSet = resolveSourceSet(target, languagePlugin)
    val resources = resolveResources(target, languagePlugin)
    val languageData = languagePlugin.resolveModule(target)
    val sourceDependencies = languagePlugin.dependencySources(target, dependencyGraph)
    val environment = environmentItem(target)
    return Module(
      label = label,
      isSynthetic = false,
      directDependencies = directDependencies,
      languages = languages,
      tags = tags,
      baseDirectory = baseDirectory,
      sourceSet = sourceSet,
      resources = resources,
      outputs = emptySet(),
      sourceDependencies = sourceDependencies,
      languageData = languageData,
      environmentVariables = environment,
    )
  }

  private fun resolveDirectDependencies(target: TargetInfo): List<Label> = target.dependenciesList.map { it.label() }

  private fun inferLanguages(target: TargetInfo): Set<Language> {
    val languagesForTarget = Language.allOfKind(target.kind)
    val languagesForSources = target.sourcesList.flatMap { Language.allOfSource(it.relativePath) }.toHashSet()
    return languagesForTarget + languagesForSources
  }

  private fun ResolvedLabel.toDirectoryUri(): URI = bazelPathsResolver.pathToDirectoryUri(this.toBazelPath().toString(), isMainWorkspace)

  private fun resolveSourceSet(target: TargetInfo, languagePlugin: LanguagePlugin<*>): SourceSet {
    val (sources, nonExistentSources) =
      (target.sourcesList + languagePlugin.calculateAdditionalSources(target))
        .toSet()
        .map(bazelPathsResolver::resolve)
        .partition { it.exists() }

    nonExistentSources.forEach { it.logNonExistingFile(target.id) }
    val generatedSources =
      target.generatedSourcesList
        .toSet()
        .map(bazelPathsResolver::resolve)
        .filter { it.extension != "srcjar" }
        .onEach { if (it.notExists()) it.logNonExistingFile(target.id) }
        .filter { it.exists() }

    val sourceRootsAndData = sources.map { it to languagePlugin.calculateSourceRootAndAdditionalData(it) }
    val generatedRootsAndData = generatedSources.map { it to languagePlugin.calculateSourceRootAndAdditionalData(it) }
    return SourceSet(
      sources =
        sourceRootsAndData
          .map {
            SourceWithData(
              source = it.first.toUri(),
              data = it.second?.data,
            )
          }.toSet(),
      generatedSources =
        generatedRootsAndData
          .map {
            SourceWithData(
              source = it.first.toUri(),
              data = it.second?.data,
            )
          }.toSet(),
      sourceRoots = (sourceRootsAndData + generatedRootsAndData).mapNotNull { it.second?.sourceRoot?.toUri() }.toSet(),
    )
  }

  private fun Path.logNonExistingFile(targetId: String) {
    val message = "[WARN] target $targetId: $this does not exist."
    bspClientLogger.error(message)
  }

  private fun resolveResources(target: TargetInfo, languagePlugin: LanguagePlugin<*>): Set<URI> =
    bazelPathsResolver.resolveUris(target.resourcesList).toSet() + languagePlugin.resolveAdditionalResources(target)

  private fun environmentItem(target: TargetInfo): Map<String, String> {
    val inheritedEnvs = collectInheritedEnvs(target)
    val targetEnv = target.envMap
    return inheritedEnvs + targetEnv
  }

  private fun collectInheritedEnvs(targetInfo: TargetInfo): Map<String, String> =
    targetInfo.envInheritList.associateWith { System.getenv(it) }

  private fun removeDotBazelBspTarget(targets: Collection<Label>): Collection<Label> =
    targets.filter {
      it.isMainWorkspace && !it.packagePath.toString().startsWith(".bazelbsp")
    }

  private suspend fun createRustExternalModules(
    targetsToImport: Sequence<TargetInfo>,
    dependencyGraph: DependencyGraph,
    generatedLibraries: Map<Label, Collection<Library>>,
  ): Sequence<Module> {
    val modules = createModules(targetsToImport, dependencyGraph, generatedLibraries)
    return modules.asSequence().onEach {
      (it.languageData as? RustModule)?.isExternalModule = true
    }
  }
}
