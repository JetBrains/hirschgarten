package org.jetbrains.bazel.server.sync

import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.bsp.info.BspInfo
import org.jetbrains.bazel.server.bzlmod.BzlmodRepoMapping
import org.jetbrains.bazel.server.bzlmod.RepoMappingDisabled
import org.jetbrains.bazel.server.model.AspectSyncProject
import org.jetbrains.bazel.server.model.BspMappings
import org.jetbrains.bazel.server.model.Language
import org.jetbrains.bazel.server.model.Module
import org.jetbrains.bazel.server.model.NonModuleTarget
import org.jetbrains.bazel.server.model.Project
import org.jetbrains.bazel.server.model.Tag
import org.jetbrains.bazel.server.paths.BazelPathsResolver
import org.jetbrains.bazel.server.sync.languages.LanguagePluginsService
import org.jetbrains.bazel.server.sync.languages.java.IdeClasspathResolver
import org.jetbrains.bazel.server.sync.languages.java.JavaModule
import org.jetbrains.bazel.server.sync.languages.jvm.javaModule
import org.jetbrains.bazel.server.sync.languages.scala.ScalaModule
import org.jetbrains.bazel.workspacecontext.WorkspaceContextProvider
import org.jetbrains.bsp.protocol.BazelResolveLocalToRemoteParams
import org.jetbrains.bsp.protocol.BazelResolveLocalToRemoteResult
import org.jetbrains.bsp.protocol.BazelResolveRemoteToLocalParams
import org.jetbrains.bsp.protocol.BazelResolveRemoteToLocalResult
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetCapabilities
import org.jetbrains.bsp.protocol.CppOptionsItem
import org.jetbrains.bsp.protocol.CppOptionsParams
import org.jetbrains.bsp.protocol.CppOptionsResult
import org.jetbrains.bsp.protocol.DependencySourcesItem
import org.jetbrains.bsp.protocol.DependencySourcesParams
import org.jetbrains.bsp.protocol.DependencySourcesResult
import org.jetbrains.bsp.protocol.DirectoryItem
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
import org.jetbrains.bsp.protocol.LibraryItem
import org.jetbrains.bsp.protocol.NonModuleTargetsResult
import org.jetbrains.bsp.protocol.PythonOptionsItem
import org.jetbrains.bsp.protocol.PythonOptionsParams
import org.jetbrains.bsp.protocol.PythonOptionsResult
import org.jetbrains.bsp.protocol.ResourcesItem
import org.jetbrains.bsp.protocol.ResourcesParams
import org.jetbrains.bsp.protocol.ResourcesResult
import org.jetbrains.bsp.protocol.ScalacOptionsItem
import org.jetbrains.bsp.protocol.ScalacOptionsParams
import org.jetbrains.bsp.protocol.ScalacOptionsResult
import org.jetbrains.bsp.protocol.SourceItem
import org.jetbrains.bsp.protocol.SourceItemKind
import org.jetbrains.bsp.protocol.SourcesItem
import org.jetbrains.bsp.protocol.SourcesParams
import org.jetbrains.bsp.protocol.SourcesResult
import org.jetbrains.bsp.protocol.WorkspaceBazelRepoMappingResult
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsResult
import org.jetbrains.bsp.protocol.WorkspaceDirectoriesResult
import org.jetbrains.bsp.protocol.WorkspaceGoLibrariesResult
import org.jetbrains.bsp.protocol.WorkspaceInvalidTargetsResult
import org.jetbrains.bsp.protocol.WorkspaceLibrariesResult
import java.io.IOException
import java.net.URI
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.name
import kotlin.io.path.relativeToOrNull
import kotlin.io.path.toPath

class BspProjectMapper(
  private val languagePluginsService: LanguagePluginsService,
  private val workspaceContextProvider: WorkspaceContextProvider,
  private val bazelPathsResolver: BazelPathsResolver,
  private val bazelRunner: BazelRunner,
  private val bspInfo: BspInfo,
) {
  fun workspaceTargets(project: AspectSyncProject): WorkspaceBuildTargetsResult {
    val buildTargets = project.modules.map { it.toBuildTarget() }
    return WorkspaceBuildTargetsResult(buildTargets)
  }

  fun workspaceInvalidTargets(project: AspectSyncProject): WorkspaceInvalidTargetsResult =
    WorkspaceInvalidTargetsResult(project.invalidTargets)

  fun workspaceLibraries(project: AspectSyncProject): WorkspaceLibrariesResult {
    val libraries =
      project.libraries.values.map {
        LibraryItem(
          id = it.label,
          dependencies = it.dependencies.map { dep -> Label.parse(dep.toString()) },
          ijars = it.interfaceJars.map { uri -> uri.toString() },
          jars = it.outputs.map { uri -> uri.toString() },
          sourceJars = it.sources.map { uri -> uri.toString() },
          mavenCoordinates = it.mavenCoordinates,
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

  fun workspaceNonModuleTargets(project: AspectSyncProject): NonModuleTargetsResult {
    val nonModuleTargets =
      project.nonModuleTargets.map {
        it.toBuildTarget()
      }
    return NonModuleTargetsResult(nonModuleTargets)
  }

  fun workspaceDirectories(project: Project): WorkspaceDirectoriesResult {
    val workspaceContext = workspaceContextProvider.currentWorkspaceContext()
    val directoriesSection = workspaceContext.directories

    val symlinksToExclude = computeSymlinksToExclude(project.workspaceRoot)
    val additionalDirectoriesToExclude = computeAdditionalDirectoriesToExclude()
    val directoriesToExclude = directoriesSection.excludedValues + symlinksToExclude + additionalDirectoriesToExclude

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
          canonicalRepoNameToPath =
            repoMapping.canonicalRepoNameToPath.mapValues { (_, path) ->
              path.toUri().toString()
            },
        )
    }
  }

  private fun computeSymlinksToExclude(workspaceRoot: URI): List<Path> {
    val stableSymlinkNames = setOf("bazel-out", "bazel-testlogs", "bazel-bin")
    val workspaceRootPath = workspaceRoot.toPath()
    val workspaceSymlinkNames = setOf("bazel-${workspaceRootPath.name}")

    val symlinks = (stableSymlinkNames + workspaceSymlinkNames).map { workspaceRootPath.resolve(it) }
    val realPaths =
      symlinks.mapNotNull {
        try {
          it.toRealPath()
        } catch (e: IOException) {
          null
        }
      }
    return symlinks + realPaths
  }

  private fun computeAdditionalDirectoriesToExclude(): List<Path> = listOf(bspInfo.bazelBspDir())

  private fun Path.toDirectoryItem() =
    DirectoryItem(
      uri = this.toUri().toString(),
    )

  private fun NonModuleTarget.toBuildTarget(): BuildTarget {
    val languages = languages.flatMap(Language::allNames).distinct()
    val capabilities = inferCapabilities(tags)
    val tags = tags.mapNotNull(BspMappings::toBspTag)
    val baseDirectory = BspMappings.toBspUri(baseDirectory)
    val buildTarget =
      BuildTarget(
        id = label,
        tags = tags,
        languageIds = languages,
        capabilities = capabilities,
        displayName = label.toShortString(),
        baseDirectory = baseDirectory,
        dependencies = emptyList(),
      )
    return buildTarget
  }

  private fun Module.toBuildTarget(): BuildTarget {
    val label = label
    val dependencies =
      directDependencies
    val languages = languages.flatMap(Language::allNames).distinct()
    val capabilities = inferCapabilities(tags)
    val tags = tags.mapNotNull(BspMappings::toBspTag)
    val baseDirectory = BspMappings.toBspUri(baseDirectory)
    val buildTarget =
      BuildTarget(
        id = label,
        tags = tags,
        languageIds = languages,
        dependencies = dependencies,
        capabilities = capabilities,
        displayName = label.toShortString(),
        baseDirectory = baseDirectory,
      )

    applyLanguageData(this, buildTarget)
    return buildTarget
  }

  private fun inferCapabilities(tags: Set<Tag>): BuildTargetCapabilities {
    val canCompile = !tags.contains(Tag.NO_BUILD)
    val canTest = tags.contains(Tag.TEST)
    val canRun = tags.contains(Tag.APPLICATION)
    // Native-BSP debug is not supported with Bazel.
    // It simply means that the `debugSession/start` method should not be called on any Bazel target.
    // Enabling client-side debugging (for example, for JVM targets via JDWP) is up to the client.
    val canDebug = false
    return BuildTargetCapabilities(
      canCompile = canCompile,
      canTest = canTest,
      canRun = canRun,
      canDebug = canDebug,
    )
  }

  private fun applyLanguageData(module: Module, buildTarget: BuildTarget) {
    val plugin = languagePluginsService.getPlugin(module.languages)
    module.languageData?.let { plugin.setModuleData(it, buildTarget) }
  }

  fun sources(project: AspectSyncProject, sourcesParams: SourcesParams): SourcesResult {
    fun toSourcesItem(module: Module): SourcesItem {
      val sourceSet = module.sourceSet
      val sourceItems =
        sourceSet.sources.map {
          SourceItem(
            uri = it.source.toString(),
            kind = SourceItemKind.FILE,
            generated = false,
            jvmPackagePrefix = it.jvmPackagePrefix,
          )
        }
      val generatedSourceItems =
        sourceSet.generatedSources.map {
          SourceItem(
            uri = it.source.toString(),
            kind = SourceItemKind.FILE,
            generated = true,
            jvmPackagePrefix = it.jvmPackagePrefix,
          )
        }
      val sourceRoots = sourceSet.sourceRoots.map(BspMappings::toBspUri)
      val sourcesItem = SourcesItem((module).label, sourceItems + generatedSourceItems, roots = sourceRoots)
      return sourcesItem
    }

    fun emptySourcesItem(label: Label): SourcesItem = SourcesItem(label, emptyList())

    val labels = sourcesParams.targets
    val sourcesItems =
      labels.map {
        project.findModule(it)?.let(::toSourcesItem) ?: emptySourcesItem(it)
      }
    return SourcesResult(sourcesItems)
  }

  fun resources(project: AspectSyncProject, resourcesParams: ResourcesParams): ResourcesResult {
    fun toResourcesItem(module: Module): ResourcesItem {
      val resources = module.resources.map(BspMappings::toBspUri)
      return ResourcesItem(module.label, resources)
    }

    fun emptyResourcesItem(label: Label): ResourcesItem = ResourcesItem(label, emptyList())

    val labels = resourcesParams.targets
    val resourcesItems =
      labels.map {
        project.findModule(it)?.let(::toResourcesItem) ?: emptyResourcesItem(it)
      }
    return ResourcesResult(resourcesItems)
  }

  suspend fun inverseSources(project: AspectSyncProject, inverseSourcesParams: InverseSourcesParams): InverseSourcesResult {
    val documentUri = BspMappings.toUri(inverseSourcesParams.textDocument)
    val documentRelativePath =
      documentUri
        .toPath()
        .relativeToOrNull(project.workspaceRoot.toPath()) ?: throw RuntimeException("File path outside of project root")
    return InverseSourcesQuery.inverseSourcesQuery(documentRelativePath, bazelRunner, project.bazelRelease)
  }

  fun dependencySources(project: AspectSyncProject, dependencySourcesParams: DependencySourcesParams): DependencySourcesResult {
    fun getDependencySourcesItem(label: Label): DependencySourcesItem {
      val sources =
        project
          .findModule(label)
          ?.sourceDependencies
          ?.map(BspMappings::toBspUri)
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

  private suspend fun getJvmEnvironmentItems(project: AspectSyncProject, targets: List<Label>): List<JvmEnvironmentItem> {
    fun extractJvmEnvironmentItem(module: Module, runtimeClasspath: List<URI>): JvmEnvironmentItem? =
      module.javaModule?.let { javaModule ->
        JvmEnvironmentItem(
          module.label,
          runtimeClasspath.map { it.toString() },
          javaModule.jvmOps.toList(),
          bazelPathsResolver.unresolvedWorkspaceRoot().toString(),
          module.environmentVariables,
          mainClasses = javaModule.mainClass?.let { listOf(JvmMainClass(it, javaModule.args)) }.orEmpty(),
        )
      }

    return targets.mapNotNull {
      val module = project.findModule(it)
      val cqueryResult = ClasspathQuery.classPathQuery(it, bspInfo, bazelRunner).runtime_classpath
      val resolvedClasspath = resolveClasspath(cqueryResult)
      module?.let { extractJvmEnvironmentItem(module, resolvedClasspath) }
    }
  }

  fun jvmBinaryJars(project: AspectSyncProject, params: JvmBinaryJarsParams): JvmBinaryJarsResult {
    fun toJvmBinaryJarsItem(module: Module): JvmBinaryJarsItem? =
      module.javaModule?.let { javaModule ->
        val jars = javaModule.binaryOutputs.map { it.toString() }
        JvmBinaryJarsItem(module.label, jars)
      }

    val jvmBinaryJarsItems =
      params.targets.mapNotNull { target ->
        val module = project.findModule(target)
        module?.let { toJvmBinaryJarsItem(it) }
      }
    return JvmBinaryJarsResult(jvmBinaryJarsItems)
  }

  suspend fun buildTargetJavacOptions(project: AspectSyncProject, params: JavacOptionsParams): JavacOptionsResult {
    val items =
      params.targets.collectClasspathForTargetsAndApply(project, false) { module, ideClasspath ->
        module.javaModule?.let { toJavacOptionsItem(module, it, ideClasspath) }
      }
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

  fun buildTargetPythonOptions(project: AspectSyncProject, params: PythonOptionsParams): PythonOptionsResult {
    val modules = BspMappings.getModules(project, params.targets)
    val items = modules.mapNotNull(::extractPythonOptionsItem)
    return PythonOptionsResult(items)
  }

  private fun extractPythonOptionsItem(module: Module): PythonOptionsItem? =
    languagePluginsService.extractPythonModule(module)?.let {
      languagePluginsService.pythonLanguagePlugin.toPythonOptionsItem(module, it)
    }

  suspend fun buildTargetScalacOptions(project: AspectSyncProject, params: ScalacOptionsParams): ScalacOptionsResult {
    val items =
      params.targets.collectClasspathForTargetsAndApply(project, false) { module, ideClasspath ->
        toScalacOptionsItem(module, ideClasspath)
      }
    return ScalacOptionsResult(items)
  }

  private suspend fun <T> List<Label>.collectClasspathForTargetsAndApply(
    project: AspectSyncProject,
    includeClasspath: Boolean,
    mapper: (Module, List<URI>) -> T?,
  ): List<T> =
    this
      .mapNotNull { project.findModule(it) }
      .mapNotNull {
        val classpath = if (includeClasspath) readIdeClasspath(it.label) else emptyList()
        mapper(it, classpath)
      }

  private suspend fun readIdeClasspath(targetLabel: Label): List<URI> {
    val classPathFromQuery = ClasspathQuery.classPathQuery(targetLabel, bspInfo, bazelRunner)
    val ideClasspath =
      IdeClasspathResolver.resolveIdeClasspath(
        label = targetLabel,
        runtimeClasspath = resolveClasspath(classPathFromQuery.runtime_classpath),
        compileClasspath = resolveClasspath(classPathFromQuery.compile_classpath),
      )
    return ideClasspath
  }

  private fun resolveClasspath(cqueryResult: List<String>) =
    cqueryResult
      .map { bazelPathsResolver.resolveOutput(Paths.get(it)) }
      .filter { it.toFile().exists() } // I'm surprised this is needed, but we literally test it in e2e tests
      .map { it.toUri() }

  private fun toScalacOptionsItem(module: Module, ideClasspath: List<URI>): ScalacOptionsItem? =
    (module.languageData as? ScalaModule)?.let { scalaModule ->
      scalaModule.javaModule?.let { javaModule ->
        val javacOptions = toJavacOptionsItem(module, javaModule, ideClasspath)
        ScalacOptionsItem(
          javacOptions.target,
          scalaModule.scalacOpts,
          javacOptions.classpath,
          javacOptions.classDirectory,
        )
      }
    }

  private fun toJavacOptionsItem(
    module: Module,
    javaModule: JavaModule,
    ideClasspath: List<URI>,
  ): JavacOptionsItem =
    JavacOptionsItem(
      module.label,
      javaModule.javacOpts.toList(),
      ideClasspath.map { it.toString() },
      javaModule.mainOutput.toString(),
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
