package org.jetbrains.bazel.sync.workspace.languages.java

import com.google.devtools.build.lib.view.proto.Deps
import com.google.devtools.intellij.aspect.Common.ArtifactLocation
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.JvmTargetInfo
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.TargetIdeInfo
import com.intellij.openapi.project.Project
import com.intellij.util.EnvironmentUtil
import com.intellij.util.io.DigestUtil
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.LanguageClassService
import org.jetbrains.bazel.commons.LocalRepositoryMapping
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.commons.getLocalRepositories
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.label.assumeResolved
import org.jetbrains.bazel.label.label
import org.jetbrains.bazel.languages.projectview.ProjectView
import org.jetbrains.bazel.languages.projectview.ideJavaHomeOverride
import org.jetbrains.bazel.languages.projectview.importIjars
import org.jetbrains.bazel.languages.projectview.preferClassJarsOverSourcelessJars
import org.jetbrains.bazel.languages.projectview.projectView
import org.jetbrains.bazel.languages.projectview.testSources
import org.jetbrains.bazel.server.BazelServerFacade
import org.jetbrains.bazel.server.model.generatedSourcesList
import org.jetbrains.bazel.server.model.sourcesList
import org.jetbrains.bazel.sync.JavaLanguageClass
import org.jetbrains.bazel.sync.workspace.languages.LanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot.SourceRootOptimizationMode
import org.jetbrains.bazel.sync.workspace.languages.jvm.JavaProviderData
import org.jetbrains.bazel.sync.workspace.languages.jvm.JavaToolchainData
import org.jetbrains.bazel.sync.workspace.languages.jvm.JdepsJar
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmOutputs
import org.jetbrains.bazel.sync.workspace.snapshot.SourceFileCollectionBuilder
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSyncConfig
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.SourceFileCollection
import org.jetbrains.bsp.protocol.StrictDependencyCheckedType
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.inputStream
import kotlin.io.path.name
import kotlin.io.path.relativeToOrSelf
import kotlin.reflect.KClass

@ApiStatus.Internal
class JavaLanguagePlugin : LanguagePlugin {
  override val providedBuildTargetTypes: Set<KClass<out BuildTargetData>>
    get() = setOf(JvmBuildTarget::class, JavaProviderData::class, JavaToolchainData::class)

  override fun getSupportedLanguages(): Set<LanguageClass> = setOf(JavaLanguageClass.JAVA)

  override fun collectUsedLanguages(target: TargetIdeInfo): List<LanguageClass> =
    if (target.javaCommon.jvmTarget) listOf(JavaLanguageClass.JAVA) else emptyList()

  override suspend fun createSyncConfigs(project: Project, projectView: ProjectView): List<WorkspaceSyncConfig> {
    return listOf(
      JavaWorkspaceSyncConfig(
        testSourcesPatterns = projectView.testSources,

        // RC: we bypass `WorkspaceContext` here completely
        importIjars = projectView.importIjars,

        // RC: as you can see we pass `SourceRootOptimizationMode` as `WorkspaceSyncConfig`
        //  property, so can compare it against previous snapshot and assess whatever it has changes
        //  thus performing automatic full importer invalidation
        sourceRootOptimizationMode = SourceRootOptimizationMode.createFromProject(project),
        excludeCompiledSourceCodeInsideJars = BazelFeatureFlags.excludeCompiledSourceCodeInsideJars,
        preferClassJarsOverSourcelessJars = projectView.preferClassJarsOverSourcelessJars,
        ideJavaHomeOverride = projectView.ideJavaHomeOverride,
      ),
    )
  }

  override suspend fun mapBuildTargetData(
    server: BazelServerFacade,
    target: TargetIdeInfo,
    repoMapping: RepoMapping,
  ): List<BuildTargetData> {
    return listOfNotNull(
      createJvmBuildTargetData(server, target, repoMapping),
      createJavaProviderData(server, target, repoMapping),
      createJavaToolchainData(server, target, repoMapping),
    )
  }

  private suspend fun createJavaProviderData(server: BazelServerFacade, target: TargetIdeInfo, repoMapping: RepoMapping): JavaProviderData? {
    val hasProvider = target.javaCommon.jvmTarget ||
                      target.javaProvider.fullCompileJarsCount > 0 ||
                      target.javaProvider.hasApiGeneratingPlugins
    if (!hasProvider) return null
    val localRepositories = repoMapping.getLocalRepositories()
    return JavaProviderData(
      fullCompileJars = target.javaProvider.fullCompileJarsList.toHardlinkedFileCollection(server, localRepositories),
      hasApiGeneratingPlugins = target.javaProvider.hasApiGeneratingPlugins,
    )
  }

  private fun createJavaToolchainData(server: BazelServerFacade, target: TargetIdeInfo, repoMapping: RepoMapping): JavaToolchainData? {
    if (!target.hasJavaToolchainInfo()) return null
    val localRepositories = repoMapping.getLocalRepositories()
    val toolchain = target.javaToolchainInfo
    return JavaToolchainData(
      sourceVersion = toolchain.sourceVersion.takeIf { it.isNotBlank() },
      targetVersion = toolchain.targetVersion.takeIf { it.isNotBlank() },
      javaHome = if (toolchain.hasJavaHome()) server.bazelPathsResolver.resolve(toolchain.javaHome, localRepositories) else null,
      bootClasspathJavaHome =
        if (toolchain.hasBootClasspathJavaHome()) server.bazelPathsResolver.resolve(
          toolchain.bootClasspathJavaHome,
          localRepositories,
        )
        else null,
      isExecConfig = toolchain.isExecConfig,
    )
  }

  private suspend fun createJvmBuildTargetData(server: BazelServerFacade, target: TargetIdeInfo, repoMapping: RepoMapping): JvmBuildTarget? {
    if (!target.javaCommon.jvmTarget) {
      return null
    }
    val baseDirectory = server.bazelPathsResolver.toDirectoryPath(target.label().assumeResolved(), repoMapping)
    val localRepositories = repoMapping.getLocalRepositories()
    val jvmTarget = target.jvmTargetInfo
    val rawBinaryOutputs = target.javaCommon.jarsList.flatMap { it.binaryJarsList }
      .map { server.bazelPathsResolver.resolve(it, localRepositories) }
    val binaryOutputs = server.outFileHardLinks.createOutputFileHardLinks(rawBinaryOutputs)
    val environmentVariables =
      target.envMap + target.envInheritList.associateWith { EnvironmentUtil.getValue(it) ?: "" }

    val hardLinkedInterfaceJars = server.outFileHardLinks.createOutputFileHardLinks(getTargetInterfaceJarsList(server, target, localRepositories))
    val hardLinkedSourceJars = server.outFileHardLinks.createOutputFileHardLinks(getSourceJarPaths(server, target, localRepositories).toList())

    val generatedJvmOutputs = target.javaCommon.generatedJarsList.map { gen ->
      JvmOutputs(
        binaryJars = gen.binaryJarsList.toHardlinkedFileCollection(server, localRepositories),
        interfaceJars = gen.interfaceJarsList.toHardlinkedFileCollection(server, localRepositories),
        sourceJars = gen.sourceJarsList.toHardlinkedFileCollection(server, localRepositories),
      )
    }
    val jdepsJarItems = dependencyJarsFromJdepsFiles(server, target, localRepositories)
      .filterNot { shouldSkipJdepsJar(it) }
      .map { JdepsJar(syntheticLabel = syntheticLabel(server, it), jar = server.outFileHardLinks.createOutputFileHardLink(it) ?: it) }
    val pluginJars = server.outFileHardLinks.createOutputFileHardLinks(getIntellijPluginJars(server, target, localRepositories).toList())

    return JvmBuildTarget(
      javacOpts = target.javaCommon.javacOptsList,
      binaryOutputs = SourceFileCollectionBuilder.build(relativeRoot = baseDirectory, paths = binaryOutputs),
      rawBinaryOutputs = SourceFileCollectionBuilder.build(relativeRoot = baseDirectory, paths = rawBinaryOutputs),
      environmentVariables = environmentVariables,
      mainClass = getMainClass(jvmTarget),
      jvmArgs = jvmTarget.jvmFlagsList,
      programArgs = jvmTarget.argsList,
      resolvedResourceStripPrefix = target.resolveResourceStripPrefixToAbsolutePath(server, localRepositories),
      outputInterfaceJars = SourceFileCollectionBuilder.build(hardLinkedInterfaceJars),
      outputSourceJars = SourceFileCollectionBuilder.build(hardLinkedSourceJars),
      generatedJars = generatedJvmOutputs,
      jdepsJars = jdepsJarItems,
      intellijPluginJars = SourceFileCollectionBuilder.build(pluginJars),
      containsInternalJars = target.containsAnyInternalJars(server, localRepositories),
      hasExecutableInfo = target.hasExecutableInfo(),
      checkStrictDependencies = targetChecksStrictDeps(target),
    )
  }

  private suspend fun Iterable<ArtifactLocation>.toHardlinkedFileCollection(server: BazelServerFacade, localRepositories: LocalRepositoryMapping): SourceFileCollection {
    val paths = this.map { server.bazelPathsResolver.resolve(it, localRepositories) }
    return SourceFileCollectionBuilder.build(paths = server.outFileHardLinks.createOutputFileHardLinks(paths))
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

  private fun getMainClass(jvmTargetInfo: JvmTargetInfo): String? =
    jvmTargetInfo.mainClass.takeUnless { jvmTargetInfo.mainClass.isBlank() }

  private fun dependencyJarsFromJdepsFiles(server: BazelServerFacade, targetInfo: TargetIdeInfo, localRepositories: LocalRepositoryMapping): Set<Path> =
    targetInfo.javaCommon.jdepsList
      .flatMap { jdeps ->
        val path = server.bazelPathsResolver.resolve(jdeps, localRepositories)
        if (path.exists()) {
          val dependencyList =
            path.inputStream().use {
              Deps.Dependencies.parseFrom(it).dependencyList
            }
          dependencyList
            .asSequence()
            .filter { it.isRelevant() }
            .map { server.bazelPathsResolver.resolveOutput(Paths.get(it.path)) }
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

  // See https://github.com/bazel-contrib/rules_jvm_external/issues/786
  private fun shouldSkipJdepsJar(jar: Path): Boolean =
    jar.name.startsWith("header_") && jar.resolveSibling("processed_${jar.name.substring(7)}").exists()

  private val replacementRegex = "[^0-9a-zA-Z]".toRegex()

  private fun syntheticLabel(server: BazelServerFacade, lib: Path): Label {
    val relativeLibPath = lib.relativeToOrSelf(server.bazelPathsResolver.bazelBin())
    val shaOfPath = DigestUtil.sha1Hex(relativeLibPath.toString()).take(7) // just in case of a conflict in filename
    return Label.synthetic(
      lib
        .fileName
        .toString()
        .replace(replacementRegex, "-") + "-" + shaOfPath,
    )
  }

  private fun TargetIdeInfo.containsAnyInternalJars(server: BazelServerFacade, localRepositories: LocalRepositoryMapping) = javaCommon.jarsList.any { jars ->
    jars.sourceJarsList.any {
      !server.bazelPathsResolver.isExternal(
        it,
        localRepositories,
      )
    } && jars.binaryJarsList.any { !server.bazelPathsResolver.isExternal(it, localRepositories) }
  }

  private fun getIntellijPluginJars(server: BazelServerFacade, targetInfo: TargetIdeInfo, localRepositories: LocalRepositoryMapping): Set<Path> {
    // _repackaged_files is created upon calling repackaged_files in rules_intellij
    if (targetInfo.kind != "_repackaged_files") return emptySet()
    return targetInfo.generatedSourcesList.toList()
      .resolvePaths(server, localRepositories)
      .filter { it.extension == "jar" }
      .toSet()
  }

  private fun getSourceJarPaths(server: BazelServerFacade, targetInfo: TargetIdeInfo, localRepositories: LocalRepositoryMapping) =
    targetInfo.javaCommon.jarsList
      .flatMap { it.sourceJarsList }
      .resolvePaths(server, localRepositories)

  private fun getTargetInterfaceJarsList(server: BazelServerFacade, targetInfo: TargetIdeInfo, localRepositories: LocalRepositoryMapping) =
    targetInfo.javaCommon.jarsList
      .flatMap { it.interfaceJarsList }
      .map { server.bazelPathsResolver.resolve(it, localRepositories) }

  private fun List<ArtifactLocation>.resolvePaths(server: BazelServerFacade, localRepositories: LocalRepositoryMapping) =
    map { server.bazelPathsResolver.resolve(it, localRepositories) }.toSet()

  private fun TargetIdeInfo.resolveResourceStripPrefixToAbsolutePath(server: BazelServerFacade, repositories: LocalRepositoryMapping): Path? {
    if (!hasJvmTargetInfo()) return null
    val prefix = jvmTargetInfo.resourceStripPrefix.ifEmpty { null } ?: return null
    val workspaceRoot = server.bazelPathsResolver.workspaceRoot()
    val repoPath = when (val label = label()) {
      is ResolvedLabel -> repositories.localRepositories[label.repoName]?.let(workspaceRoot::resolve) ?: workspaceRoot
      else -> workspaceRoot
    }
    return repoPath.resolve(prefix)
  }
}
