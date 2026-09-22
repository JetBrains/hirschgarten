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
import org.jetbrains.bazel.label.label
import org.jetbrains.bazel.languages.projectview.ProjectView
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
import org.jetbrains.bazel.sync.workspace.snapshot.OutputLocationCollectionBuilder
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSyncConfig
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.OutputLocation
import org.jetbrains.bsp.protocol.StrictDependencyCheckedType
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.inputStream
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
        // RC: as you can see we pass `SourceRootOptimizationMode` as `WorkspaceSyncConfig`
        //  property, so can compare it against previous snapshot and assess whatever it has changes
        //  thus performing automatic full importer invalidation
        sourceRootOptimizationMode = SourceRootOptimizationMode.createFromProject(project),
        excludeCompiledSourceCodeInsideJars = BazelFeatureFlags.excludeCompiledSourceCodeInsideJars,
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
      createJavaProviderData(server, target),
      createJavaToolchainData(server, target),
    )
  }

  private suspend fun createJavaProviderData(server: BazelServerFacade, target: TargetIdeInfo): JavaProviderData? {
    val hasProvider = target.javaCommon.jvmTarget ||
                      target.javaProvider.fullCompileJarsCount > 0 ||
                      target.javaProvider.hasApiGeneratingPlugins
    if (!hasProvider) return null
    return JavaProviderData(
      fullCompileJars = OutputLocationCollectionBuilder.build(target.javaProvider.fullCompileJarsList, server.outputParser),
      hasApiGeneratingPlugins = target.javaProvider.hasApiGeneratingPlugins,
    )
  }

  private suspend fun createJavaToolchainData(server: BazelServerFacade, target: TargetIdeInfo): JavaToolchainData? {
    if (!target.hasJavaToolchainInfo()) return null
    val toolchain = target.javaToolchainInfo
    val homePaths = listOf(toolchain.javaHomePath, toolchain.bootClasspathJavaHomePath)
    // an empty path means that the aspect did not set it
    val (javaHome, bootClasspathJavaHome) = server.outputParser.parseExecrootPath(homePaths)
      .zip(homePaths) { location, path -> location.takeIf { path.isNotEmpty() } }
    return JavaToolchainData(
      sourceVersion = toolchain.sourceVersion.takeIf { it.isNotBlank() },
      targetVersion = toolchain.targetVersion.takeIf { it.isNotBlank() },
      javaHome = javaHome,
      bootClasspathJavaHome = bootClasspathJavaHome,
      isExecConfig = toolchain.isExecConfig,
    )
  }

  private suspend fun createJvmBuildTargetData(server: BazelServerFacade, target: TargetIdeInfo, repoMapping: RepoMapping): JvmBuildTarget? {
    if (!target.javaCommon.jvmTarget) {
      return null
    }
    val localRepositories = repoMapping.getLocalRepositories()
    val jvmTarget = target.jvmTargetInfo
    val parser = server.outputParser
    val environmentVariables =
      target.envMap + target.envInheritList.associateWith { EnvironmentUtil.getValue(it) ?: "" }

    val generatedJvmOutputs = target.javaCommon.generatedJarsList.map { gen ->
      JvmOutputs(
        binaryJars = OutputLocationCollectionBuilder.build(gen.binaryJarsList, parser),
        interfaceJars = OutputLocationCollectionBuilder.build(gen.interfaceJarsList, parser),
        sourceJars = OutputLocationCollectionBuilder.build(gen.sourceJarsList, parser),
      )
    }

    return JvmBuildTarget(
      javacOpts = target.javaCommon.javacOptsList.toList(),
      binaryOutputs = OutputLocationCollectionBuilder.build(target.javaCommon.jarsList.flatMap { it.binaryJarsList }, parser),
      environmentVariables = environmentVariables.toMap(),
      mainClass = getMainClass(jvmTarget),
      jvmArgs = jvmTarget.jvmFlagsList.toList(),
      programArgs = jvmTarget.argsList.toList(),
      resolvedResourceStripPrefix = target.resourceStripPrefixLocation(localRepositories),
      outputInterfaceJars = OutputLocationCollectionBuilder.build(target.javaCommon.jarsList.flatMap { it.interfaceJarsList }, parser),
      outputSourceJars = OutputLocationCollectionBuilder.build(target.javaCommon.jarsList.flatMap { it.sourceJarsList }, parser),
      generatedJars = generatedJvmOutputs,
      jdepsJars = createJdepsJars(server, target, localRepositories),
      intellijPluginJars = OutputLocationCollectionBuilder.build(getIntellijPluginJars(target), parser),
      containsInternalJars = target.containsAnyInternalJars(server, localRepositories),
      hasExecutableInfo = target.hasExecutableInfo(),
      checkStrictDependencies = targetChecksStrictDeps(target),
    )
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

  private suspend fun createJdepsJars(
    server: BazelServerFacade,
    targetInfo: TargetIdeInfo,
    localRepositories: LocalRepositoryMapping,
  ): List<JdepsJar> {
    // the absolute path checks the file and gives the synthetic label, the execroot path gives the location
    val jars = dependencyJarsFromJdepsFiles(server, targetInfo, localRepositories)
      .map { it to server.bazelPathsResolver.resolveOutput(Paths.get(it)) }
    val locations = server.outputParser.parseExecrootPath(jars.map { (execrootPath, _) -> execrootPath })
    return jars.zip(locations) { (_, path), location -> JdepsJar(syntheticLabel = syntheticLabel(server, path), jar = location) }
  }

  // returns the execroot paths of the jars
  private fun dependencyJarsFromJdepsFiles(server: BazelServerFacade, targetInfo: TargetIdeInfo, localRepositories: LocalRepositoryMapping): Set<String> =
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
            .map { it.path }
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

  private fun getIntellijPluginJars(targetInfo: TargetIdeInfo): List<ArtifactLocation> {
    // _repackaged_files is created upon calling repackaged_files in rules_intellij
    if (targetInfo.kind != "_repackaged_files") return emptyList()
    return targetInfo.generatedSourcesList
      .filter { it.relativePath.endsWith(".jar") }
      .toList()
  }

  // a prefix in a local repository is external, so the local override resolves it to the local checkout
  private fun TargetIdeInfo.resourceStripPrefixLocation(repositories: LocalRepositoryMapping): OutputLocation? {
    if (!hasJvmTargetInfo()) return null
    val prefix = jvmTargetInfo.resourceStripPrefix.ifEmpty { null } ?: return null
    val label = label()
    if (label is ResolvedLabel && label.repoName in repositories.localRepositories) {
      return OutputLocation.External(repoName = label.repoName, relativePath = prefix)
    }
    return OutputLocation.Workspace(prefix)
  }
}
