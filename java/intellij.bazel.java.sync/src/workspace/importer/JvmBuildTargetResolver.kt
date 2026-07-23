package org.jetbrains.bazel.workspace.importer

import com.intellij.openapi.util.Ref
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.label.DependencyLabelKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.languages.java.JavaWorkspaceSyncConfig
import org.jetbrains.bazel.sync.workspace.languages.jvm.JavaProviderData
import org.jetbrains.bazel.sync.workspace.languages.jvm.JavaToolchainData
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmDependency
import org.jetbrains.bazel.sync.workspace.languages.jvm.KotlinBuildTarget
import org.jetbrains.bazel.sync.workspace.languages.jvm.ScalaBuildTarget
import org.jetbrains.bazel.sync.workspace.mapper.normal.MavenCoordinatesResolver
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationId
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTarget
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.sync.workspace.snapshot.findBuildData
import org.jetbrains.bazel.sync.workspace.snapshot.kind
import org.jetbrains.bsp.protocol.LibraryItem
import org.jetbrains.bsp.protocol.MavenCoordinates
import org.jetbrains.bsp.protocol.allJars
import java.nio.file.Path
import kotlin.io.path.name

private typealias DependencyLabelPatcher = (DependencyLabel) -> DependencyLabel

@ApiStatus.Internal
class JvmBuildTargetResolver(
  private val allTargets: Map<WorkspaceTargetKey, WorkspaceTarget>,
  private val targetsToImport: Map<WorkspaceTargetKey, WorkspaceTarget>,
  private val javaSyncConfig: JavaWorkspaceSyncConfig,
) {
  private var extraLibDependencies: Map<WorkspaceTargetKey, List<DependencyLabel>> = mapOf()
  private var toolchainDependencies: Map<WorkspaceTargetKey, List<DependencyLabel>> = mapOf()
  private var allLibraries: Map<WorkspaceTargetKey, List<LibraryItem>> = mapOf()
  private var wellKnownTargetKeyByMavenCoordinates: Map<MavenCoordinatesKey, WorkspaceTargetKey> = mapOf()

  // keep output jar label identity
  private val outputJarsByLabel: Map<Label, Set<Path>> =
    buildMap<Label, MutableSet<Path>> {
      for ((key, target) in allTargets) {
        val jars = target.rawOutputBinaryJars()
        if (jars.isNotEmpty()) {
          getOrPut(key.label) { mutableSetOf() } += jars
        }
      }
    }

  private val projectJavaHome: Path? by lazy { JdkResolver(allTargets, javaSyncConfig.ideJavaHomeOverride).resolve()?.javaHome }

  fun resolveAll(): Map<WorkspaceTargetKey, JvmResolvedTarget> {
    wellKnownTargetKeyByMavenCoordinates = computeWellKnownTargetKeyByMavenCoordinates()
    calculateAllLibraries(targetsToImport = targetsToImport.filterValues { it.findBuildData<JvmBuildTarget>() != null })

    // We merge targets based on (label, configuration) pair because:
    //  * dependency edges contain only partial key (label + configuration)
    //  * to avoid unnecessary expansion of module graph (two axis is enough),
    //    Merging is being done inside `JvmWorkspaceTargetMerger`, this part of the code aims to replicate that,
    //    so we get exactly single target for (label, configuration) pair
    //  The most common example where merging is used is for code generators like protobuf,
    //  which use aspect to attach language-specific providers to e.g.,
    //  proto provider, then we get `(label = //a, config = X, aspects = [])` - no JavaInfo
    //  and `(label = //a, config = X, aspects = [//proto_smth])` - with JavaInfo (generated proto sources),
    //  and based on that generated target when used as dependency bring proto generated sources
    return targetsToImport.values
      .filter { it.findBuildData<JvmBuildTarget>() != null }
      .map { createJvmResolvedTarget(it) }
      .groupBy { it.key.stripAspects() }
      .mapValues { (strippedKey, group) ->
        JvmResolvedTarget(
          key = strippedKey,
          libraries = group.flatMap { it.libraries }.distinctBy { it.key },
          jvmDependencies = group.flatMap { it.jvmDependencies }
            .distinctBy { it::class to it.dependency.copy(targetKey = it.dependency.targetKey.stripAspects()) },
          javaHome = projectJavaHome,
          javaVersion = group.first().javaVersion,
        )
      }
  }

  private fun createJvmResolvedTarget(target: WorkspaceTarget): JvmResolvedTarget {
    val targetKey = target.targetKey
    val javaVersion =
      javaVersionFromJavacOpts(target.findBuildData<JvmBuildTarget>()?.javacOpts.orEmpty()) ?: javaVersionFromToolchain(target)

    val targetLibraries = allLibraries[targetKey].orEmpty().associateBy { it.key }

    return JvmResolvedTarget(
      key = targetKey,
      libraries = targetLibraries.values.toList(),
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
      javaHome = projectJavaHome,
      javaVersion = javaVersion.orEmpty(),
    )
  }

  private val dependenciesCache = mutableMapOf<WorkspaceTargetKey, List<DependencyLabel>>()
  private val mavenExportDependenciesCache = mutableMapOf<WorkspaceTargetKey, List<DependencyLabel>>()

  private fun WorkspaceTarget.dependencies(): List<DependencyLabel> {
    if (mavenCoordinatesKeyOrNull() in wellKnownTargetKeyByMavenCoordinates) return mavenExportDependencies()
    return directDependencies()
  }

  private fun WorkspaceTarget.directDependencies(): List<DependencyLabel> {
    val target = this
    return dependenciesCache.getOrPut(target.targetKey) F@{
      val kind = target.rawBuildTarget.kind.kind

      // Well-known targets which include generated libraries as dependencies.
      // They must be exported, but this is not returned from aspects:
      // https://bazel.build/reference/be/protocol-buffer#proto_library_args
      if (kind == "java_proto_library") {
        return@F target.rawBuildTarget.dependencies.map {
          it.copy(kind = DependencyLabelKind.EXPORTED_COMPILE_TIME)
        }
      }

      // Scala proto libraries do not have meaningful dependencies despite somethign might return from aspects
      // The entire transitive closure of libraries, which is used by compiler,
      // is returned in targetInfo.javaProvider.fullCompileJarsList
      if (kind == "scala_proto_library") {
        return@F emptyList()
      }

      // https://youtrack.jetbrains.com/issue/BAZEL-3218
      // Some custom rules declare the dependent output jar as its own,
      // thus making the dependency effectively exported
      val exportByOutputJarsPatcher: DependencyLabelPatcher =
        if (kind in wellKnownTargetKinds) {
          // Well known rules have well known behavior
          { it }
        }
        else {
          val outputJars = target.rawOutputBinaryJars();
          DependencyLabelPatcher@{ dependency ->
            // Over-approximate to label to keep correct behavior when target depend on targets with different configuration/aspects
            val depJars = outputJarsByLabel[dependency.targetKey.label] ?: emptySet()
            if (outputJars.intersect(depJars).isNotEmpty())
              return@DependencyLabelPatcher dependency.copy(kind = DependencyLabelKind.EXPORTED_COMPILE_TIME)

            dependency
          }
        }

      return@F target.rawBuildTarget.dependencies.map {
        exportByOutputJarsPatcher(it.toWellKnownTargetByMavenCoordinates())
      }
    }
  }

  // Locates a target of known kind for every maven coordinates that is also associated with target of unknown kind.
  // This pattern is produced by `java_export` macro from rules_jvm_external
  // It should also work with other macros following similar pattern e.g. macros from selenium repository
  private fun computeWellKnownTargetKeyByMavenCoordinates(): Map<MavenCoordinatesKey, WorkspaceTargetKey> {
    val unknownTargetsWithMavenCoordinates = allTargets.values
      .asSequence()
      .filter { it.kind !in wellKnownTargetKinds && it.rawBuildTarget.isWorkspace }
      .mapNotNull {
        val key = it.mavenCoordinatesKeyOrNull() ?: return@mapNotNull null
        key to it.targetKey
      }.groupBy(keySelector = { it.first }, valueTransform = { it.second })
      .toMap()
    val result = mutableMapOf<MavenCoordinatesKey, WorkspaceTargetKey>()
    val ambiguousResults = mutableSetOf<MavenCoordinatesKey>()
    for (target in allTargets.values) {
      if (target.kind !in wellKnownTargetKinds || !target.rawBuildTarget.isWorkspace) continue
      val coordinatesKey = target.mavenCoordinatesKeyOrNull() ?: continue
      val targetsToReplaceKeys = unknownTargetsWithMavenCoordinates[coordinatesKey] ?: continue
      val targetsToReplace = targetsToReplaceKeys.mapNotNull { allTargets[it] }.ifEmpty { null } ?: continue
      val targetGeneratorName = target.rawBuildTarget.generatorName
      if (targetGeneratorName == null || targetsToReplace.any { it.rawBuildTarget.generatorName != targetGeneratorName }) continue
      if (coordinatesKey in ambiguousResults) continue
      if (coordinatesKey in result) {
        ambiguousResults += coordinatesKey
        continue
      }
      result[coordinatesKey] = target.targetKey
    }
    return result - ambiguousResults
  }

  private fun DependencyLabel.toWellKnownTargetByMavenCoordinates(): DependencyLabel {
    val target = allTargets[targetKey] ?: return this
    if (target.kind in wellKnownTargetKinds || !target.rawBuildTarget.isWorkspace) return this
    val coordinatesKey = target.mavenCoordinatesKeyOrNull() ?: return this
    val libraryKey = wellKnownTargetKeyByMavenCoordinates[coordinatesKey] ?: return this
    return this.copy(targetKey = libraryKey)
  }

  private fun WorkspaceTarget.mavenCoordinatesKeyOrNull(): MavenCoordinatesKey? {
    val coordinates = MavenCoordinatesResolver
      .fromTargetTagsList(rawBuildTarget.tags)
      ?: return null
    return coordinates.toKey(targetKey.configuration)
  }

  // `java_export` (rules_jvm_external) merges every transitive dependency that is NOT itself a maven artifact into the single published jar
  // we mirror that by treating those inlined non-maven deps as exported
  // not exporting those leads to red code when referring those non-maven transitive dependencies
  private fun WorkspaceTarget.mavenExportDependencies(): List<DependencyLabel> = mavenExportDependenciesCache.getOrPut(targetKey) {
    directDependencies()
      .flatMap { dependency ->
        val depTarget = allTargets[dependency.targetKey]
        when {
          depTarget != null && depTarget.shouldBeExportedForMavenArtifact() -> dependency
            .copy(kind = DependencyLabelKind.EXPORTED_COMPILE_TIME)
            .let(::listOf)
            .plus(depTarget.mavenExportDependencies())
          else -> listOf(dependency)
        }
      }.distinct()
  }

  private fun WorkspaceTarget.shouldBeExportedForMavenArtifact(): Boolean =
    rawBuildTarget.isWorkspace && mavenCoordinatesKeyOrNull() == null

  private fun calculateAllLibraries(
    targetsToImport: Map<WorkspaceTargetKey, WorkspaceTarget>,
  ) {
    // Avoid creating the same LibraryItem instance several times to avoid O(N^2) (BAZEL-3203)
    val libraryItemByIdCache = hashMapOf<WorkspaceTargetKey, Ref<LibraryItem?>>()

    val importDependenciesAsLibraries: Map<WorkspaceTargetKey, List<LibraryItem>> =
      targetsToImport.mapValues { (_, target) ->
        target.dependencies()
          .mapNotNull { dependency ->
            val depKey = dependency.targetKey
            if (targetsToImport.containsKey(depKey))
              return@mapNotNull null // Dependency target is imported, no need to create library

            val libTarget = allTargets[depKey]
                            ?: return@mapNotNull null

            libraryItemByIdCache.getOrPut(depKey) {
              Ref(createLibrary(depKey, libTarget))
            }.get()
          }
      }

    val interfacesAndBinariesFromTargetsToImport: Map<WorkspaceTargetKey, Set<Path>> =
      collectInterfacesAndClasses(targetsToImport.values)
    val outputJarsLibraries: Map<WorkspaceTargetKey, List<LibraryItem>> =
      calculateOutputJarsLibraries(targetsToImport.values)
    val annotationProcessorLibraries: Map<WorkspaceTargetKey, List<LibraryItem>> =
      annotationProcessorLibraries(targetsToImport.values)
    val librariesFromToolchains: Map<WorkspaceTargetKey, List<LibraryItem>> =
      calculateToolchainLibraries(targetsToImport)
    val librariesFromDeps: Map<WorkspaceTargetKey, List<LibraryItem>> =
      concatenateMaps(listOf(outputJarsLibraries, annotationProcessorLibraries))
    val librariesFromDepsAndTargets: Map<WorkspaceTargetKey, List<LibraryItem>> =
      concatenateMaps(listOf(librariesFromDeps, librariesFromToolchains, importDependenciesAsLibraries))
    val extraLibrariesFromJdeps: Map<WorkspaceTargetKey, List<LibraryItem>> =
      jdepsLibraries(
        targetsToImport,
        librariesFromDepsAndTargets,
        interfacesAndBinariesFromTargetsToImport,
      )

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

  private fun toolchainInfo(target: WorkspaceTarget): JavaToolchainData? {
    target.findBuildData<JavaToolchainData>()?.let { return it }
    return target.rawBuildTarget.dependencies.asSequence()
      .filter { it.kind == DependencyLabelKind.TOOLCHAIN }
      .mapNotNull { allTargets[it.targetKey] }
      .firstNotNullOfOrNull { it.findBuildData<JavaToolchainData>() }
  }

  private fun javaVersionFromToolchain(target: WorkspaceTarget): String? = toolchainInfo(target)?.sourceVersion

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

  private fun hasKnownJvmSources(target: WorkspaceTarget): Boolean =
    target.rawBuildTarget.sources.getFiles().any {
      val path = it.toString()
      path.endsWith(".java") ||
      path.endsWith(".kt") ||
      path.endsWith(".scala")
    }

  private fun shouldCreateOutputJarsLibrary(target: WorkspaceTarget): Boolean {
    // Resource-only targets and non-JVM targets never produce output jars worth indexing.
    if (target.rawBuildTarget.kind.kind.endsWith("_resources") || target.findBuildData<JvmBuildTarget>() == null) {
      return false
    }

    val hasGeneratedSrcJar = target.rawBuildTarget.generatedSources.getFiles().any { it.toString().endsWith(".srcjar") }
    val hasOnlyNonJvmSources = target.rawBuildTarget.sources.getFiles().any() && !hasKnownJvmSources(target)
    val isUnknownTargetWithoutSources =
      target.rawBuildTarget.sources.getFiles().none() && target.rawBuildTarget.kind.kind !in wellKnownTargetKinds &&
      !(target.findBuildData<JvmBuildTarget>()?.hasExecutableInfo ?: false)
    val hasApiGeneratingPlugins = target.findBuildData<JavaProviderData>()?.hasApiGeneratingPlugins ?: false
    val dependsOnExportedApiGeneratingPlugins =
      target.findBuildData<KotlinBuildTarget>()?.exportedCompilerPluginTargetsList.orEmpty().any { key ->
        allTargets[key]?.findBuildData<JavaProviderData>()?.hasApiGeneratingPlugins ?: false
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
    targetsToImport: Collection<WorkspaceTarget>,
  ): Map<WorkspaceTargetKey, List<LibraryItem>> {
    return targetsToImport
      .filter { shouldCreateOutputJarsLibrary(it) }
      .mapNotNull { target ->
        createLibrary(target.targetKey, target)?.let { library ->
          target.targetKey to listOf(library)
        }
      }.toMap()
  }

  private fun annotationProcessorLibraries(targetsToImport: Collection<WorkspaceTarget>): Map<WorkspaceTargetKey, List<LibraryItem>> {
    return targetsToImport
      .filter { it.findBuildData<JvmBuildTarget>()?.generatedJars?.isNotEmpty() == true }
      .associate { target ->
        val libKey = target.targetKey
        val generated = target.findBuildData<JvmBuildTarget>()?.generatedJars.orEmpty()
        libKey to
          createLibrary(
            // `Label.toString()` round-trips the raw target label, so this matches the pre-refactor `key.label + "_generated"`
            key = libKey.copy(label = Label.synthetic(target.targetKey.label.toString() + "_generated")),
            ijars = emptySet(),
            jars = generated.flatMap { it.binaryJars.getFiles().toList() }.toSet(),
            sourceJars = generated.flatMap { it.sourceJars.getFiles().toList() }.toSet(),
          )
      }.mapValues { listOf(it.value) }
      .toMap()
  }

  // inlined `toolchainLibraries` from `JvmLanguagePluginMixin`
  private fun calculateToolchainLibraries(
    targetsToImport: Map<WorkspaceTargetKey, WorkspaceTarget>,
  ): Map<WorkspaceTargetKey, List<LibraryItem>> {
    val result = HashMap<WorkspaceTargetKey, MutableList<LibraryItem>>()

    val stdlibJars =
      allTargets.values.mapNotNull { it.findBuildData<KotlinBuildTarget>() }.flatMap { it.stdlibHardLinkedJars.getFiles().toList() }
        .distinct()
    if (stdlibJars.isNotEmpty()) {
      val stdlibSources =
        allTargets.values.mapNotNull { it.findBuildData<KotlinBuildTarget>() }.flatMap { it.stdlibInferredSourceJars.getFiles().toList() }
          .distinct()
      val stdlib =
        createLibrary(
          WorkspaceTargetKey(label = Label.synthetic("rules_kotlin_kotlin-stdlibs")),
          ijars = emptySet(),
          jars = stdlibJars.toSet(),
          sourceJars = stdlibSources.toSet(),
        )
      targetsToImport.filterValues { it.findBuildData<KotlinBuildTarget>() != null }.keys.forEach {
        result.getOrPut(it) { mutableListOf() }.add(stdlib)
      }
    }

    val sdkLibByJar =
      allTargets.values.mapNotNull { it.findBuildData<ScalaBuildTarget>() }.flatMap { it.sdkJars.getFiles().toList() }.toSet()
        .associateWith {
          createLibrary(
            WorkspaceTargetKey(label = Label.synthetic(it.name)),
            ijars = emptySet(),
            jars = setOf(it),
            sourceJars = emptySet(),
          )
        }
    val testLibByJar = allTargets.values.flatMap { it.scalatestClasspathJars() }.toSet()
      .associateWith {
        createLibrary(
          WorkspaceTargetKey(label = Label.synthetic(it.name)),
          ijars = emptySet(),
          jars = setOf(it),
          sourceJars = emptySet(),
        )
      }
    targetsToImport.forEach { (key, target) ->
      val scala = target.findBuildData<ScalaBuildTarget>() ?: return@forEach
      val libs = (scala.sdkJars.getFiles().mapNotNull { sdkLibByJar[it] }.toList() + target.scalatestClasspathJars()
        .mapNotNull { testLibByJar[it] }).distinct()
      if (libs.isNotEmpty()) {
        result.getOrPut(key) { mutableListOf() }.addAll(libs)
      }
    }
    return result
  }

  /**
   * In some cases, the jar dependencies of a target might be injected by bazel or rules and not are not
   * available via `deps` field of a target. For this reason, we read JavaOutputInfo's jdeps file and
   * filter out jars that have not been included in the target's `deps` list.
   *
   * The old Bazel Plugin performs similar step here
   * https://github.com/bazelbuild/intellij/blob/b68ec8b33aa54ead6d84dd94daf4822089b3b013/java/src/com/google/idea/blaze/java/sync/importer/BlazeJavaWorkspaceImporter.java#L256
   */
  private fun jdepsLibraries(
    targetsToImport: Map<WorkspaceTargetKey, WorkspaceTarget>,
    libraryDependencies: Map<WorkspaceTargetKey, List<LibraryItem>>,
    interfacesAndBinariesFromTargetsToImport: Map<WorkspaceTargetKey, Set<Path>>,
  ): Map<WorkspaceTargetKey, List<LibraryItem>> {
    val targetsToJdepsJars: Map<WorkspaceTargetKey, Set<Path>> =
      getAllJdepsDependencies(targetsToImport, libraryDependencies)
    val libraryNameToLibraryValueMap = HashMap<WorkspaceTargetKey, LibraryItem>()
    return targetsToJdepsJars.mapValues { target: Map.Entry<WorkspaceTargetKey, Set<Path>> ->
      val interfacesAndBinariesFromTarget =
        interfacesAndBinariesFromTargetsToImport.getOrDefault(target.key, emptySet())
      target.value
        .filter { it !in interfacesAndBinariesFromTarget }
        .mapNotNull {
          // the synthetic label was precomputed by the plugin (it needs `bazelBin`, only available at map time)
          val label = jdepsLabelByJar[it] ?: return@mapNotNull null
          val key = WorkspaceTargetKey(label = label)
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

  private val jdepsLabelByJar: Map<Path, Label> =
    allTargets.values.asSequence().mapNotNull { it.findBuildData<JvmBuildTarget>() }
      .flatMap { it.jdepsJars.asSequence() }.associate { it.jar to it.syntheticLabel }

  private fun getAllJdepsDependencies(
    targetsToImport: Map<WorkspaceTargetKey, WorkspaceTarget>,
    libraryDependencies: Map<WorkspaceTargetKey, List<LibraryItem>>,
  ): Map<WorkspaceTargetKey, Set<Path>> {
    val jdepsJars =
      targetsToImport
        .mapValues { (_, target) -> target.findBuildData<JvmBuildTarget>()?.jdepsJars.orEmpty().map { it.jar }.toSet() }
        .filterValues { it.isNotEmpty() }

    val allJdepsJars =
      jdepsJars.values
        .asSequence()
        .flatten()
        .toSet()

    val outputJarsFromTransitiveDepsCache =  mutableMapOf<WorkspaceTargetKey, Set<Path>>()
    return jdepsJars
      .mapValues { (targetKey, jarsFromJdeps) ->
        val transitiveJdepsJars =
          getJdepsJarsFromTransitiveDependencies(
            targetKey,
            targetsToImport,
            libraryDependencies,
            outputJarsFromTransitiveDepsCache,
            allJdepsJars,
            visited = hashSetOf(),
          )
        jarsFromJdeps - transitiveJdepsJars
      }
      .filterValues { it.isNotEmpty() }
  }

  private fun getJdepsJarsFromTransitiveDependencies(
    target: WorkspaceTargetKey,
    targetsToImport: Map<WorkspaceTargetKey, WorkspaceTarget>,
    libraryDependencies: Map<WorkspaceTargetKey, List<LibraryItem>>,
    outputJarsFromTransitiveDepsCache: MutableMap<WorkspaceTargetKey, Set<Path>>,
    allJdepsJars: Set<Path>,
    visited: MutableSet<WorkspaceTargetKey>,
  ): Set<Path> {
    val cached = outputJarsFromTransitiveDepsCache[target]
    if (cached != null)
      return cached

    if (!visited.add(target)) // prevent STOFL if dependency cycle
      return emptySet()

    val jarsFromTargets =
      targetsToImport[target]?.let { getTargetOutputJarsList(it) + getTargetInterfaceJarsList(it) }
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
        visited,
      )
    }

    outputJarsFromTransitiveDepsCache[target] = outputJars
    return outputJars
  }

  private fun createLibrary(
    key: WorkspaceTargetKey,
    target: WorkspaceTarget,
  ): LibraryItem? {
    val outputs = getTargetOutputJarsList(target).toSet() + getIntellijPluginJars(target)
    val rawSources = getSourceJarPaths(target)
    val sources = if (javaSyncConfig.preferClassJarsOverSourcelessJars) {
      rawSources - outputs
    }
    else {
      rawSources
    }

    val interfaceJars = getTargetInterfaceJarsList(target).toSet()
    if (outputs.isEmptyJarList() && interfaceJars.isEmptyJarList() && sources.isEmptyJarList()) {
      return null
    }

    val mavenCoordinates =
      MavenCoordinatesResolver.fromTargetTagsList(target.rawBuildTarget.tags)
      ?: outputs.firstOrNull()?.let { outputJar ->
        MavenCoordinatesResolver.resolveMavenCoordinates(key.label, outputJar)
      }

    return createLibrary(
      key = key,
      ijars = interfaceJars,
      jars = outputs,
      sourceJars = sources,
      mavenCoordinates = mavenCoordinates,
      containsInternalJars = containsAnyInternalJars(target),
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

  private fun Collection<Path>.isEmptyJarList(): Boolean = isEmpty() || singleOrNull()?.name == "empty.jar"

  private fun getIntellijPluginJars(target: WorkspaceTarget): Set<Path> =
    target.findBuildData<JvmBuildTarget>()?.intellijPluginJars?.getFiles()?.toSet().orEmpty()

  private fun getSourceJarPaths(target: WorkspaceTarget): Set<Path> =
    target.findBuildData<JvmBuildTarget>()?.outputSourceJars?.getFiles()?.toSet().orEmpty()

  private fun getTargetOutputJarsList(target: WorkspaceTarget): List<Path> {
    // proto generator put the generated jar into `javaProvider.fullCompileJarsList`
    // See test `simpleBazelProjectsForTest/protobufStrictDepsTest`
    if (target.rawBuildTarget.kind.kind == "scala_proto_library")
      return target.findBuildData<JavaProviderData>()?.fullCompileJars?.getFiles()?.toList().orEmpty()

    return target.outputBinaryJars().toList()
  }

  private fun WorkspaceTarget.outputBinaryJars(): Set<Path> =
    findBuildData<JvmBuildTarget>()?.binaryOutputs?.getFiles()?.toSet() ?: emptySet()

  private fun WorkspaceTarget.rawOutputBinaryJars(): Set<Path> =
    findBuildData<JvmBuildTarget>()?.rawBinaryOutputs?.getFiles()?.toSet() ?: emptySet()

  private fun getTargetInterfaceJarsList(target: WorkspaceTarget): List<Path> =
    target.findBuildData<JvmBuildTarget>()?.outputInterfaceJars?.getFiles()?.toList().orEmpty()

  private fun containsAnyInternalJars(target: WorkspaceTarget): Boolean =
    target.findBuildData<JvmBuildTarget>()?.containsInternalJars ?: false

  private fun collectInterfacesAndClasses(targets: Collection<WorkspaceTarget>): Map<WorkspaceTargetKey, Set<Path>> {
    return targets.associate { target ->
      target.targetKey to
        (getTargetInterfaceJarsList(target) + getTargetOutputJarsList(target))
          .toSet()
    }
  }

  private fun WorkspaceTarget.scalatestClasspathJars(): List<Path> =
    findBuildData<ScalaBuildTarget>()?.scalatestClasspathTargets.orEmpty().flatMap { label ->
      allTargets[targetKey.copy(label = label)]?.findBuildData<JavaProviderData>()?.fullCompileJars?.getFiles()?.toList().orEmpty()
    }

  private fun <K, V> concatenateMaps(maps: Collection<Map<K, List<V>>>): Map<K, List<V>> =
    maps
      .flatMap { it.keys }
      .distinct()
      .associateWith { key ->
        maps.flatMap { it[key].orEmpty() }
      }
}

private fun MavenCoordinates.toKey(configuration: WorkspaceConfigurationId?): MavenCoordinatesKey {
  return MavenCoordinatesKey(this, configuration)
}

private data class MavenCoordinatesKey(
  val coordinates: MavenCoordinates,
  val configuration: WorkspaceConfigurationId?,
)
