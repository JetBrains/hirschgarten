package org.jetbrains.bazel.sync.workspace.mapper.normal

import com.google.common.hash.Hashing
import com.google.devtools.build.lib.view.proto.Deps
import com.intellij.openapi.diagnostic.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.BzlmodRepoMapping
import org.jetbrains.bazel.commons.EnvironmentProvider
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.commons.RepoMappingDisabled
import org.jetbrains.bazel.commons.Tag
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.info.BspTargetInfo.FileLocation
import org.jetbrains.bazel.info.BspTargetInfo.TargetInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.label.assumeResolved
import org.jetbrains.bazel.performance.bspTracer
import org.jetbrains.bazel.performance.telemetry.useWithScope
import org.jetbrains.bazel.server.label.label
import org.jetbrains.bazel.sync.workspace.graph.DependencyGraph
import org.jetbrains.bazel.sync.workspace.languages.LanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.LanguagePluginsService
import org.jetbrains.bazel.sync.workspace.model.Library
import org.jetbrains.bazel.sync.workspace.model.Module
import org.jetbrains.bazel.sync.workspace.model.NonModuleTarget
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

class AspectBazelProjectMapper(
  private val languagePluginsService: LanguagePluginsService,
  private val bazelPathsResolver: BazelPathsResolver,
  private val targetTagsResolver: TargetTagsResolver,
  private val mavenCoordinatesResolver: MavenCoordinatesResolver,
  private val environmentProvider: EnvironmentProvider,
) {
  val logger = logger<AspectBazelProjectMapper>()

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
    targets: Map<Label, TargetInfo>,
    rootTargets: Set<Label>,
    workspaceContext: WorkspaceContext,
    featureFlags: FeatureFlags,
    repoMapping: RepoMapping,
    hasError: Boolean,
  ): AspectBazelMappedProject {
    languagePluginsService.prepareSync(targets.values.asSequence(), workspaceContext)
    val dependencyGraph =
      measure("Build dependency tree") {
        DependencyGraph(rootTargets, targets)
      }
    val (targetsToImport, targetsAsLibraries) =
      measure("Select targets") {
        // the import depth mechanism does not apply for go targets sync
        // for now, go sync assumes to retrieve all transitive targets, which is equivalent to `import_depth: -1`
        // in fact, go sync should not even go through this highly overfitted JVM model: https://youtrack.jetbrains.com/issue/BAZEL-2210
        val (goTargetLabels, nonGoTargetLabels) = rootTargets.partition { targets[it]?.hasGoTargetInfo() == true }
        val nonGoTargetsAtDepth =
          dependencyGraph
            .allTargetsAtDepth(
              workspaceContext.importDepth.value,
              nonGoTargetLabels.toSet(),
              isExternalTarget = { !isTargetTreatedAsInternal(it.assumeResolved(), repoMapping) },
              targetSupportsStrictDeps = { id -> targets[id]?.let { targetSupportsStrictDeps(it) } == true },
              isWorkspaceTarget = { id ->
                targets[id]?.let { target ->
                  target.sourcesCount > 0 && isWorkspaceTarget(target, repoMapping, featureFlags)
                } == true
              },
            )
        val (nonGoTargetsToImport, nonWorkspaceTargets) =
          nonGoTargetsAtDepth.targets.partition {
            isWorkspaceTarget(it, repoMapping, featureFlags)
          }
        val goTargetsToImport =
          dependencyGraph
            .allTransitiveTargets(goTargetLabels.toSet())
            .targets
            .filter { isWorkspaceTarget(it, repoMapping, featureFlags) }
        val libraries = (nonWorkspaceTargets + nonGoTargetsAtDepth.directDependencies).associateBy { it.label() }
        (nonGoTargetsToImport + goTargetsToImport).asSequence() to libraries
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
        createLibraries(targetsAsLibraries, repoMapping) +
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
    val modulesFromBazel =
      measure("Create modules") {
        createModules(
          targetsToImport,
          dependencyGraph,
          concatenateMaps(
            librariesFromDeps,
            extraLibrariesFromJdeps,
          ),
          repoMapping,
          workspaceContext,
        )
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

    val workspaceName = targets.values.map { it.workspaceName }.firstOrNull() ?: "_main"

    return AspectBazelMappedProject(
      workspaceName = workspaceName,
      modules = modulesFromBazel.toList(),
      libraries = librariesToImport,
      nonModuleTargets = nonModuleTargets,
      hasError = hasError,
      moduleCache = modulesFromBazel.associateBy { it.label },
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
        createLibrary(Label.parse(target.id + "_output_jars"), target, onlyOutputJars = true, isInternalTarget = true)?.let { library ->
          target.label() to listOf(library)
        }
      }.toMap()

  private fun shouldCreateOutputJarsLibrary(targetInfo: TargetInfo) =
    !targetInfo.kind.endsWith("_resources") &&
      (
        targetInfo.generatedSourcesList.any { it.relativePath.endsWith(".srcjar") } ||
          (targetInfo.hasJvmTargetInfo() && !hasKnownJvmSources(targetInfo)) ||
          (targetInfo.hasJvmTargetInfo() && targetInfo.jvmTargetInfo.hasApiGeneratingPlugins)
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

  private fun getGoRootPath(targetInfo: TargetInfo, repoMapping: RepoMapping): Path =
    bazelPathsResolver.toDirectoryPath(targetInfo.label().assumeResolved(), repoMapping)

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
      featureFlags.isGoSupportEnabled &&
      target.hasGoTargetInfo() &&
      hasKnownGoSources(target) ||
      featureFlags.isPythonSupportEnabled &&
      target.hasPythonTargetInfo() &&
      hasKnownPythonSources(target)

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

  // TODO BAZEL-2208
  // The only language that supports strict deps by default is Java, in Kotlin and Scala strict deps are disabled by default.
  private fun targetSupportsStrictDeps(target: TargetInfo): Boolean =
    target.hasJvmTargetInfo() && !target.hasScalaTargetInfo() && !target.hasKotlinTargetInfo()

  private suspend fun createModules(
    targetsToImport: Sequence<TargetInfo>,
    dependencyGraph: DependencyGraph,
    generatedLibraries: Map<Label, Collection<Library>>,
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
              generatedLibraries[it.label()].orEmpty(),
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
    repoMapping: RepoMapping,
    workspaceContext: WorkspaceContext,
  ): Module {
    val label = target.label().assumeResolved()
    val resolvedDependencies = resolveDirectDependencies(target)
    // https://youtrack.jetbrains.com/issue/BAZEL-983: extra libraries can override some library versions, so they should be put before
    val (extraLibraries, lowPriorityExtraLibraries) = extraLibraries.partition { !it.isLowPriority }
    val directDependencies = extraLibraries.map { it.label } + resolvedDependencies + lowPriorityExtraLibraries.map { it.label }
    val languages = inferLanguages(target)
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
      (target.sourcesList + languagePlugin.calculateAdditionalSources(target))
        .toSet()
        .map(bazelPathsResolver::resolve)
        .onEach { if (it.notExists()) logNonExistingFile(it, target.id) }
        .map { SourceItem(path = it, generated = false, jvmPackagePrefix = languagePlugin.calculateJvmPackagePrefix(it)) }

    val generatedSources =
      target.generatedSourcesList
        .toSet()
        .map(bazelPathsResolver::resolve)
        .filter { it.extension != "srcjar" }
        .onEach { if (it.notExists()) logNonExistingFile(it, target.id) }
        .map { SourceItem(path = it, generated = true, jvmPackagePrefix = languagePlugin.calculateJvmPackagePrefix(it)) }

    return sources + generatedSources
  }

  private fun logNonExistingFile(file: Path, targetId: String) {
    val message = "target $targetId: $file does not exist."
    logger.warn(message)
  }

  private fun resolveResources(target: TargetInfo, languagePlugin: LanguagePlugin<*>): Set<Path> =
    (bazelPathsResolver.resolvePaths(target.resourcesList) + languagePlugin.resolveAdditionalResources(target))
      .toSet()

  private fun environmentItem(target: TargetInfo): Map<String, String> {
    val inheritedEnvs = collectInheritedEnvs(target)
    val targetEnv = target.envMap
    return inheritedEnvs + targetEnv
  }

  private fun collectInheritedEnvs(targetInfo: TargetInfo): Map<String, String> =
    targetInfo.envInheritList.associateWith { environmentProvider.getValue(it) ?: "" }

  private fun removeDotBazelBspTarget(targets: Collection<Label>): Collection<Label> =
    targets.filter {
      it.isMainWorkspace && !it.packagePath.toString().startsWith(".bazelbsp")
    }
}
