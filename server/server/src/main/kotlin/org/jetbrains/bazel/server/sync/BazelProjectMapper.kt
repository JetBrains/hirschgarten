package org.jetbrains.bazel.server.sync

import com.google.common.hash.Hashing
import com.google.devtools.build.lib.view.proto.Deps
import com.intellij.platform.diagnostic.telemetry.helpers.useWithScope
import com.intellij.util.EnvironmentUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.bazelrunner.utils.BazelInfo
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.info.Dependency
import org.jetbrains.bazel.info.FileLocation
import org.jetbrains.bazel.info.TargetInfo
import org.jetbrains.bazel.label.CanonicalLabel
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.assumeLabel
import org.jetbrains.bazel.logger.BspClientLogger
import org.jetbrains.bazel.performance.bspTracer
import org.jetbrains.bazel.server.bzlmod.BzlmodRepoMapping
import org.jetbrains.bazel.server.bzlmod.RepoMapping
import org.jetbrains.bazel.server.bzlmod.RepoMappingDisabled
import org.jetbrains.bazel.server.bzlmod.TodoRepoMapping
import org.jetbrains.bazel.server.dependencygraph.DependencyGraph
import org.jetbrains.bazel.server.model.AspectSyncProject
import org.jetbrains.bazel.server.model.GoLibrary
import org.jetbrains.bazel.server.model.Library
import org.jetbrains.bazel.server.model.Module
import org.jetbrains.bazel.server.model.NonModuleTarget
import org.jetbrains.bazel.server.model.Tag
import org.jetbrains.bazel.server.paths.BazelPathsResolver
import org.jetbrains.bazel.server.sync.languages.LanguagePlugin
import org.jetbrains.bazel.server.sync.languages.LanguagePluginsService
import org.jetbrains.bazel.server.sync.languages.android.KotlinAndroidModulesMerger
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.FeatureFlags
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

private val THIRD_PARTY_LIBRARIES_PATTERN = "external/[^/]+/(.+)/([^/]+)/[^/]+$".toRegex()

class BazelProjectMapper(
  private val languagePluginsService: LanguagePluginsService,
  private val bazelPathsResolver: BazelPathsResolver,
  private val targetTagsResolver: TargetTagsResolver,
  private val mavenCoordinatesResolver: MavenCoordinatesResolver,
  private val kotlinAndroidModulesMerger: KotlinAndroidModulesMerger,
  private val bspClientLogger: BspClientLogger,
) {
  private suspend fun <T> measure(description: String, body: suspend () -> T): T =
    bspTracer.spanBuilder(description).useWithScope { body() }

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
    targets: Map<CanonicalLabel, TargetInfo>,
    rootTargets: Set<CanonicalLabel>,
    workspaceContext: WorkspaceContext,
    featureFlags: FeatureFlags,
    bazelInfo: BazelInfo,
    repoMapping: RepoMapping,
    hasError: Boolean,
  ): AspectSyncProject {
    languagePluginsService.prepareSync(targets.values.asSequence(), workspaceContext)
    val dependencyGraph =
      measure("Build dependency tree") {
        DependencyGraph(rootTargets, targets)
      }
    val transitiveCompileTimeJarsTargetKinds = workspaceContext.experimentalTransitiveCompileTimeJarsTargetKinds.values.toSet()
    val (targetsToImport, targetsAsLibraries) =
      measure("Select targets") {
        val targetsAtDepth =
          dependencyGraph
            .allTargetsAtDepth(
              workspaceContext.importDepth.value,
              rootTargets,
            ) { !isTargetTreatedAsInternal(it, repoMapping) }
        val (targetsToImport, nonWorkspaceTargets) =
          targetsAtDepth.targets.partition {
            isWorkspaceTarget(it, repoMapping, transitiveCompileTimeJarsTargetKinds, featureFlags)
          }
        val libraries = (nonWorkspaceTargets + targetsAtDepth.directDependencies).associateBy { it.id }
        val usedLibraries = dependencyGraph.filterUsedLibraries(libraries, targetsToImport.asSequence())
        targetsToImport.asSequence() to usedLibraries
      }
    val interfacesAndBinariesFromTargetsToImport =
      measure("Collect interfaces and classes from targets to import") {
        collectInterfacesAndClasses(targetsToImport)
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
        createLibraries(targetsAsLibraries, repoMapping) +
          librariesFromDeps.values
            .flatten()
            .distinct()
            .associateBy { it.label }
      }
    val extraLibrariesFromJdeps =
      measure("Libraries from jdeps") {
        jdepsLibraries(
          targetsToImport.associateBy { it.id },
          librariesFromDeps,
          librariesFromDepsAndTargets,
          interfacesAndBinariesFromTargetsToImport,
          transitiveCompileTimeJarsTargetKinds,
        )
      }
    val librariesFromTransitiveCompileTimeJars =
      measure("Libraries from transitive compile-time jars") {
        createLibrariesFromTransitiveCompileTimeJars(
          targetsToImport,
          targets,
          extraLibrariesFromJdeps,
          dependencyGraph,
          workspaceContext.experimentalAddTransitiveCompileTimeJars.value,
          transitiveCompileTimeJarsTargetKinds,
          workspaceContext.experimentalNoPruneTransitiveCompileTimeJarsPatterns.values,
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
          transitiveCompileTimeJarsTargetKinds,
          repoMapping,
          workspaceContext,
        )
      }
    val mergedModulesFromBazel =
      measure("Merge Kotlin Android modules") {
        kotlinAndroidModulesMerger.mergeKotlinAndroidModules(modulesFromBazel, featureFlags)
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
          createGoLibraries(targetsAsLibraries, repoMapping)
      }

    val nonModuleTargetIds =
      (removeDotBazelBspTarget(targets.keys) - mergedModulesFromBazel.map { it.label }.toSet() - librariesToImport.keys).toSet()
    val nonModuleTargets =
      createNonModuleTargets(
        targets.filterKeys {
          nonModuleTargetIds.contains(it) &&
            isTargetTreatedAsInternal(it.assumeLabel(), repoMapping)
        },
        repoMapping,
        workspaceContext,
      )

    val workspaceName = targets.values.map { it.workspaceName }.firstOrNull() ?: "_main"

    return AspectSyncProject(
      workspaceRoot = workspaceRoot,
      bazelRelease = bazelInfo.release,
      modules = mergedModulesFromBazel.toList(),
      libraries = librariesToImport,
      goLibraries = goLibrariesToImport,
      nonModuleTargets = nonModuleTargets,
      repoMapping = repoMapping,
      hasError = hasError,
      workspaceName = workspaceName,
      workspaceContext = workspaceContext,
      targets = targets,
    )
  }

  private fun <K, V> concatenateMaps(vararg maps: Map<K, List<V>>): Map<K, List<V>> =
    maps
      .flatMap { it.keys }
      .distinct()
      .associateWith { key ->
        maps.flatMap { it[key].orEmpty() }
      }

  private fun calculateOutputJarsLibraries(targetsToImport: Sequence<TargetInfo>): Map<CanonicalLabel, List<Library>> =
    targetsToImport
      .filter { shouldCreateOutputJarsLibrary(it) }
      .mapNotNull { target ->
        createLibrary(
          Label.parseCanonical(target.id.toString() + "_output_jars"),
          target,
          onlyOutputJars = true,
          isInternalTarget = true,
        )?.let { library ->
          target.id to listOf(library)
        }
      }.toMap()

  private fun shouldCreateOutputJarsLibrary(targetInfo: TargetInfo) =
    !targetInfo.kind.endsWith("_resources") &&
      (
        targetInfo.generatedSources.any { it.relativePath.endsWith(".srcjar") } ||
          targetInfo.jvmTargetInfo != null &&
          !hasKnownJvmSources(targetInfo)
      )

  private fun annotationProcessorLibraries(targetsToImport: Sequence<TargetInfo>): Map<CanonicalLabel, List<Library>> =
    targetsToImport
      .filter { it.jvmTargetInfo?.generatedJars?.isNotEmpty() == true }
      .associate { targetInfo ->
        targetInfo.id to
          Library(
            label = Label.parseCanonical(targetInfo.id.toString() + "_generated"),
            outputs =
              targetInfo.jvmTargetInfo!!
                .generatedJars
                .flatMap { it.binaryJars }
                .map { bazelPathsResolver.resolve(it) }
                .toSet(),
            sources =
              targetInfo.jvmTargetInfo!!
                .generatedJars
                .flatMap { it.sourceJars }
                .map { bazelPathsResolver.resolve(it) }
                .toSet(),
            dependencies = emptyList(),
            interfaceJars = emptySet(),
          )
      }.map { it.key to listOf(it.value) }
      .toMap()

  private fun calculateKotlinStdlibsMapper(targetsToImport: Sequence<TargetInfo>): Map<CanonicalLabel, List<Library>> {
    val projectLevelKotlinStdlibsLibrary = calculateProjectLevelKotlinStdlibsLibrary(targetsToImport)
    val kotlinTargetsIds = targetsToImport.filter { it.kotlinTargetInfo != null }.map { it.id }

    return projectLevelKotlinStdlibsLibrary
      ?.let { stdlibsLibrary -> kotlinTargetsIds.associateWith { listOf(stdlibsLibrary) } }
      .orEmpty()
  }

  private fun calculateProjectLevelKotlinStdlibsLibrary(targetsToImport: Sequence<TargetInfo>): Library? {
    val kotlinStdlibsJars = calculateProjectLevelKotlinStdlibsJars(targetsToImport)

    // rules_kotlin does not expose source jars for jvm stdlibs, so this is the way they can be retrieved for now
    val inferredSourceJars =
      kotlinStdlibsJars
        .map { it.parent.resolve(it.fileName.toString().replace(".jar", "-sources.jar")) }
        .filter { it.exists() }
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

  private fun calculateProjectLevelKotlinStdlibsJars(targetsToImport: Sequence<TargetInfo>): Set<Path> =
    targetsToImport
      .filter { it.kotlinTargetInfo != null }
      .map { it.kotlinTargetInfo!!.stdlibs }
      .flatMap { it.resolvePaths() }
      .toSet()

  private fun calculateKotlincPluginLibrariesMapper(targetsToImport: Sequence<TargetInfo>): Map<CanonicalLabel, List<Library>> =
    targetsToImport
      .filter { it.kotlinTargetInfo != null && it.kotlinTargetInfo!!.kotlincPluginInfos.isNotEmpty() }
      .associate {
        val pluginClasspaths =
          it.kotlinTargetInfo!!
            .kotlincPluginInfos
            .flatMap { it.pluginJars }
            .map { bazelPathsResolver.resolve(it) }
            .distinct()
        Pair(
          it.id,
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

  private fun calculateScalaLibrariesMapper(targetsToImport: Sequence<TargetInfo>): Map<CanonicalLabel, List<Library>> {
    val projectLevelScalaSdkLibraries = calculateProjectLevelScalaSdkLibraries()
    val projectLevelScalaTestLibraries = calculateProjectLevelScalaTestLibraries()
    val scalaTargets = targetsToImport.filter { it.scalaTargetInfo != null }.map { it.id }
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

  private fun calculateProjectLevelScalaSdkLibraries(): Map<Path, Library> =
    getProjectLevelScalaSdkLibrariesJars().associateWith {
      Library(
        label = Label.synthetic(it.name),
        outputs = setOf(it),
        sources = emptySet(),
        dependencies = emptyList(),
      )
    }

  private fun calculateProjectLevelScalaTestLibraries(): Map<Path, Library> =
    languagePluginsService.scalaLanguagePlugin.scalaTestJars.values
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

  private fun getProjectLevelScalaSdkLibrariesJars(): Set<Path> =
    languagePluginsService.scalaLanguagePlugin.scalaSdks.values
      .toSet()
      .flatMap {
        it.compilerJars
      }.toSet()

  private fun calculateAndroidLibrariesMapper(targetsToImport: Sequence<TargetInfo>): Map<CanonicalLabel, List<Library>> =
    targetsToImport
      .mapNotNull { target ->
        val aidlLibrary = createAidlLibrary(target) ?: return@mapNotNull null
        target.id to listOf(aidlLibrary)
      }.toMap()

  private fun createAidlLibrary(target: TargetInfo): Library? {
    target.androidTargetInfo?.aidlBinaryJar ?: return null

    val libraryLabel = Label.parseCanonical(target.id.toString() + "_aidl")
    if (target.sources.isEmpty()) {
      // Bazel doesn't create the AIDL jar if there's no sources, since it'd be the same as the output jar
      return null
    }

    val outputs = listOfNotNull(target.androidTargetInfo?.aidlBinaryJar).resolvePaths()
    val sources =
      listOfNotNull(target.androidTargetInfo?.aidlSourceJar).resolvePaths()
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
        val goInfo = target.goTargetInfo ?: return@mapNotNull null
        val label = target.id
        val libraries =
          goInfo.generatedLibraries.map {
            GoLibrary(
              label = label,
              goImportPath = goInfo.importPath,
              goRoot = bazelPathsResolver.resolve(it).parent,
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
    targetsToImport: Map<CanonicalLabel, TargetInfo>,
    libraryDependencies: Map<CanonicalLabel, List<Library>>,
    librariesToImport: Map<CanonicalLabel, Library>,
    interfacesAndBinariesFromTargetsToImport: Map<CanonicalLabel, Set<Path>>,
    transitiveCompileTimeJarsTargetKinds: Set<String>,
  ): Map<CanonicalLabel, List<Library>> {
    val targetsToJdepsJars =
      getAllJdepsDependencies(targetsToImport, libraryDependencies, librariesToImport, transitiveCompileTimeJarsTargetKinds)
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
    targetsToImport: Map<CanonicalLabel, TargetInfo>,
    libraryDependencies: Map<CanonicalLabel, List<Library>>,
    librariesToImport: Map<CanonicalLabel, Library>,
    transitiveCompileTimeJarsTargetKinds: Set<String>,
  ): Map<CanonicalLabel, Set<Path>> {
    val jdepsJars =
      withContext(Dispatchers.IO) {
        targetsToImport.values
          .filter { targetSupportsJdeps(it, transitiveCompileTimeJarsTargetKinds) }
          .map { target ->
            async {
              target.id to dependencyJarsFromJdepsFiles(target)
            }
          }.awaitAll()
      }.filter { it.second.isNotEmpty() }.toMap()

    val allJdepsJars =
      jdepsJars.values
        .asSequence()
        .flatten()
        .toSet()

    return withContext(Dispatchers.Default) {
      val outputJarsFromTransitiveDepsCache = ConcurrentHashMap<CanonicalLabel, Set<Path>>()
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
    targetOrLibrary: CanonicalLabel,
    targetsToImport: Map<CanonicalLabel, TargetInfo>,
    libraryDependencies: Map<CanonicalLabel, List<Library>>,
    librariesToImport: Map<CanonicalLabel, Library>,
    outputJarsFromTransitiveDepsCache: ConcurrentHashMap<CanonicalLabel, Set<Path>>,
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
        targetsToImport[targetOrLibrary]?.dependencies.orEmpty().map { it.id } +
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
    targetInfo.jvmTargetInfo!!
      .jdeps
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

  private fun targetSupportsJdeps(targetInfo: TargetInfo, transitiveCompileTimeJarsTargetKinds: Set<String>): Boolean {
    if (targetInfo.kind in transitiveCompileTimeJarsTargetKinds) return false
    val languages = inferLanguages(targetInfo, transitiveCompileTimeJarsTargetKinds)
    return setOf(LanguageClass.JAVA, LanguageClass.KOTLIN, LanguageClass.SCALA, LanguageClass.ANDROID).containsAll(languages)
  }

  private val replacementRegex = "[^0-9a-zA-Z]".toRegex()

  private fun syntheticLabel(lib: Path): CanonicalLabel {
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
    targets: Map<CanonicalLabel, TargetInfo>,
    repoMapping: RepoMapping,
    workspaceContext: WorkspaceContext,
  ): List<NonModuleTarget> =
    targets
      .map { (label, targetInfo) ->
        NonModuleTarget(
          label = label,
          tags = targetTagsResolver.resolveTags(targetInfo, workspaceContext),
          baseDirectory = bazelPathsResolver.toDirectoryPath(label.assumeLabel(), repoMapping),
          kindString = targetInfo.kind,
        )
      }

  private suspend fun createLibraries(targets: Map<CanonicalLabel, TargetInfo>, repoMapping: RepoMapping): Map<CanonicalLabel, Library> =
    withContext(Dispatchers.Default) {
      targets
        .map { (targetId, targetInfo) ->
          async {
            createLibrary(
              label = targetId,
              targetInfo = targetInfo,
              onlyOutputJars = false,
              isInternalTarget = isTargetTreatedAsInternal(targetId, repoMapping),
            )?.let { library ->
              targetId to library
            }
          }
        }.awaitAll()
        .filterNotNull()
        .toMap()
    }

  private fun createLibrary(
    label: CanonicalLabel,
    targetInfo: TargetInfo,
    onlyOutputJars: Boolean,
    isInternalTarget: Boolean,
  ): Library? {
    val outputs = getTargetOutputJarPaths(targetInfo) + getAndroidAarPaths(targetInfo) + getIntellijPluginJars(targetInfo)
    val sources = getSourceJarPaths(targetInfo)
    val interfaceJars = getTargetInterfaceJarsSet(targetInfo).toSet()
    val dependencies: List<Dependency> = if (!onlyOutputJars) targetInfo.dependencies else emptyList()
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
      dependencies = targetInfo.dependencies.map { it.id },
      interfaceJars = interfaceJars,
      mavenCoordinates = mavenCoordinates,
      isFromInternalTarget = isInternalTarget,
    )
  }

  private fun shouldCreateLibrary(
    dependencies: List<Dependency>,
    outputs: Collection<Path>,
    interfaceJars: Collection<Path>,
    sources: Collection<Path>,
  ): Boolean = dependencies.isNotEmpty() || !outputs.isEmptyJarList() || !interfaceJars.isEmptyJarList() || !sources.isEmptyJarList()

  private fun Collection<Path>.isEmptyJarList(): Boolean = isEmpty() || singleOrNull()?.name == "empty.jar"

  private fun createGoLibraries(targets: Map<CanonicalLabel, TargetInfo>, repoMapping: RepoMapping): Map<CanonicalLabel, GoLibrary> =
    targets
      .mapValues { (targetId, targetInfo) ->
        createGoLibrary(targetId, targetInfo, repoMapping)
      }.filterValues {
        it.isGoLibrary()
      }

  private fun GoLibrary.isGoLibrary(): Boolean = !goImportPath.isNullOrEmpty() && goRoot.toString().isNotEmpty()

  private fun createGoLibrary(
    label: CanonicalLabel,
    targetInfo: TargetInfo,
    repoMapping: RepoMapping,
  ): GoLibrary =
    GoLibrary(
      label = label,
      goImportPath = targetInfo.goTargetInfo?.importPath,
      goRoot = getGoRootPath(targetInfo, repoMapping),
    )

  private fun createLibrariesFromTransitiveCompileTimeJars(
    targetsToImport: Sequence<TargetInfo>,
    targetsMap: Map<CanonicalLabel, TargetInfo>,
    extraLibrariesFromJdeps: Map<CanonicalLabel, List<Library>>,
    dependencyGraph: DependencyGraph,
    transitiveCompileTimeJarsEnabled: Boolean,
    transitiveCompileTimeJarsTargetKinds: Set<String>,
    noPrunePatterns: List<String>,
  ): Map<CanonicalLabel, List<Library>> =
    if (transitiveCompileTimeJarsEnabled) {
      val res = HashMap<Label, Library>()
      targetsToImport.filter { it.kind in transitiveCompileTimeJarsTargetKinds }.associate { targetInfo ->
        val targetLabel = targetInfo.id
        val explicitCompileTimeInterfaces = calculateExplicitCompileTimeInterfaces(targetInfo, targetsMap)
        val jdepsJars = collectReverseDepsJdepsJars(targetLabel, dependencyGraph, extraLibrariesFromJdeps)
        val explicitlyDefinedThirdPartyLibraries = extractExplicitThirdPartyLibraries(explicitCompileTimeInterfaces)
        targetLabel to
          targetInfo.jvmTargetInfo!!
            .transitiveCompileTimeJars
            .map { bazelPathsResolver.resolve(it) }
            .filter { path ->
              explicitCompileTimeInterfaces.contains(path) ||
                jdepsJars.contains(path) ||
                matchesExplicitThirdPartyLibrary(path, explicitlyDefinedThirdPartyLibraries) ||
                doNotPrune(path, noPrunePatterns)
            }.map { path ->
              val label = syntheticLabel(path)
              res.computeIfAbsent(label) {
                Library(
                  label = label,
                  outputs = setOf(path),
                  sources = setOf(),
                  dependencies = listOf(),
                )
              }
            }
      }
    } else {
      emptyMap()
    }

  private fun calculateExplicitCompileTimeInterfaces(target: TargetInfo, targetsMap: Map<CanonicalLabel, TargetInfo>) =
    target.dependencies
      .asSequence()
      .mapNotNull { targetsMap[it.id] }
      .filter { !it.isCompilableByJps() }
      .flatMap { getTargetInterfaceJarsList(it) }
      .toSet()

  private fun TargetInfo.isCompilableByJps(): Boolean {
    val languages = inferLanguages(this, emptySet())
    if (languages.isEmpty()) return false
    return setOf(LanguageClass.JAVA, LanguageClass.KOTLIN).containsAll(languages)
  }

  private fun collectReverseDepsJdepsJars(
    targetLabel: CanonicalLabel,
    dependencyGraph: DependencyGraph,
    extraLibrariesFromJdeps: Map<CanonicalLabel, List<Library>>,
  ): Set<Path> =
    dependencyGraph
      .getReverseDependencies(targetLabel)
      .mapNotNull { extraLibrariesFromJdeps[it] }
      .flatMap { it.flatMap { library -> library.outputs } }
      .toSet()

  private fun extractExplicitThirdPartyLibraries(explicitCompileTimeInterfaces: Set<Path>): Set<String> =
    explicitCompileTimeInterfaces
      .filter { it.toString().contains("/external/") }
      .mapNotNull { jarPath ->
        THIRD_PARTY_LIBRARIES_PATTERN.find(jarPath.toString())?.groupValues?.getOrNull(1)
      }.toSet()

  /**
   * When pruning transitive compile time jars list, ignore jars that are in the pruning exception list
   */
  private fun doNotPrune(jar: Path, noPrunePatterns: List<String>): Boolean {
    val pathString = jar.toString()
    return noPrunePatterns.any { pathString.contains(it) }
  }

  private fun matchesExplicitThirdPartyLibrary(jar: Path, explicitThirdPartyLibraries: Set<String>): Boolean {
    val pathString = jar.toString()
    // TODO: generalize the logic to support more general cases
    // related ticket: https://youtrack.jetbrains.com/issue/BAZEL-1739/Generalize-logic-to-retrieve-transitive-compile-time-jars-prune-them-better
    if (pathString.contains("/external/maven") || pathString.contains("/external/multiversion_maven")) {
      val matcher = THIRD_PARTY_LIBRARIES_PATTERN.find(pathString)
      if (matcher != null) {
        // e.g. "com/google/guava/guava"
        val id = matcher.groupValues.getOrNull(1) ?: return false
        return explicitThirdPartyLibraries.contains(id)
      }
    }
    return false
  }

  private fun List<FileLocation>.resolvePaths() = map { bazelPathsResolver.resolve(it) }.toSet()

  private fun getTargetOutputJarPaths(targetInfo: TargetInfo) =
    getTargetOutputJarsList(targetInfo)
      .toSet()

  private fun getAndroidAarPaths(targetInfo: TargetInfo): Set<Path> {
    val androidAarImportInfo = targetInfo.androidAarImportInfo ?: return emptySet()

    val result = mutableSetOf<Path>()
    result.add(bazelPathsResolver.resolve(androidAarImportInfo.manifest))
    result.add(bazelPathsResolver.resolve(androidAarImportInfo.resourceFolder).resolve("res"))
    result.add(bazelPathsResolver.resolve(androidAarImportInfo.rTxt))
    return result
  }

  private fun getIntellijPluginJars(targetInfo: TargetInfo): Set<Path> {
    // _repackaged_files is created upon calling repackaged_files in rules_intellij
    if (targetInfo.kind != "_repackaged_files") return emptySet()
    return targetInfo.generatedSources
      .resolvePaths()
      .filter { it.extension == "jar" }
      .toSet()
  }

  private fun getSourceJarPaths(targetInfo: TargetInfo) =
    targetInfo.jvmTargetInfo!!
      .jars
      .flatMap { it.sourceJars }
      .resolvePaths()

  private fun getTargetOutputJarsSet(targetInfo: TargetInfo) = getTargetOutputJarsList(targetInfo).toSet()

  private fun getTargetOutputJarsList(targetInfo: TargetInfo) =
    targetInfo.jvmTargetInfo!!
      .jars
      .flatMap { it.binaryJars }
      .map { bazelPathsResolver.resolve(it) }

  private fun getTargetInterfaceJarsSet(targetInfo: TargetInfo) = getTargetInterfaceJarsList(targetInfo).toSet()

  private fun getTargetInterfaceJarsList(targetInfo: TargetInfo) =
    targetInfo.jvmTargetInfo!!
      .jars
      .flatMap { it.interfaceJars }
      .map { bazelPathsResolver.resolve(it) }

  private fun getGoRootPath(targetInfo: TargetInfo, repoMapping: RepoMapping): Path =
    bazelPathsResolver.toDirectoryPath(targetInfo.id.assumeLabel(), repoMapping)

  private fun collectInterfacesAndClasses(targets: Sequence<TargetInfo>) =
    targets
      .associate { target ->
        target.id to
          (getTargetInterfaceJarsList(target) + getTargetOutputJarsList(target))
            .toSet()
      }

  private fun hasKnownJvmSources(targetInfo: TargetInfo) =
    targetInfo.sources.any {
      it.relativePath.endsWith(".java") ||
        it.relativePath.endsWith(".kt") ||
        it.relativePath.endsWith(".scala")
    }

  private fun hasKnownPythonSources(targetInfo: TargetInfo): Boolean =
    targetInfo.sources.any {
      it.relativePath.endsWith(".py")
    } ||
      targetInfo.pythonTargetInfo?.isCodeGenerator == true

  private fun hasKnownGoSources(targetInfo: TargetInfo) =
    targetInfo.sources.any {
      it.relativePath.endsWith(".go")
    }

  private fun externalRepositoriesTreatedAsInternal(repoMapping: RepoMapping) =
    when (repoMapping) {
      is BzlmodRepoMapping -> repoMapping.canonicalRepoNameToLocalPath.keys
      is RepoMappingDisabled -> emptySet()
      TodoRepoMapping -> TODO()
    }

  /**
   * This is a heuristic to guess if the target is generated by gazelle.
   *
   * It is not 100% reliable, but it works for our use cases, i.e., to import gazelle generated targets as internal.
   */
  private val Label.isGazelleGenerated get() = repoName.startsWith("gazelle")

  private fun isTargetTreatedAsInternal(target: Label, repoMapping: RepoMapping): Boolean =
    target.isMainWorkspace || target.repo.repoName in externalRepositoriesTreatedAsInternal(repoMapping) || target.isGazelleGenerated

  // TODO https://youtrack.jetbrains.com/issue/BAZEL-1303
  private fun isWorkspaceTarget(
    target: TargetInfo,
    repoMapping: RepoMapping,
    transitiveCompileTimeJarsTargetKinds: Set<String>,
    featureFlags: FeatureFlags,
  ): Boolean =
    (
      isTargetTreatedAsInternal(target.id.assumeLabel(), repoMapping) &&
        (
          shouldImportTargetKind(target.kind, transitiveCompileTimeJarsTargetKinds) ||
            target.jvmTargetInfo != null &&
            (
              target.dependencies.isNotEmpty() ||
                hasKnownJvmSources(target)
            )
        )
    ) ||
      featureFlags.isGoSupportEnabled &&
      target.goTargetInfo != null &&
      hasKnownGoSources(target) ||
      featureFlags.isPythonSupportEnabled &&
      target.pythonTargetInfo != null &&
      hasKnownPythonSources(target)

  private fun shouldImportTargetKind(kind: String, transitiveCompileTimeJarsTargetKinds: Set<String>): Boolean =
    kind in workspaceTargetKinds || kind in transitiveCompileTimeJarsTargetKinds

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
      "android_library",
      "android_binary",
      "android_local_test",
      "kt_android_library",
      "kt_android_local_test",
      "intellij_plugin_debug_target",
      "go_proto_library",
      "go_library",
      "go_binary",
      "go_test",
    )

  private suspend fun createModules(
    targetsToImport: Sequence<TargetInfo>,
    dependencyGraph: DependencyGraph,
    generatedLibraries: Map<CanonicalLabel, Collection<Library>>,
    transitiveCompileTimeJarsTargetKinds: Set<String>,
    repoMapping: RepoMapping,
    workspaceContext: WorkspaceContext,
  ): List<Module> =
    withContext(Dispatchers.Default) {
      targetsToImport
        .toList()
        .map {
          async {
            createModule(
              it,
              dependencyGraph,
              generatedLibraries[it.id].orEmpty(),
              transitiveCompileTimeJarsTargetKinds,
              repoMapping,
              workspaceContext,
            )
          }
        }.awaitAll()
        .filterNot { it.tags.contains(Tag.NO_IDE) }
    }

  private fun createModule(
    target: TargetInfo,
    dependencyGraph: DependencyGraph,
    extraLibraries: Collection<Library>,
    transitiveCompileTimeJarsTargetKinds: Set<String>,
    repoMapping: RepoMapping,
    workspaceContext: WorkspaceContext,
  ): Module {
    val label = target.id
    val resolvedDependencies = resolveDirectDependencies(target)
    // extra libraries can override some library versions, so they should be put before
    val directDependencies = extraLibraries.map { it.label } + resolvedDependencies
    val languages = inferLanguages(target, transitiveCompileTimeJarsTargetKinds)
    val tags = targetTagsResolver.resolveTags(target, workspaceContext)
    val baseDirectory = bazelPathsResolver.toDirectoryPath(label, repoMapping)
    val languagePlugin = languagePluginsService.getPlugin(languages)
    val sources = resolveSourceSet(target, languagePlugin)
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
      sources = sources,
      resources = resources,
      sourceDependencies = sourceDependencies,
      languageData = languageData,
      environmentVariables = environment,
      kindString = target.kind,
    )
  }

  private fun resolveDirectDependencies(target: TargetInfo): List<CanonicalLabel> = target.dependencies.map { it.id }

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
      "android_binary" to setOf(LanguageClass.JAVA, LanguageClass.ANDROID),
      "android_library" to setOf(LanguageClass.JAVA, LanguageClass.ANDROID),
      "android_local_test" to setOf(LanguageClass.JAVA, LanguageClass.ANDROID),
      // TODO this should include kotlin probably, but I'm leaving it as it was
      "kt_android_library" to setOf(LanguageClass.JAVA, LanguageClass.ANDROID),
      "kt_android_local_test" to setOf(LanguageClass.JAVA, LanguageClass.ANDROID),
      "go_binary" to setOf(LanguageClass.GO),
      "go_test" to setOf(LanguageClass.GO),
      "go_library" to setOf(LanguageClass.GO),
      "go_source" to setOf(LanguageClass.GO),
      "py_binary" to setOf(LanguageClass.PYTHON),
      "py_test" to setOf(LanguageClass.PYTHON),
      "py_library" to setOf(LanguageClass.PYTHON),
    )

  private fun inferLanguages(target: TargetInfo, transitiveCompileTimeJarsTargetKinds: Set<String>): Set<LanguageClass> =
    buildSet {
      // TODO It's a hack preserved from before TargetKind refactorking, to be removed
      if (transitiveCompileTimeJarsTargetKinds.contains(target.kind)) {
        add(LanguageClass.JAVA)
      }
      if (target.jvmTargetInfo != null) {
        add(LanguageClass.JAVA)
      }
      if (target.pythonTargetInfo != null) {
        add(LanguageClass.PYTHON)
      }
      if (target.goTargetInfo != null) {
        add(LanguageClass.GO)
      }
      languagesFromKinds[target.kind]?.let {
        addAll(it)
      }
    }

  private fun resolveSourceSet(target: TargetInfo, languagePlugin: LanguagePlugin<*>): List<SourceItem> {
    val sources =
      (target.sources + languagePlugin.calculateAdditionalSources(target))
        .toSet()
        .map(bazelPathsResolver::resolve)
        .onEach { if (it.notExists()) logNonExistingFile(it, target.id) }
        .filter { it.exists() }
        .map { SourceItem(path = it, generated = false, jvmPackagePrefix = languagePlugin.calculateJvmPackagePrefix(it)) }

    val generatedSources =
      target.generatedSources
        .toSet()
        .map(bazelPathsResolver::resolve)
        .filter { it.extension != "srcjar" }
        .onEach { if (it.notExists()) logNonExistingFile(it, target.id) }
        .filter { it.exists() }
        .map { SourceItem(path = it, generated = true, jvmPackagePrefix = languagePlugin.calculateJvmPackagePrefix(it)) }

    return sources + generatedSources
  }

  private fun logNonExistingFile(file: Path, targetId: CanonicalLabel) {
    val message = "[WARN] target $targetId: $file does not exist."
    bspClientLogger.error(message)
  }

  private fun resolveResources(target: TargetInfo, languagePlugin: LanguagePlugin<*>): Set<Path> =
    (bazelPathsResolver.resolvePaths(target.resources) + languagePlugin.resolveAdditionalResources(target))
      .filter { it.exists() }
      .toSet()

  private fun environmentItem(target: TargetInfo): Map<String, String> {
    val inheritedEnvs = collectInheritedEnvs(target)
    val targetEnv = target.env
    return inheritedEnvs + targetEnv
  }

  private fun collectInheritedEnvs(targetInfo: TargetInfo): Map<String, String> =
    targetInfo.envInherit.associateWith { EnvironmentUtil.getValue(it).orEmpty() }

  private fun removeDotBazelBspTarget(targets: Collection<Label>): Collection<Label> =
    targets.filter {
      it.isMainWorkspace && !it.packagePath.toString().startsWith(".bazelbsp")
    }
}
