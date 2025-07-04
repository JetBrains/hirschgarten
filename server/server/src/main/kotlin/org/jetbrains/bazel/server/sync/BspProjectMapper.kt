package org.jetbrains.bazel.server.sync

import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.jpsCompilation.utils.JPS_COMPILED_BASE_DIRECTORY
import org.jetbrains.bazel.label.CanonicalLabel
import org.jetbrains.bazel.server.bsp.info.BspInfo
import org.jetbrains.bazel.server.bzlmod.BzlmodRepoMapping
import org.jetbrains.bazel.server.bzlmod.RepoMappingDisabled
import org.jetbrains.bazel.server.model.AspectSyncProject
import org.jetbrains.bazel.server.model.BspMappings
import org.jetbrains.bazel.server.model.Module
import org.jetbrains.bazel.server.model.NonModuleTarget
import org.jetbrains.bazel.server.model.Project
import org.jetbrains.bazel.server.model.Tag
import org.jetbrains.bazel.server.paths.BazelPathsResolver
import org.jetbrains.bazel.server.sync.languages.LanguagePluginsService
import org.jetbrains.bazel.server.sync.languages.java.JavaModule
import org.jetbrains.bazel.server.sync.languages.jvm.javaModule
import org.jetbrains.bsp.protocol.BazelResolveLocalToRemoteParams
import org.jetbrains.bsp.protocol.BazelResolveLocalToRemoteResult
import org.jetbrains.bsp.protocol.BazelResolveRemoteToLocalParams
import org.jetbrains.bsp.protocol.BazelResolveRemoteToLocalResult
import org.jetbrains.bsp.protocol.CppOptionsItem
import org.jetbrains.bsp.protocol.CppOptionsParams
import org.jetbrains.bsp.protocol.CppOptionsResult
import org.jetbrains.bsp.protocol.DependencySourcesItem
import org.jetbrains.bsp.protocol.DependencySourcesParams
import org.jetbrains.bsp.protocol.DependencySourcesResult
import org.jetbrains.bsp.protocol.DirectoryItem
import org.jetbrains.bsp.protocol.FeatureFlags
import org.jetbrains.bsp.protocol.GoLibraryItem
import org.jetbrains.bsp.protocol.InverseSourcesParams
import org.jetbrains.bsp.protocol.InverseSourcesResult
import org.jetbrains.bsp.protocol.JavacOptionsItem
import org.jetbrains.bsp.protocol.JavacOptionsParams
import org.jetbrains.bsp.protocol.JavacOptionsResult
import org.jetbrains.bsp.protocol.JvmBinaryJarsItem
import org.jetbrains.bsp.protocol.JvmBinaryJarsParams
import org.jetbrains.bsp.protocol.JvmBinaryJarsResult
import org.jetbrains.bsp.protocol.JvmEnvironmentItem
import org.jetbrains.bsp.protocol.JvmMainClass
import org.jetbrains.bsp.protocol.JvmRunEnvironmentParams
import org.jetbrains.bsp.protocol.JvmRunEnvironmentResult
import org.jetbrains.bsp.protocol.JvmTestEnvironmentParams
import org.jetbrains.bsp.protocol.JvmTestEnvironmentResult
import org.jetbrains.bsp.protocol.JvmToolchainInfo
import org.jetbrains.bsp.protocol.LibraryItem
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.jetbrains.bsp.protocol.WorkspaceBazelRepoMappingResult
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsResult
import org.jetbrains.bsp.protocol.WorkspaceDirectoriesResult
import org.jetbrains.bsp.protocol.WorkspaceGoLibrariesResult
import org.jetbrains.bsp.protocol.WorkspaceLibrariesResult
import java.nio.file.Path
import kotlin.io.path.relativeToOrNull

class BspProjectMapper(
  private val languagePluginsService: LanguagePluginsService,
  private val bazelPathsResolver: BazelPathsResolver,
  private val bazelRunner: BazelRunner,
  private val bspInfo: BspInfo,
  private val featureFlags: FeatureFlags,
) {
  fun workspaceTargets(project: AspectSyncProject): WorkspaceBuildTargetsResult {
    val highPrioritySources =
      if (featureFlags.isSharedSourceSupportEnabled) {
        emptySet()
      } else {
        project.modules
          .filter { !it.hasLowSharedSourcesPriority() }
          .flatMap { it.sources }
          .map { it.path }
          .toSet()
      }
    val buildTargets = project.modules.map { it.toBuildTarget(highPrioritySources) }
    val nonModuleTargets =
      project.nonModuleTargets
        .map {
          it.toBuildTarget()
        }.filter { it.kind.isExecutable } // Filter out non-module targets that would just clutter the ui
    return WorkspaceBuildTargetsResult(buildTargets + nonModuleTargets, hasError = project.hasError)
  }

  fun workspaceLibraries(project: AspectSyncProject): WorkspaceLibrariesResult {
    val libraries =
      project.libraries.values.map {
        LibraryItem(
          id = it.label,
          dependencies = it.dependencies,
          ijars = it.interfaceJars.toList(),
          jars = it.outputs.toList(),
          sourceJars = it.sources.toList(),
          mavenCoordinates = it.mavenCoordinates,
          isFromInternalTarget = it.isFromInternalTarget,
        )
      }
    return WorkspaceLibrariesResult(libraries)
  }

  fun workspaceGoLibraries(project: AspectSyncProject): WorkspaceGoLibrariesResult {
    val libraries =
      project.goLibraries.values.map {
        GoLibraryItem(
          id = it.label,
          goImportPath = it.goImportPath,
          goRoot = it.goRoot,
        )
      }
    return WorkspaceGoLibrariesResult(libraries)
  }

  fun workspaceDirectories(project: Project): WorkspaceDirectoriesResult {
    // bazel symlinks exclusion logic is now taken care by BazelSymlinkExcludeService,
    // so there is no need for excluding them here anymore

    val directoriesSection = project.workspaceContext.directories

    val workspaceRoot = project.workspaceRoot

    val additionalDirectoriesToExclude = computeAdditionalDirectoriesToExclude(workspaceRoot)
    val directoriesToExclude = directoriesSection.excludedValues + additionalDirectoriesToExclude

    return WorkspaceDirectoriesResult(
      includedDirectories = directoriesSection.values.map { it.toDirectoryItem() },
      excludedDirectories = directoriesToExclude.map { it.toDirectoryItem() },
    )
  }

  fun workspaceBazelRepoMapping(project: Project): WorkspaceBazelRepoMappingResult {
    val repoMapping = project.repoMapping
    return when (repoMapping) {
      is RepoMappingDisabled -> WorkspaceBazelRepoMappingResult(emptyMap(), emptyMap())
      is BzlmodRepoMapping ->
        WorkspaceBazelRepoMappingResult(
          apparentRepoNameToCanonicalName = repoMapping.apparentRepoNameToCanonicalName,
          canonicalRepoNameToPath = repoMapping.canonicalRepoNameToPath,
        )
    }
  }

  private fun computeAdditionalDirectoriesToExclude(workspaceRoot: Path): List<Path> =
    listOf(
      bspInfo.bazelBspDir(),
      workspaceRoot.resolve(JPS_COMPILED_BASE_DIRECTORY),
    )

  private fun Path.toDirectoryItem() =
    DirectoryItem(
      uri = this.toUri().toString(),
    )

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
      )
    return buildTarget
  }

  private fun Module.toBuildTarget(highPrioritySources: Set<Path>): RawBuildTarget {
    val label = label
    val dependencies =
      directDependencies
    val tags = tags.mapNotNull(BspMappings::toBspTag)

    val (sources, lowPrioritySharedSources) =
      if (hasLowSharedSourcesPriority()) {
        sources.partition { it.path !in highPrioritySources }
      } else {
        sources to emptyList()
      }

    val buildTarget =
      RawBuildTarget(
        id = label,
        tags = tags,
        dependencies = dependencies,
        kind = inferKind(this.tags, kindString, languages),
        baseDirectory = baseDirectory,
        sources = sources,
        lowPrioritySharedSources = lowPrioritySharedSources,
        noBuild = this.tags.contains(Tag.NO_BUILD),
        resources = resources.toList(),
      )

    applyLanguageData(this, buildTarget)
    return buildTarget
  }

  private fun Module.hasLowSharedSourcesPriority(): Boolean = Tag.IDE_LOW_SHARED_SOURCES_PRIORITY in tags

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

  private fun applyLanguageData(module: Module, buildTarget: RawBuildTarget) {
    val plugin = languagePluginsService.getPlugin(module.languages)
    module.languageData?.let { plugin.setModuleData(it, buildTarget) }
  }

  suspend fun inverseSources(project: AspectSyncProject, inverseSourcesParams: InverseSourcesParams): InverseSourcesResult {
    val documentRelativePath =
      inverseSourcesParams.textDocument.path
        .relativeToOrNull(project.workspaceRoot) ?: throw RuntimeException("File path outside of project root")
    return InverseSourcesQuery.inverseSourcesQuery(documentRelativePath, bazelRunner, project.bazelRelease, project.workspaceContext)
  }

  fun dependencySources(project: AspectSyncProject, dependencySourcesParams: DependencySourcesParams): DependencySourcesResult {
    fun getDependencySourcesItem(label: CanonicalLabel): DependencySourcesItem {
      val sources =
        project
          .findModule(label)
          ?.sourceDependencies
          ?.toList()
          .orEmpty()
      return DependencySourcesItem(label, sources)
    }

    val labels = dependencySourcesParams.targets
    val items = labels.map(::getDependencySourcesItem)
    return DependencySourcesResult(items)
  }

  suspend fun jvmRunEnvironment(project: AspectSyncProject, params: JvmRunEnvironmentParams): JvmRunEnvironmentResult {
    val targets = params.targets
    val result = getJvmEnvironmentItems(project, targets)
    return JvmRunEnvironmentResult(result)
  }

  suspend fun jvmTestEnvironment(project: AspectSyncProject, params: JvmTestEnvironmentParams): JvmTestEnvironmentResult {
    val targets = params.targets
    val result = getJvmEnvironmentItems(project, targets)
    return JvmTestEnvironmentResult(result)
  }

  private suspend fun getJvmEnvironmentItems(project: AspectSyncProject, targets: List<CanonicalLabel>): List<JvmEnvironmentItem> {
    fun extractJvmEnvironmentItem(module: Module, runtimeClasspath: List<Path>): JvmEnvironmentItem? =
      module.javaModule?.let { javaModule ->
        JvmEnvironmentItem(
          module.label,
          runtimeClasspath,
          javaModule.jvmOps.toList(),
          bazelPathsResolver.workspaceRoot(),
          module.environmentVariables,
          mainClasses = javaModule.mainClass?.let { listOf(JvmMainClass(it, javaModule.args)) }.orEmpty(),
        )
      }

    return targets.mapNotNull {
      val module = project.findModule(it)
      val cqueryResult = ClasspathQuery.classPathQuery(it, bspInfo, bazelRunner, project.workspaceContext).runtime_classpath
      val resolvedClasspath = resolveClasspath(cqueryResult)
      module?.let { extractJvmEnvironmentItem(module, resolvedClasspath) }
    }
  }

  fun jvmBinaryJars(project: AspectSyncProject, params: JvmBinaryJarsParams): JvmBinaryJarsResult {
    fun toJvmBinaryJarsItem(module: Module): JvmBinaryJarsItem? =
      module.javaModule?.let { javaModule ->
        val jars = javaModule.binaryOutputs
        JvmBinaryJarsItem(module.label, jars)
      }

    val jvmBinaryJarsItems =
      params.targets.mapNotNull { target ->
        val module = project.findModule(target)
        module?.let { toJvmBinaryJarsItem(it) }
      }
    return JvmBinaryJarsResult(jvmBinaryJarsItems)
  }

  fun buildTargetJavacOptions(project: AspectSyncProject, params: JavacOptionsParams): JavacOptionsResult {
    val items =
      params.targets
        .mapNotNull { project.findModule(it) }
        .mapNotNull { module -> module.javaModule?.let { toJavacOptionsItem(module, it) } }
    return JavacOptionsResult(items)
  }

  fun buildTargetCppOptions(project: AspectSyncProject, params: CppOptionsParams): CppOptionsResult {
    fun extractCppOptionsItem(module: Module): CppOptionsItem? =
      languagePluginsService.extractCppModule(module)?.let {
        languagePluginsService.cppLanguagePlugin.toCppOptionsItem(module, it)
      }

    val modules = BspMappings.getModules(project, params.targets)
    val items = modules.mapNotNull(::extractCppOptionsItem)
    return CppOptionsResult(items)
  }

  private fun resolveClasspath(cqueryResult: List<Path>): List<Path> =
    cqueryResult
      .map { bazelPathsResolver.resolveOutput(it) }
      .filter { it.toFile().exists() } // I'm surprised this is needed, but we literally test it in e2e tests

  private fun toJavacOptionsItem(module: Module, javaModule: JavaModule): JavacOptionsItem =
    JavacOptionsItem(
      module.label,
      javaModule.javacOpts.toList(),
    )

  fun resolveLocalToRemote(params: BazelResolveLocalToRemoteParams): BazelResolveLocalToRemoteResult {
    val resolve = languagePluginsService.goLanguagePlugin::resolveLocalToRemote
    return resolve(params)
  }

  fun resolveRemoteToLocal(params: BazelResolveRemoteToLocalParams): BazelResolveRemoteToLocalResult {
    val resolve = languagePluginsService.goLanguagePlugin::resolveRemoteToLocal
    return resolve(params)
  }

  suspend fun jvmBuilderParams(project: Project): JvmToolchainInfo =
    JvmToolchainQuery.jvmToolchainQuery(bspInfo, bazelRunner, project.workspaceContext)
}
