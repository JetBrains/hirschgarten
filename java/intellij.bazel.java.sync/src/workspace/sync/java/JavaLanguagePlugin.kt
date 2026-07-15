package org.jetbrains.bazel.sync.workspace.languages.java

import com.google.devtools.build.lib.view.proto.Deps
import com.google.devtools.intellij.aspect.Common.ArtifactLocation
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.JvmTargetInfo
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.TargetIdeInfo
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.util.EnvironmentUtil
import com.intellij.util.io.DigestUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.LanguageClassService
import org.jetbrains.bazel.commons.LocalRepositoryMapping
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.commons.getLocalRepositories
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.label.DependencyLabelKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.label.assumeResolved
import org.jetbrains.bazel.label.label
import org.jetbrains.bazel.label.toDependencyLabel
import org.jetbrains.bazel.languages.projectview.importIjars
import org.jetbrains.bazel.languages.projectview.projectView
import org.jetbrains.bazel.languages.projectview.testSources
import org.jetbrains.bazel.performance.measure
import org.jetbrains.bazel.server.BazelServerFacade
import org.jetbrains.bazel.server.model.generatedSourcesList
import org.jetbrains.bazel.server.model.sourcesList
import org.jetbrains.bazel.sync.JavaLanguageClass
import org.jetbrains.bazel.sync.workspace.graph.DependencyGraph
import org.jetbrains.bazel.sync.workspace.languages.LanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot.SourceRootOptimizationMode
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmDependency
import org.jetbrains.bazel.sync.workspace.mapper.normal.MavenCoordinatesResolver
import org.jetbrains.bazel.sync.workspace.snapshot.SourceFileCollectionBuilder
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSyncConfig
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.sync.workspace.snapshot.toWorkspaceTargetKey
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.LibraryItem
import org.jetbrains.bsp.protocol.MavenCoordinates
import org.jetbrains.bsp.protocol.StrictDependencyCheckedType
import org.jetbrains.bsp.protocol.allJars
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.inputStream
import kotlin.io.path.name
import kotlin.io.path.relativeToOrSelf
import kotlin.reflect.KClass

@ApiStatus.Internal
interface JvmLanguagePluginMixin {
  val providedBuildTargetTypes: Set<KClass<out BuildTargetData>>

  fun getSupportedLanguages(): Set<LanguageClass>
  fun collectUsedLanguages(target: TargetIdeInfo): List<LanguageClass>
  fun createProjectMapper(project: Project, server: BazelServerFacade): Mapper

  /**
   * @see [LanguagePlugin.createSyncConfigs]
   */
  suspend fun createSyncConfigs(project: Project, workspaceContext: WorkspaceContext): List<WorkspaceSyncConfig> = listOf()

  interface Mapper {
    suspend fun prepareSync(
      graph: DependencyGraph,
      targetsToImport: Map<WorkspaceTargetKey, TargetIdeInfo>,
      repoMapping: RepoMapping,
    ) {
    }

    suspend fun toolchainLibraries(
      targetsToImport: Map<WorkspaceTargetKey, TargetIdeInfo>,
      repoMapping: RepoMapping,
    ): Map<WorkspaceTargetKey, List<LibraryItem>> = emptyMap()

    suspend fun createBuildTargetData(
      target: TargetIdeInfo,
      targetsToImport: Map<WorkspaceTargetKey, TargetIdeInfo>,
      repoMapping: RepoMapping,
    ): List<BuildTargetData> = emptyList()
  }

  companion object {
    val EP = ExtensionPointName<JvmLanguagePluginMixin>("org.jetbrains.bazel.jvmLanguagePlugin")
    val mixins = EP.extensionList
  }
}

private typealias DependencyLabelPatcher = (DependencyLabel) -> DependencyLabel

@ApiStatus.Internal
class JavaLanguagePlugin : LanguagePlugin {
  override val providedBuildTargetTypes: Set<KClass<out BuildTargetData>>
    get() = setOf(JvmBuildTarget::class) + JvmLanguagePluginMixin.mixins.flatMap { it.providedBuildTargetTypes }.toSet()

  override fun getSupportedLanguages(): Set<LanguageClass> =
    setOf(JavaLanguageClass.JAVA) + JvmLanguagePluginMixin.mixins.flatMap { it.getSupportedLanguages() }

  override fun collectUsedLanguages(target: TargetIdeInfo): List<LanguageClass> {
    return JvmLanguagePluginMixin.mixins.flatMap { it.collectUsedLanguages(target) } +
           (if (target.javaCommon.jvmTarget) listOf(JavaLanguageClass.JAVA) else emptyList())
  }


  override fun createProjectMapper(project: Project, server: BazelServerFacade): Mapper = Mapper(project, server)
  override suspend fun createSyncConfigs(project: Project, workspaceContext: WorkspaceContext): List<WorkspaceSyncConfig> {
    return JvmLanguagePluginMixin.mixins.flatMap { it.createSyncConfigs(project, workspaceContext) } +
           JavaWorkspaceSyncConfig(
             testSourcesPatterns = project.projectView().testSources,

             // RC: we bypass `WorkspaceContext` here completely
             importIjars = project.projectView().importIjars,

             // RC: as you can see we pass `SourceRootOptimizationMode` as `WorkspaceSyncConfig`
             //  property, so can compare it against previous snapshot and assess whatever it has changes
             //  thus performing automatic full importer invalidation
             sourceRootOptimizationMode = SourceRootOptimizationMode.createFromProject(project),
             excludeCompiledSourceCodeInsideJars = BazelFeatureFlags.excludeCompiledSourceCodeInsideJars,
           )
  }

  inner class Mapper(private val project: Project, private val server: BazelServerFacade) : LanguagePlugin.Mapper {
    private val mixins: List<JvmLanguagePluginMixin.Mapper> = JvmLanguagePluginMixin.mixins.map { it.createProjectMapper(project, server) }

    private val bazelPathsResolver = server.bazelPathsResolver
    private val jdkResolver = JdkResolver(bazelPathsResolver)
    private val outFilesHardLink = server.outFileHardLinks

    private var jdk: Jdk? = null
    private var toolchainTargets: Map<WorkspaceTargetKey, TargetIdeInfo> = mapOf()
    private var extraLibDependencies: Map<WorkspaceTargetKey, List<DependencyLabel>> = mapOf()
    private var interfacesAndBinaries: Map<WorkspaceTargetKey, Set<Path>> = mapOf()
    private var toolchainDependencies: Map<WorkspaceTargetKey, List<DependencyLabel>> = mapOf()
    private var allLibraries: Map<WorkspaceTargetKey, List<LibraryItem>> = mapOf()

    private lateinit var repoMapping: RepoMapping
    private lateinit var allTargets: Map<WorkspaceTargetKey, TargetIdeInfo>

    // keep output jar label identity
    private lateinit var outputJarsByLabel: Map<Label, Set<Path>>

    override val langPlugin: LanguagePlugin
      get() = this@JavaLanguagePlugin

    override suspend fun prepareSync(
      graph: DependencyGraph,
      targetsToImport: Map<WorkspaceTargetKey, TargetIdeInfo>,
      repoMapping: RepoMapping,
    ) {
      this.repoMapping = repoMapping
      this.allTargets = graph.idToTargetInfo

      val localRepositories = repoMapping.getLocalRepositories()
      outputJarsByLabel = buildMap<Label, MutableSet<Path>> {
        for ((key, info) in allTargets) {
          val jars = info.javaCommon.jarsList.flatMap { it.binaryJarsList }
            .map { bazelPathsResolver.resolve(it, localRepositories) }
          if (jars.isNotEmpty()) {
            getOrPut(key.label) { mutableSetOf() } += jars
          }
        }
      }

      mixins.forEach { it.prepareSync(graph, targetsToImport, repoMapping) }

      toolchainTargets = graph.idToTargetInfo.filter { it.value.hasJavaToolchainInfo() }
      val ideJavaHomeOverride = server.workspaceContext.ideJavaHomeOverride
      jdk = ideJavaHomeOverride?.let { Jdk(javaHome = it) } ?: jdkResolver.resolve(graph.idToTargetInfo, repoMapping)

      calculateAllLibraries(targetsToImport = targetsToImport.filterValues { it.javaCommon.jvmTarget })
    }

    override suspend fun createBuildTargetData(
      target: TargetIdeInfo,
      targetsToImport: Map<WorkspaceTargetKey, TargetIdeInfo>,
      graph: DependencyGraph,
      repoMapping: RepoMapping,
    ): List<BuildTargetData> {
      return mixins.flatMap { it.createBuildTargetData(target, targetsToImport, repoMapping) } +
             listOfNotNull(createJvmBuildTargetData(target, repoMapping))
    }

    suspend fun createJvmBuildTargetData(
      target: TargetIdeInfo,
      repoMapping: RepoMapping,
    ): JvmBuildTarget? {
      if (!target.javaCommon.jvmTarget) {
        return null
      }
      val baseDirectory = server.bazelPathsResolver.toDirectoryPath(target.label().assumeResolved(), repoMapping)
      val localRepositories = repoMapping.getLocalRepositories()
      val jvmTarget = target.jvmTargetInfo
      val binaryOutputs = target.javaCommon.jarsList.flatMap { it.binaryJarsList }.map { bazelPathsResolver.resolve(it, localRepositories) }
      val mainClass = getMainClass(jvmTarget)

      val jdk = jdk ?: return null
      val javaVersion = javaVersionFromJavacOpts(target.javaCommon.javacOptsList) ?: javaVersionFromToolchain(target)
      val javaHome = jdk.javaHome
      val environmentVariables =
        target.envMap + target.envInheritList.associateWith { EnvironmentUtil.getValue(it) ?: "" }

      val targetKey = target.key.toWorkspaceTargetKey()
      val targetLibraries = allLibraries[targetKey].orEmpty().associateBy { it.key }

      return JvmBuildTarget(
        javaVersion = javaVersion.orEmpty(),
        javaHome = javaHome,
        javacOpts = target.javaCommon.javacOptsList,
        binaryOutputs = SourceFileCollectionBuilder.build(relativeRoot = baseDirectory, paths = binaryOutputs),
        environmentVariables = environmentVariables,
        mainClass = mainClass,
        jvmArgs = jvmTarget.jvmFlagsList,
        programArgs = jvmTarget.argsList,
        resolvedResourceStripPrefix = target.resolveResourceStripPrefixToAbsolutePath(localRepositories),
        libraries = targetLibraries.values.map { it.hardLink() },
        outputJars = outFilesHardLink.createOutputFileHardLinks(interfacesAndBinaries[targetKey].orEmpty()).toSet(),
        // https://youtrack.jetbrains.com/issue/BAZEL-983
        // extra libraries can override some library versions, so they should be put before
        jvmDependencies =
          extraLibDependencies[targetKey].orEmpty().map { JvmDependency.LibraryDependency(it) } +
          target.dependencies().map {
            if (targetLibraries.containsKey(it.targetKey))
              JvmDependency.LibraryDependency(it)
            else
              JvmDependency.ModuleDependency(it)
          } +
          toolchainDependencies[targetKey].orEmpty().map { JvmDependency.LibraryDependency(it) },
        checkStrictDependencies = targetChecksStrictDeps(target),
      )
    }

    private val dependenciesCache = ConcurrentHashMap<WorkspaceTargetKey, List<DependencyLabel>>()

    private fun TargetIdeInfo.dependencies(): List<DependencyLabel> {
      val target = this
      return dependenciesCache.computeIfAbsent(target.key.toWorkspaceTargetKey()) F@{
        // Well-known targets which include generated libraries as dependencies.
        // They must be exported, but this is not returned from aspects:
        // https://bazel.build/reference/be/protocol-buffer#proto_library_args
        if (target.kind == "java_proto_library") {
          return@F target.depsList.map {
            it.toDependencyLabel().copy(kind = DependencyLabelKind.EXPORTED_COMPILE_TIME)
          }
        }

        // Scala proto libraries do not have meaningful dependencies despite somethign might return from aspects
        // The entire transitive closure of libraries, which is used by compiler,
        // is returned in targetInfo.javaProvider.fullCompileJarsList
        if (target.kind == "scala_proto_library") {
          return@F emptyList()
        }

        // https://youtrack.jetbrains.com/issue/BAZEL-3218
        // Some custom rules declare the dependent output jar as its own,
        // thus making the dependency effectively exported
        val exportByOutputJarsPatcher: DependencyLabelPatcher =
          if (target.kind in wellKnownTargetKinds) {
            // Well known rules have well known behavior
            { it }
          }
          else {
            val localRepositories = repoMapping.getLocalRepositories()

            fun TargetIdeInfo.outputJars(): Set<Path> = this.javaCommon.jarsList.flatMap { it.binaryJarsList }
              .map { bazelPathsResolver.resolve(it, localRepositories) }
              .toSet()

            val outputJars = target.outputJars();
            DependencyLabelPatcher@{ dependency ->
              // match by label so a dependency aspect-shadow output jars
              val depJars = outputJarsByLabel[dependency.targetKey.label] ?: emptySet()
              if (outputJars.intersect(depJars).isNotEmpty())
                return@DependencyLabelPatcher dependency.copy(kind = DependencyLabelKind.EXPORTED_COMPILE_TIME)

              dependency
            }
          }

        return@F target.depsList.map {
          exportByOutputJarsPatcher(it.toDependencyLabel())
        }
      }
    }

    private suspend fun calculateAllLibraries(
      targetsToImport: Map<WorkspaceTargetKey, TargetIdeInfo>,
    ) {
      val localRepositories = repoMapping.getLocalRepositories()
      // Avoid creating the same LibraryItem instance several times to avoid O(N^2) (BAZEL-3203)
      val libraryItemByIdCache = hashMapOf<WorkspaceTargetKey, Ref<LibraryItem?>>()

      val importDependenciesAsLibraries: Map<WorkspaceTargetKey, List<LibraryItem>> =
        targetsToImport.mapValues { (_, target) ->
          target.dependencies()
            .mapNotNull { dependency ->
              val depKey = dependency.targetKey
              if (targetsToImport.containsKey(depKey))
                return@mapNotNull null // Dependency target is imported, no need to create library

              val libTargetInfo = allTargets[depKey]
                                  ?: return@mapNotNull null

              libraryItemByIdCache.getOrPut(depKey) {
                Ref(
                  createLibrary(
                    server.workspaceContext,
                    depKey,
                    libTargetInfo,
                    onlyOutputJars = false,
                    localRepositories,
                  ),
                )
              }.get()
            }
        }

      val interfacesAndBinariesFromTargetsToImport: Map<WorkspaceTargetKey, Set<Path>> =
        measure("Collect interfaces and classes from targets to import") {
          collectInterfacesAndClasses(targetsToImport.values)
        }
      // exposed via JvmBuildTarget.outputJars so the jdeps library-shadows-module index
      // can be built downstream in ImportContext/JvmTargetEntitiesBuilder
      interfacesAndBinaries = interfacesAndBinariesFromTargetsToImport
      val outputJarsLibraries: Map<WorkspaceTargetKey, List<LibraryItem>> =
        measure("Create output jars libraries") {
          calculateOutputJarsLibraries(server.workspaceContext, targetsToImport.values)
        }
      val annotationProcessorLibraries: Map<WorkspaceTargetKey, List<LibraryItem>> =
        measure("Create AP libraries") {
          annotationProcessorLibraries(targetsToImport.values)
        }

      val librariesFromToolchains: Map<WorkspaceTargetKey, List<LibraryItem>> =
        measure("Create toolchain libraries") {
          calculateToolchainLibraries(targetsToImport)
        }

      val librariesFromDeps: Map<WorkspaceTargetKey, List<LibraryItem>> =
        measure("Merge libraries from deps") {
          concatenateMaps(listOf(outputJarsLibraries, annotationProcessorLibraries))
        }

      val librariesFromDepsAndTargets: Map<WorkspaceTargetKey, List<LibraryItem>> =
        measure("Libraries from targets and deps") {
          concatenateMaps(listOf(librariesFromDeps, librariesFromToolchains, importDependenciesAsLibraries))
        }

      val extraLibrariesFromJdeps: Map<WorkspaceTargetKey, List<LibraryItem>> =
        measure("Libraries from jdeps") {
          jdepsLibraries(
            targetsToImport,
            librariesFromDepsAndTargets,
            interfacesAndBinariesFromTargetsToImport,
          )
        }

      val extraLibraries = concatenateMaps(listOf(librariesFromDeps, extraLibrariesFromJdeps))

      extraLibDependencies =
        extraLibraries.mapValues { (_, libs) ->
          libs.map {
            DependencyLabel(
              targetKey = it.key,
              kind = DependencyLabelKind.EXPORTED_COMPILE_TIME,
            )
          }
        }
      toolchainDependencies =
        librariesFromToolchains.mapValues { (_, libs) -> libs.map { DependencyLabel(targetKey = it.key) } }
      allLibraries = concatenateMaps(listOf(importDependenciesAsLibraries, extraLibraries, librariesFromToolchains))
    }

    private fun targetChecksStrictDeps(target: TargetIdeInfo): StrictDependencyCheckedType {
      // Special case, to be dropped shortly
      // Ultimate monorepo rules do not support strict deps
      if (target.kind == "jvm_library" || target.kind == "_jvm_library_jps")
        return StrictDependencyCheckedType.OFF

      // At the moment only java supports strict deps
      // https://blog.bazel.build/2017/06/28/sjd-unused_deps.html
      // TODO: support Kotlin strict deps
      // TODO: investigate scala
      val hasJavaSources = target.sourcesList.any {
        LanguageClassService.getInstance().fromPath(it.relativePath) == JavaLanguageClass.JAVA
      }
      if (!hasJavaSources)
        return StrictDependencyCheckedType.OFF

      if (target.hasKotlinTargetInfo() || target.hasScalaTargetInfo())
        return StrictDependencyCheckedType.WARNING

      return StrictDependencyCheckedType.ERROR
    }

    suspend fun calculateToolchainLibraries(
      targetsToImport: Map<WorkspaceTargetKey, TargetIdeInfo>,
    ): Map<WorkspaceTargetKey, List<LibraryItem>> {
      return concatenateMaps(mixins.map { it.toolchainLibraries(targetsToImport, repoMapping) })
    }

    private fun getMainClass(jvmTargetInfo: JvmTargetInfo): String? =
      jvmTargetInfo.mainClass.takeUnless { jvmTargetInfo.mainClass.isBlank() }

    private fun toolchainInfo(target: TargetIdeInfo): IntellijIdeInfo.JavaToolchainInfo? {
      if (target.hasJavaToolchainInfo()) return target.javaToolchainInfo
      return target.depsList.asSequence()
        .filter { it.dependencyTypeValue == IntellijIdeInfo.Dependency.DependencyType.TOOLCHAIN_VALUE }
        .mapNotNull { toolchainTargets[it.target.toWorkspaceTargetKey()] }
        .firstOrNull { it.hasJavaToolchainInfo() }
        ?.javaToolchainInfo
    }

    private fun javaVersionFromToolchain(target: TargetIdeInfo): String? = toolchainInfo(target)?.sourceVersion

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

    private fun hasKnownJvmSources(target: TargetIdeInfo): Boolean =
      target.sourcesList.any {
        it.relativePath.endsWith(".java") ||
        it.relativePath.endsWith(".kt") ||
        it.relativePath.endsWith(".scala")
      }

    private fun shouldCreateOutputJarsLibrary(targetInfo: TargetIdeInfo, allTargets: Map<WorkspaceTargetKey, TargetIdeInfo>): Boolean {
      // Resource-only targets and non-JVM targets never produce output jars worth indexing.
      if (targetInfo.kind.endsWith("_resources") || !targetInfo.javaCommon.jvmTarget) {
        return false
      }

      val hasGeneratedSrcJar = targetInfo.generatedSourcesList.any { it.relativePath.endsWith(".srcjar") }
      val hasOnlyNonJvmSources = targetInfo.sourcesList.any() && !hasKnownJvmSources(targetInfo)
      val isUnknownTargetWithoutSources =
        targetInfo.sourcesList.none() && targetInfo.kind !in wellKnownTargetKinds && !targetInfo.hasExecutableInfo()
      val hasApiGeneratingPlugins = targetInfo.javaProvider.hasApiGeneratingPlugins
      val dependsOnExportedApiGeneratingPlugins =
        targetInfo.kotlinTargetInfo.exportedCompilerPluginTargetsList.any {
          allTargets[it.toWorkspaceTargetKey()]?.javaProvider?.hasApiGeneratingPlugins ?: false
        }

      return hasGeneratedSrcJar ||
             hasOnlyNonJvmSources ||
             isUnknownTargetWithoutSources ||
             hasApiGeneratingPlugins ||
             dependsOnExportedApiGeneratingPlugins
    }

    private val wellKnownTargetKinds =
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
      targetsToImport: Collection<TargetIdeInfo>,
    ): Map<WorkspaceTargetKey, List<LibraryItem>> {
      val localRepositories = repoMapping.getLocalRepositories()
      return targetsToImport
        .filter { shouldCreateOutputJarsLibrary(it, allTargets) }
        .mapNotNull { target ->
          createLibrary(
            workspaceContext,
            target.key.toWorkspaceTargetKey(), // Label.parse(target.key.label+ OUTPUT_JARS_SUFFIX),
            target,
            onlyOutputJars = true,
            localRepositories,
          )?.let { library ->
            target.key.toWorkspaceTargetKey() to listOf(library)
          }
        }.toMap()
    }

    private fun annotationProcessorLibraries(targetsToImport: Collection<TargetIdeInfo>): Map<WorkspaceTargetKey, List<LibraryItem>> {
      val localRepositories = repoMapping.getLocalRepositories()
      return targetsToImport
        .filter { it.javaCommon.generatedJarsList.isNotEmpty() }
        .associate { targetInfo ->
          val libKey = targetInfo.key.toWorkspaceTargetKey()
          libKey to
            createLibrary(
              key = libKey.copy(label = Label.synthetic(targetInfo.key.label + "_generated")),
              ijars = emptySet(),
              jars = targetInfo.javaCommon.generatedJarsList
                .flatMap { it.binaryJarsList }
                .map { bazelPathsResolver.resolve(it, localRepositories) }
                .toSet(),
              sourceJars = targetInfo.javaCommon.generatedJarsList
                .flatMap { it.sourceJarsList }
                .map { bazelPathsResolver.resolve(it, localRepositories) }
                .toSet(),
            )
        }.mapValues { listOf(it.value) }
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
      targetsToImport: Map<WorkspaceTargetKey, TargetIdeInfo>,
      libraryDependencies: Map<WorkspaceTargetKey, List<LibraryItem>>,
      interfacesAndBinariesFromTargetsToImport: Map<WorkspaceTargetKey, Set<Path>>,
    ): Map<WorkspaceTargetKey, List<LibraryItem>> {
      val localRepositories = repoMapping.getLocalRepositories()
      val targetsToJdepsJars: Map<WorkspaceTargetKey, Set<Path>> =
        getAllJdepsDependencies(targetsToImport, libraryDependencies, localRepositories)
      val libraryNameToLibraryValueMap = HashMap<WorkspaceTargetKey, LibraryItem>()
      return targetsToJdepsJars.mapValues { target: Map.Entry<WorkspaceTargetKey, Set<Path>> ->
        val interfacesAndBinariesFromTarget =
          interfacesAndBinariesFromTargetsToImport.getOrDefault(target.key, emptySet())
        target.value
          .map { path: Path -> bazelPathsResolver.resolve(path) }
          .filter { it !in interfacesAndBinariesFromTarget }
          .mapNotNull {
            if (shouldSkipJdepsJar(it)) return@mapNotNull null
            val key = WorkspaceTargetKey(label = syntheticLabel(it))
            libraryNameToLibraryValueMap.getOrPut(key) {
              createLibrary(
                key = key,
                ijars = emptySet(),
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
      targetsToImport: Map<WorkspaceTargetKey, TargetIdeInfo>,
      libraryDependencies: Map<WorkspaceTargetKey, List<LibraryItem>>,
      localRepositories: LocalRepositoryMapping,
    ): Map<WorkspaceTargetKey, Set<Path>> {
      val jdepsJars =
        withContext(Dispatchers.IO) {
          targetsToImport
            .map { (key, target) ->
              async {
                key to dependencyJarsFromJdepsFiles(target, localRepositories)
              }
            }.awaitAll()
        }.filter { it.second.isNotEmpty() }.toMap()

      val allJdepsJars =
        jdepsJars.values
          .asSequence()
          .flatten()
          .toSet()

      return withContext(Dispatchers.Default) {
        val outputJarsFromTransitiveDepsCache = ConcurrentHashMap<WorkspaceTargetKey, Set<Path>>()
        jdepsJars
          .map { (targetKey, jarsFromJdeps) ->
            async {
              val transitiveJdepsJars =
                getJdepsJarsFromTransitiveDependencies(
                  targetKey,
                  targetsToImport,
                  libraryDependencies,
                  outputJarsFromTransitiveDepsCache,
                  allJdepsJars,
                  localRepositories,
                  visited = hashSetOf(),
                )
              targetKey to jarsFromJdeps - transitiveJdepsJars
            }
          }.awaitAll()
          .toMap()
          .filterValues { it.isNotEmpty() }
      }
    }

    private fun getJdepsJarsFromTransitiveDependencies(
      target: WorkspaceTargetKey,
      targetsToImport: Map<WorkspaceTargetKey, TargetIdeInfo>,
      libraryDependencies: Map<WorkspaceTargetKey, List<LibraryItem>>,
      outputJarsFromTransitiveDepsCache: ConcurrentHashMap<WorkspaceTargetKey, Set<Path>>,
      allJdepsJars: Set<Path>,
      localRepositories: LocalRepositoryMapping,
      visited: MutableSet<WorkspaceTargetKey>,
    ): Set<Path> {
      val cached = outputJarsFromTransitiveDepsCache[target]
      if (cached != null)
        return cached

      if (!visited.add(target)) // prevent STOFL if dependency cycle
        return emptySet()

      val jarsFromTargets =
        targetsToImport[target]?.let { getTargetOutputJarsList(it, localRepositories) + getTargetInterfaceJarsList(it, localRepositories) }
          .orEmpty()
      val outputJars: MutableSet<Path> =
        (jarsFromTargets + libraryDependencies[target].orEmpty().flatMap { it.allJars })
          .filter { it in allJdepsJars }
          .toMutableSet()

      val dependencies =
        targetsToImport[target]?.dependencies().orEmpty().map { it.targetKey } +
        libraryDependencies[target].orEmpty().filter { it.key != target }.map { it.key }

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

    private fun dependencyJarsFromJdepsFiles(targetInfo: TargetIdeInfo, localRepositories: LocalRepositoryMapping): Set<Path> =
      targetInfo.javaCommon.jdepsList
        .flatMap { jdeps ->
          val path = bazelPathsResolver.resolve(jdeps, localRepositories)
          if (path.exists()) {
            val dependencyList =
              path.inputStream().use {
                Deps.Dependencies.parseFrom(it).dependencyList
              }
            dependencyList
              .asSequence()
              .filter { it.isRelevant() }
              .map { bazelPathsResolver.resolveOutput(Paths.get(it.path)) }
              .toList()
          }
          else {
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
      val relativeLibPath = lib.relativeToOrSelf(bazelPathsResolver.bazelBin())
      val shaOfPath = DigestUtil.sha1Hex(relativeLibPath.toString()).take(7) // just in case of a conflict in filename
      return Label.synthetic(
        lib
          .fileName
          .toString()
          .replace(replacementRegex, "-") + "-" + shaOfPath,
      )
    }

    private fun TargetIdeInfo.containsAnyInternalJars(localRepositories: LocalRepositoryMapping) = javaCommon.jarsList.any { jars ->
      jars.sourceJarsList.any {
        !bazelPathsResolver.isExternal(
          it,
          localRepositories,
        )
      } && jars.binaryJarsList.any { !bazelPathsResolver.isExternal(it, localRepositories) }
    }

    private fun createLibrary(
      workspaceContext: WorkspaceContext,
      key: WorkspaceTargetKey,
      targetInfo: TargetIdeInfo,
      onlyOutputJars: Boolean,
      localRepositories: LocalRepositoryMapping,
    ): LibraryItem? {
      val outputs = getTargetOutputJarsList(targetInfo, localRepositories).toSet() + getIntellijPluginJars(targetInfo, localRepositories)
      val rawSources = getSourceJarPaths(targetInfo, localRepositories)
      val sources = if (workspaceContext.preferClassJarsOverSourcelessJars) {
        rawSources - outputs
      }
      else {
        rawSources
      }

      val interfaceJars = getTargetInterfaceJarsList(targetInfo, localRepositories).toSet()
      if (!shouldCreateLibrary(
          dependencies = if (!onlyOutputJars) targetInfo.depsList else emptyList(),
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
            MavenCoordinatesResolver.resolveMavenCoordinates(key.label, outputJar)
          }
        }
        else {
          null
        }

      return createLibrary(
        key = key,
        ijars = interfaceJars,
        jars = outputs,
        sourceJars = sources,
        mavenCoordinates = mavenCoordinates,
        containsInternalJars = targetInfo.containsAnyInternalJars(localRepositories),
      )
    }

    private fun createLibrary(
      key: WorkspaceTargetKey,
      ijars: Collection<Path>,
      jars: Collection<Path>,
      sourceJars: Collection<Path>,
      mavenCoordinates: MavenCoordinates? = null,
      containsInternalJars: Boolean = false,
    ): LibraryItem {
      return LibraryItem(
        key = key,
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
      dependencies: List<IntellijIdeInfo.Dependency>,
      outputs: Collection<Path>,
      interfaceJars: Collection<Path>,
      sources: Collection<Path>,
    ): Boolean = dependencies.isNotEmpty() || !outputs.isEmptyJarList() || !interfaceJars.isEmptyJarList() || !sources.isEmptyJarList()

    private fun Collection<Path>.isEmptyJarList(): Boolean = isEmpty() || singleOrNull()?.name == "empty.jar"

    private fun getIntellijPluginJars(targetInfo: TargetIdeInfo, localRepositories: LocalRepositoryMapping): Set<Path> {
      // _repackaged_files is created upon calling repackaged_files in rules_intellij
      if (targetInfo.kind != "_repackaged_files") return emptySet()
      return targetInfo.generatedSourcesList.toList()
        .resolvePaths(localRepositories)
        .filter { it.extension == "jar" }
        .toSet()
    }

    private fun getSourceJarPaths(targetInfo: TargetIdeInfo, localRepositories: LocalRepositoryMapping) =
      targetInfo.javaCommon.jarsList
        .flatMap { it.sourceJarsList }
        .resolvePaths(localRepositories)

    private fun getTargetOutputJarsList(targetInfo: TargetIdeInfo, localRepositories: LocalRepositoryMapping): List<Path> {
      // proto generator put the generated jar into `javaProvider.fullCompileJarsList`
      // See test `simpleBazelProjectsForTest/protobufStrictDepsTest`
      if (/*targetInfo.kind == "java_proto_library" || */targetInfo.kind == "scala_proto_library")
        return targetInfo.javaProvider.fullCompileJarsList
          .map { bazelPathsResolver.resolve(it, localRepositories) }

      return targetInfo.javaCommon
        .jarsList
        .flatMap { it.binaryJarsList }
        .map { bazelPathsResolver.resolve(it, localRepositories) }
    }

    private fun getTargetInterfaceJarsList(targetInfo: TargetIdeInfo, localRepositories: LocalRepositoryMapping) =
      targetInfo.javaCommon.jarsList
        .flatMap { it.interfaceJarsList }
        .map { bazelPathsResolver.resolve(it, localRepositories) }


    private fun collectInterfacesAndClasses(targets: Collection<TargetIdeInfo>): Map<WorkspaceTargetKey, Set<Path>> {
      val localRepositories = repoMapping.getLocalRepositories()
      return targets.associate { target ->
        target.key.toWorkspaceTargetKey() to
          (getTargetInterfaceJarsList(target, localRepositories) + getTargetOutputJarsList(target, localRepositories))
            .toSet()
      }
    }

    private fun List<ArtifactLocation>.resolvePaths(localRepositories: LocalRepositoryMapping) =
      map { bazelPathsResolver.resolve(it, localRepositories) }.toSet()

    private fun TargetIdeInfo.resolveResourceStripPrefixToAbsolutePath(repositories: LocalRepositoryMapping): Path? {
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
}

private fun <K, V> concatenateMaps(maps: Collection<Map<K, List<V>>>): Map<K, List<V>> =
  maps
    .flatMap { it.keys }
    .distinct()
    .associateWith { key ->
      maps.flatMap { it[key].orEmpty() }
    }
