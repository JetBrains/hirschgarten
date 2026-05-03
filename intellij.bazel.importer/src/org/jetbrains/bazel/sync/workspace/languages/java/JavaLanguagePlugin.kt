package org.jetbrains.bazel.sync.workspace.languages.java

import com.google.common.hash.Hashing
import com.google.devtools.build.lib.view.proto.Deps
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.util.EnvironmentUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.LocalRepositoryMapping
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.commons.getLocalRepositories
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.info.BspTargetInfo.ArtifactLocation
import org.jetbrains.bazel.info.BspTargetInfo.JvmTargetInfo
import org.jetbrains.bazel.info.BspTargetInfo.TargetInfo
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.label.label
import org.jetbrains.bazel.label.toDependencyLabel
import org.jetbrains.bazel.performance.measure
import org.jetbrains.bazel.server.model.generatedSourcesList
import org.jetbrains.bazel.server.model.sourcesList
import org.jetbrains.bazel.sync.workspace.graph.DependencyGraph
import org.jetbrains.bazel.sync.workspace.languages.LanguagePlugin
import org.jetbrains.bazel.sync.workspace.mapper.normal.MavenCoordinatesResolver
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.BazelServerFacade
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.JvmBuildTarget
import org.jetbrains.bsp.protocol.JvmDependency
import org.jetbrains.bsp.protocol.LibraryItem
import org.jetbrains.bsp.protocol.MavenCoordinates
import org.jetbrains.bsp.protocol.allJars
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.inputStream
import kotlin.io.path.name

@ApiStatus.Internal
interface JvmLanguagePluginMixin {
  fun getSupportedLanguages(): Set<LanguageClass>
  fun createProjectMapper(project: Project, server: BazelServerFacade): Mapper

  interface Mapper {
    suspend fun prepareSync(
      graph: DependencyGraph,
      targetsToImport: Map<Label, TargetInfo>,
      repoMapping: RepoMapping,
    ) {}

    suspend fun toolchainLibraries(
      targetsToImport: Map<Label, TargetInfo>,
      repoMapping: RepoMapping
    ): Map<Label, List<LibraryItem>> = emptyMap()

    suspend fun createBuildTargetData(
      target: TargetInfo,
      targetsToImport: Map<Label, TargetInfo>,
      graph: DependencyGraph,
      repoMapping: RepoMapping,
    ): List<BuildTargetData> = emptyList()
  }

  companion object {
    val EP = ExtensionPointName<JvmLanguagePluginMixin>("org.jetbrains.bazel.jvmLanguagePlugin")
    val mixins = EP.extensionList
  }
}

@ApiStatus.Internal
class JavaLanguagePlugin: LanguagePlugin {
  override fun getSupportedLanguages(): Set<LanguageClass> = setOf(LanguageClass.JAVA) + JvmLanguagePluginMixin.mixins.flatMap { it.getSupportedLanguages() }
  override fun createProjectMapper(project: Project, server: BazelServerFacade) = Mapper(project, server)

  class Mapper(private val project: Project, private val server: BazelServerFacade) : LanguagePlugin.Mapper {
    private val mixins: List<JvmLanguagePluginMixin.Mapper> = JvmLanguagePluginMixin.mixins.map { it.createProjectMapper(project, server) }

    private val bazelPathsResolver = server.bazelPathsResolver
    private val jdkResolver = JdkResolver(bazelPathsResolver)
    private val outFilesHardLink = server.outFileHardLinks

    private var jdk: Jdk? = null
    private var toolchainTargets: Map<Label, TargetInfo> = mapOf()
    private var extraLibDependencies: Map<Label, List<DependencyLabel>> = mapOf()
    private var toolchainDependencies: Map<Label, List<DependencyLabel>> = mapOf()
    private var allLibraries: Map<Label, List<LibraryItem>> = mapOf()

    override suspend fun prepareSync(
      graph: DependencyGraph,
      targetsToImport: Map<Label, TargetInfo>,
      repoMapping: RepoMapping,
    ) {
      mixins.forEach { it.prepareSync(graph, targetsToImport, repoMapping) }

      toolchainTargets = graph.idToTargetInfo.filter { it.value.hasJavaToolchainInfo() }
      val ideJavaHomeOverride = server.workspaceContext.ideJavaHomeOverride
      jdk = ideJavaHomeOverride?.let { Jdk(javaHome = it) } ?: jdkResolver.resolve(graph.idToTargetInfo, repoMapping)

      calculateAllLibraries(
        graph,
        targetsToImport.filterValues { it.javaCommon.jvmTarget },
        repoMapping,
      )
    }

    override suspend fun createBuildTargetData(
      target: TargetInfo,
      targetsToImport: Map<Label, TargetInfo>,
      graph: DependencyGraph,
      repoMapping: RepoMapping,
    ): List<BuildTargetData> {
      return mixins.flatMap { it.createBuildTargetData(target, targetsToImport, graph, repoMapping) } +
             listOfNotNull(createJvmBuildTargetData(target, repoMapping))
    }

    suspend fun createJvmBuildTargetData(
      target: TargetInfo,
      repoMapping: RepoMapping,
    ): JvmBuildTarget? {
      if (!target.javaCommon.jvmTarget) {
        return null
      }
      val localRepositories = repoMapping.getLocalRepositories()
      val jvmTarget = target.jvmTargetInfo
      val binaryOutputs = target.javaCommon.jarsList.flatMap { it.binaryJarsList }.map { bazelPathsResolver.resolve(it, localRepositories) }
      val mainClass = getMainClass(jvmTarget)

      val jdk = jdk ?: return null
      val javaVersion = javaVersionFromJavacOpts(target.javaCommon.javacOptsList) ?: javaVersionFromToolchain(target)
      val javaHome = jdk.javaHome
      val environmentVariables =
        target.envMap + target.envInheritList.associateWith { EnvironmentUtil.getValue(it) ?: "" }

      val label = target.label()
      val targetLibraries = allLibraries[label].orEmpty().associateBy { it.id }

      return JvmBuildTarget(
        javaVersion = javaVersion.orEmpty(),
        javaHome = javaHome,
        javacOpts = target.javaCommon.javacOptsList,
        binaryOutputs = binaryOutputs,
        environmentVariables = environmentVariables,
        mainClass = mainClass,
        jvmArgs = jvmTarget.jvmFlagsList,
        programArgs = jvmTarget.argsList,
        resolvedResourceStripPrefix = target.resolveResourceStripPrefixToAbsolutePath(localRepositories),
        libraries = targetLibraries.values.map { it.hardLink() },
        // https://youtrack.jetbrains.com/issue/BAZEL-983
        // extra libraries can override some library versions, so they should be put before
        jvmDependencies =
          extraLibDependencies[label].orEmpty().map { JvmDependency.LibraryDependency(it) } +
          target.depsList.map {
            if (targetLibraries.containsKey(it.label()))
              JvmDependency.LibraryDependency(it.toDependencyLabel())
            else
              JvmDependency.ModuleDependency(it.toDependencyLabel())
          } +
          toolchainDependencies[label].orEmpty().map { JvmDependency.LibraryDependency(it) },
      )
    }

    private suspend fun calculateAllLibraries(
      graph: DependencyGraph,
      targetsToImport: Map<Label, TargetInfo>,
      repoMapping: RepoMapping,
    ) {
      val localRepositories = repoMapping.getLocalRepositories()

      val importDependenciesAsLibraries: Map<Label, List<LibraryItem>> =
        targetsToImport.mapValues { (_, target) ->
          target.depsList
            .mapNotNull { dependency ->
              val label = dependency.label()
              if (targetsToImport.containsKey(label))
                return@mapNotNull null // Dependency target is imported, no need to create library

              val libTargetInfo = graph.idToTargetInfo[label]
                                  ?: return@mapNotNull null

              createLibrary(
                server.workspaceContext,
                label,
                libTargetInfo,
                onlyOutputJars = false,
                localRepositories,
              )
            }
        }

      val interfacesAndBinariesFromTargetsToImport: Map<Label, Set<Path>> =
        measure("Collect interfaces and classes from targets to import") {
          collectInterfacesAndClasses(targetsToImport.values, repoMapping)
        }
      val outputJarsLibraries: Map<Label, List<LibraryItem>> =
        measure("Create output jars libraries") {
          calculateOutputJarsLibraries(server.workspaceContext, targetsToImport.values, graph.idToTargetInfo, repoMapping)
        }
      val annotationProcessorLibraries: Map<Label, List<LibraryItem>> =
        measure("Create AP libraries") {
          annotationProcessorLibraries(targetsToImport.values, repoMapping)
        }

      val librariesFromToolchains: Map<Label, List<LibraryItem>> =
        measure("Create toolchain libraries") {
          calculateToolchainLibraries(targetsToImport, repoMapping)
        }

      val librariesFromDeps: Map<Label, List<LibraryItem>> =
        measure("Merge libraries from deps") {
          concatenateMaps(listOf(outputJarsLibraries, annotationProcessorLibraries))
        }

      val librariesFromDepsAndTargets: Map<Label, List<LibraryItem>> =
        measure("Libraries from targets and deps") {
          concatenateMaps(listOf(librariesFromDeps, librariesFromToolchains, importDependenciesAsLibraries))
        }

      val extraLibrariesFromJdeps: Map<Label, List<LibraryItem>> =
        measure("Libraries from jdeps") {
          jdepsLibraries(
            targetsToImport,
            librariesFromDepsAndTargets,
            interfacesAndBinariesFromTargetsToImport,
            repoMapping,
          )
        }

      val extraLibraries = concatenateMaps(listOf(librariesFromDeps, extraLibrariesFromJdeps))

      extraLibDependencies = extraLibraries.mapValues { (_, libs) -> libs.map { DependencyLabel(it.id, exported = true) } }
      toolchainDependencies = librariesFromToolchains.mapValues { (_, libs) -> libs.map { DependencyLabel(it.id) } }
      allLibraries = concatenateMaps(listOf(importDependenciesAsLibraries, extraLibraries, librariesFromToolchains))
    }

    suspend fun calculateToolchainLibraries(
      targetsToImport: Map<Label, TargetInfo>,
      repoMapping: RepoMapping,
    ): Map<Label, List<LibraryItem>> {
      return concatenateMaps(mixins.map { it.toolchainLibraries(targetsToImport, repoMapping) })
    }

    private fun getMainClass(jvmTargetInfo: JvmTargetInfo): String? =
      jvmTargetInfo.mainClass.takeUnless { jvmTargetInfo.mainClass.isBlank() }

    private fun toolchainInfo(target: TargetInfo): BspTargetInfo.JavaToolchainInfo? {
      if (target.hasJavaToolchainInfo()) return target.javaToolchainInfo
      return target.depsList.asSequence()
        .filter { it.dependencyTypeValue == BspTargetInfo.Dependency.DependencyType.TOOLCHAIN_VALUE }
        .mapNotNull { Label.parseOrNull(it.target.label) }
        .mapNotNull { toolchainTargets[it] }
        .firstOrNull { it.hasJavaToolchainInfo() }
        ?.javaToolchainInfo
    }

    private fun javaVersionFromToolchain(target: TargetInfo): String? = toolchainInfo(target)?.sourceVersion

    private fun javaVersionFromJavacOpts(javacOpts: List<String>): String? {
      for (i in javacOpts.indices) {
        val option = javacOpts[i]
        val flagName = option.substringBefore(' ', missingDelimiterValue = option)
        val argument = option.substringAfter(' ', missingDelimiterValue = "")
        if (flagName == "-target" || flagName == "--target" || flagName == "--release") {
          if (argument.isNotBlank()) return argument
          return javacOpts.getOrNull(i + 1)
        }
      }
      return null
    }

    private fun hasKnownJvmSources(target: TargetInfo): Boolean =
      target.sourcesList.any {
        it.relativePath.endsWith(".java") ||
        it.relativePath.endsWith(".kt") ||
        it.relativePath.endsWith(".scala")
      }

    fun shouldCreateOutputJarsLibrary(targetInfo: TargetInfo, allTargets : Map<Label, TargetInfo>) =
      !targetInfo.kind.endsWith("_resources") && targetInfo.javaCommon.jvmTarget &&
      (
        targetInfo.generatedSourcesList.any { it.relativePath.endsWith(".srcjar") } ||
        (targetInfo.sourcesList.any() && !hasKnownJvmSources(targetInfo)) ||
        (targetInfo.sourcesList.none() && targetInfo.kind !in workspaceTargetKinds && !targetInfo.executable) ||
        targetInfo.javaProvider.hasApiGeneratingPlugins ||
        targetInfo.kotlinTargetInfo.exportedCompilerPluginTargetsFromDepsList.any { allTargets.get(Label.parse(it))?.javaProvider?.hasApiGeneratingPlugins ?: false }
      )

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
        "_resourcegroup_jps",
        "scala_library",
        "scala_binary",
        "scala_test",
        "intellij_plugin_debug_target",
      )

    private fun calculateOutputJarsLibraries(
      workspaceContext: WorkspaceContext,
      targetsToImport: Collection<TargetInfo>,
      allTargets: Map<Label, TargetInfo>,
      repoMapping: RepoMapping,
    ): Map<Label, List<LibraryItem>> {
      val localRepositories = repoMapping.getLocalRepositories()
      return targetsToImport
        .filter { shouldCreateOutputJarsLibrary(it, allTargets) }
        .mapNotNull { target ->
          createLibrary(
            workspaceContext,
            target.label(), // Label.parse(target.key.label+ OUTPUT_JARS_SUFFIX),
            target,
            onlyOutputJars = true,
            localRepositories
          )?.let { library ->
            target.label() to listOf(library)
          }
        }.toMap()
    }

    private fun annotationProcessorLibraries(targetsToImport: Collection<TargetInfo>, repoMapping: RepoMapping): Map<Label, List<LibraryItem>> {
      val localRepositories = repoMapping.getLocalRepositories()
      return targetsToImport
        .filter { it.javaCommon.generatedJarsList.isNotEmpty() }
        .associate { targetInfo ->
          targetInfo.key.label to
            createLibrary(
              id = Label.synthetic(targetInfo.key.label + "_generated"),
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
      interfacesAndBinariesFromTargetsToImport: Map<Label, Set<Path>>,
      repoMapping: RepoMapping,
    ): Map<Label, List<LibraryItem>> {
      val localRepositories = repoMapping.getLocalRepositories()
      val targetsToJdepsJars =
        getAllJdepsDependencies(targetsToImport, libraryDependencies, localRepositories)
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
      localRepositories: LocalRepositoryMapping,
    ): Map<Label, Set<Path>> {
      val jdepsJars =
        withContext(Dispatchers.IO) {
          targetsToImport
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
                  outputJarsFromTransitiveDepsCache,
                  allJdepsJars,
                  localRepositories,
                  visited = hashSetOf()
                )
              targetLabel to jarsFromJdeps - transitiveJdepsJars
            }
          }.awaitAll()
          .toMap()
          .filterValues { it.isNotEmpty() }
      }
    }

    private fun getJdepsJarsFromTransitiveDependencies(
      target: Label,
      targetsToImport: Map<Label, TargetInfo>,
      libraryDependencies: Map<Label, List<LibraryItem>>,
      outputJarsFromTransitiveDepsCache: ConcurrentHashMap<Label, Set<Path>>,
      allJdepsJars: Set<Path>,
      localRepositories : LocalRepositoryMapping,
      visited: MutableSet<Label>
    ): Set<Path> {
      val cached = outputJarsFromTransitiveDepsCache[target]
      if (cached != null)
        return cached

      if (!visited.add(target)) // prevent STOFL if dependency cycle
        return emptySet()

      val jarsFromTargets =
        targetsToImport[target]?.let { getTargetOutputJarsSet(it, localRepositories) + getTargetInterfaceJarsSet(it, localRepositories) }
          .orEmpty()
      val outputJars: MutableSet<Path> =
        (jarsFromTargets + libraryDependencies[target].orEmpty().flatMap { it.allJars })
          .filter { it in allJdepsJars }
          .toMutableSet()

      val dependencies =
        targetsToImport[target]?.depsList.orEmpty().map { it.label() } +
        libraryDependencies[target].orEmpty().filter { it.id != target }.map { it.id }

      dependencies.flatMapTo(outputJars) { dependency ->
        getJdepsJarsFromTransitiveDependencies(
          dependency,
          targetsToImport,
          libraryDependencies,
          outputJarsFromTransitiveDepsCache,
          allJdepsJars,
          localRepositories,
          visited,
        )
      }

      outputJarsFromTransitiveDepsCache[target] = outputJars
      return outputJars
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

    private fun TargetInfo.containsAnyInternalJars(localRepositories : LocalRepositoryMapping) = javaCommon.jarsList.any { jars ->
      jars.sourceJarsList.any { !bazelPathsResolver.isExternal(it, localRepositories) } && jars.binaryJarsList.any { !bazelPathsResolver.isExternal(it, localRepositories) }
    }

    private fun createLibrary(
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
            MavenCoordinatesResolver.resolveMavenCoordinates(label, outputJar)
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

    private fun createLibrary(
      id: Label,
      dependencies: List<DependencyLabel>,
      ijars: Set<Path> = emptySet(),
      jars: Set<Path>,
      sourceJars: Set<Path>,
      mavenCoordinates: MavenCoordinates? = null,
      containsInternalJars: Boolean = false,
    ): LibraryItem {
      return LibraryItem(
        id = id,
        dependencies = dependencies,
        ijars = ijars.toList(),
        jars = jars.toList(),
        sourceJars = sourceJars.toList(),
        mavenCoordinates = mavenCoordinates,
        containsInternalJars = containsInternalJars,
      )
    }

    private suspend fun LibraryItem.hardLink(): LibraryItem =
      this.copy(
        ijars = outFilesHardLink.createOutputFileHardLinks(ijars),
        jars = outFilesHardLink.createOutputFileHardLinks(jars),
        sourceJars = outFilesHardLink.createOutputFileHardLinks(sourceJars),
      )

    private fun shouldCreateLibrary(
      dependencies: List<BspTargetInfo.Dependency>,
      outputs: Collection<Path>,
      interfaceJars: Collection<Path>,
      sources: Collection<Path>,
    ): Boolean = dependencies.isNotEmpty() || !outputs.isEmptyJarList() || !interfaceJars.isEmptyJarList() || !sources.isEmptyJarList()

    private fun Collection<Path>.isEmptyJarList(): Boolean = isEmpty() || singleOrNull()?.name == "empty.jar"

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


    private fun collectInterfacesAndClasses(targets: Collection<TargetInfo>, repoMapping: RepoMapping): Map<Label, Set<Path>> {
      val localRepositories = repoMapping.getLocalRepositories()
      return targets.associate { target ->
        target.label() to
          (getTargetInterfaceJarsList(target, localRepositories) + getTargetOutputJarsList(target, localRepositories))
            .toSet()
      }
    }

    private fun List<ArtifactLocation>.resolvePaths(localRepositories : LocalRepositoryMapping) =
      map { bazelPathsResolver.resolve(it, localRepositories) }.toSet()

    // TODO BAZEL-2208
    // The only language that supports strict deps by default is Java, in Kotlin and Scala strict deps are disabled by default.
    private fun targetSupportsStrictDeps(target: TargetInfo): Boolean =
      target.javaCommon.jvmTarget && !target.hasScalaTargetInfo() && !target.hasKotlinTargetInfo()

    private fun TargetInfo.resolveResourceStripPrefixToAbsolutePath(repositories: LocalRepositoryMapping): Path? {
      if (!hasJvmTargetInfo()) return null
      val prefix = jvmTargetInfo.resourceStripPrefix.ifEmpty { null } ?: return null
      val workspaceRoot = bazelPathsResolver.workspaceRoot()
      val repoPath = when (val label = label()) {
        is ResolvedLabel -> repositories.localRepositories[label.repoName]?.let(workspaceRoot::resolve) ?: workspaceRoot
        else -> workspaceRoot
      }
      return repoPath.resolve(prefix)
    }
  }

  companion object {
    const val OUTPUT_JARS_SUFFIX = "_output_jars"
  }
}

private fun <K, V> concatenateMaps(maps: Collection<Map<K, List<V>>>): Map<K, List<V>> =
  maps
    .flatMap { it.keys }
    .distinct()
    .associateWith { key ->
      maps.flatMap { it[key].orEmpty() }
    }
