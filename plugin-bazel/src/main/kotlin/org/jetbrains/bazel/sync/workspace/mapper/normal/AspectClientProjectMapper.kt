package org.jetbrains.bazel.sync.workspace.mapper.normal

import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.Tag
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.BazelResolvedWorkspace
import org.jetbrains.bazel.sync.workspace.BuildTargetCollection
import org.jetbrains.bazel.sync.workspace.languages.LanguagePluginsService
import org.jetbrains.bazel.sync.workspace.languages.java.JavaModule
import org.jetbrains.bazel.sync.workspace.languages.jvm.javaModule
import org.jetbrains.bazel.sync.workspace.mapper.BazelMappedProject
import org.jetbrains.bazel.sync.workspace.model.BspMappings
import org.jetbrains.bazel.sync.workspace.model.Module
import org.jetbrains.bazel.sync.workspace.model.NonModuleTarget
import org.jetbrains.bsp.protocol.BazelResolveLocalToRemoteParams
import org.jetbrains.bsp.protocol.BazelResolveLocalToRemoteResult
import org.jetbrains.bsp.protocol.BazelResolveRemoteToLocalParams
import org.jetbrains.bsp.protocol.BazelResolveRemoteToLocalResult
import org.jetbrains.bsp.protocol.DependencySourcesItem
import org.jetbrains.bsp.protocol.DependencySourcesParams
import org.jetbrains.bsp.protocol.DependencySourcesResult
import org.jetbrains.bsp.protocol.FeatureFlags
import org.jetbrains.bsp.protocol.JavacOptionsItem
import org.jetbrains.bsp.protocol.JavacOptionsParams
import org.jetbrains.bsp.protocol.JavacOptionsResult
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.bsp.protocol.JvmBinaryJarsItem
import org.jetbrains.bsp.protocol.JvmBinaryJarsParams
import org.jetbrains.bsp.protocol.JvmBinaryJarsResult
import org.jetbrains.bsp.protocol.JvmEnvironmentItem
import org.jetbrains.bsp.protocol.JvmMainClass
import org.jetbrains.bsp.protocol.JvmRunEnvironmentParams
import org.jetbrains.bsp.protocol.JvmRunEnvironmentResult
import org.jetbrains.bsp.protocol.JvmTestEnvironmentParams
import org.jetbrains.bsp.protocol.JvmTestEnvironmentResult
import org.jetbrains.bsp.protocol.LibraryItem
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.jetbrains.bsp.protocol.WorkspaceTargetClasspathQueryParams
import java.nio.file.Path

class AspectClientProjectMapper(
  private val languagePluginsService: LanguagePluginsService,
  private val featureFlags: FeatureFlags,
  private val bazelPathsResolver: BazelPathsResolver,
) {
  fun resolveWorkspace(project: AspectBazelMappedProject): BazelResolvedWorkspace =
    BazelResolvedWorkspace(
      targets = mapWorkspaceTargets(project),
      libraries = mapWorkspaceLibraries(project),
      hasError = project.hasError,
    )

  private fun mapWorkspaceTargets(project: AspectBazelMappedProject): BuildTargetCollection {
    val highPrioritySources =
      if (featureFlags.isSharedSourceSupportEnabled) {
        emptySet()
      } else {
        project.modules.asSequence()
          .filter { !it.hasLowSharedSourcesPriority() }
          .flatMap { it.sources }
          .map { it.path }
          .toSet()
      }
    val buildTargets = project.modules.map { it.toBuildTarget(highPrioritySources) }
    val nonModuleTargets =
      project.nonModuleTargets.asSequence()
        .map { it.toBuildTarget() }
        .filter { it.kind.isExecutable } // Filter out non-module targets that would just clutter the ui
        .toList()
    return BuildTargetCollection().apply {
      addBuildTargets(buildTargets)
      addNonModuleTargets(nonModuleTargets)
    }
  }

  private fun mapWorkspaceLibraries(project: AspectBazelMappedProject): List<LibraryItem> {
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
    return libraries
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

  fun dependencySources(project: BazelMappedProject, dependencySourcesParams: DependencySourcesParams): DependencySourcesResult {
    fun getDependencySourcesItem(label: Label): DependencySourcesItem {
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

  suspend fun jvmRunEnvironment(
    server: JoinedBuildServer,
    project: BazelMappedProject,
    params: JvmRunEnvironmentParams,
  ): JvmRunEnvironmentResult {
    val targets = params.targets
    val result = getJvmEnvironmentItems(server, project, targets)
    return JvmRunEnvironmentResult(result)
  }

  suspend fun jvmTestEnvironment(
    server: JoinedBuildServer,
    project: BazelMappedProject,
    params: JvmTestEnvironmentParams,
  ): JvmTestEnvironmentResult {
    val targets = params.targets
    val result = getJvmEnvironmentItems(server, project, targets)
    return JvmTestEnvironmentResult(result)
  }

  private suspend fun getJvmEnvironmentItems(
    server: JoinedBuildServer,
    project: BazelMappedProject,
    targets: List<Label>,
  ): List<JvmEnvironmentItem> {
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
      val classpath = server.workspaceTargetClasspathQuery(WorkspaceTargetClasspathQueryParams(it))
      val resolvedClasspath = resolveClasspath(classpath.runtimeClasspath)
      module?.let { extractJvmEnvironmentItem(module, resolvedClasspath) }
    }
  }

  private fun resolveClasspath(cqueryResult: List<Path>): List<Path> =
    cqueryResult
      .map { bazelPathsResolver.resolveOutput(it) }
      .filter { it.toFile().exists() } // I'm surprised this is needed, but we literally test it in e2e tests

  fun buildJvmBinaryJars(project: BazelMappedProject, params: JvmBinaryJarsParams): JvmBinaryJarsResult {
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

  fun buildTargetJavacOptions(project: BazelMappedProject, params: JavacOptionsParams): JavacOptionsResult {
    val items =
      params.targets
        .mapNotNull { project.findModule(it) }
        .mapNotNull { module -> module.javaModule?.let { toJavacOptionsItem(module, it) } }
    return JavacOptionsResult(items)
  }

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
}
