package org.jetbrains.bazel.sync.workspace.languages.kotlin

import com.google.devtools.intellij.ideinfo.IntellijIdeInfo
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.TargetIdeInfo
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.LocalRepositoryMapping
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.commons.getLocalRepositories
import org.jetbrains.bazel.server.BazelServerFacade
import org.jetbrains.bazel.sync.JavaLanguageClass
import org.jetbrains.bazel.sync.workspace.languages.LanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.jvm.KotlinBuildTarget
import org.jetbrains.bazel.sync.workspace.snapshot.SourceFileCollectionBuilder
import org.jetbrains.bazel.sync.workspace.snapshot.toWorkspaceTargetKey
import org.jetbrains.bsp.protocol.BuildTargetData
import kotlin.io.path.exists
import kotlin.reflect.KClass

@ApiStatus.Internal
class KotlinLanguagePlugin : LanguagePlugin {
  override val providedBuildTargetTypes: Set<KClass<out BuildTargetData>>
    get() = setOf(KotlinBuildTarget::class)

  override fun getSupportedLanguages(): Set<LanguageClass> = setOf(JavaLanguageClass.KOTLIN)

  override fun collectUsedLanguages(target: TargetIdeInfo): List<LanguageClass> {
    if (target.hasKotlinTargetInfo()) {
      return listOf(JavaLanguageClass.KOTLIN)
    }
    return emptyList()
  }


  override suspend fun mapBuildTargetData(
    server: BazelServerFacade,
    target: TargetIdeInfo,
    repoMapping: RepoMapping,
  ): List<BuildTargetData> {
    if (!target.hasKotlinTargetInfo()) {
      return emptyList()
    }
    val kotlinTarget = target.kotlinTargetInfo
    val localRepositories = repoMapping.getLocalRepositories()
    val stdlibJars = kotlinTarget.stdlibsList.map { server.bazelPathsResolver.resolve(it, localRepositories) }.distinct()
    val inferredSourceJars = stdlibJars
      .map { it.parent.resolve(it.fileName.toString().replace(".jar", "-sources.jar")) }
      .filter { it.exists() }
    return listOf(
      KotlinBuildTarget(
        languageVersion = kotlinTarget.languageVersion.takeIf { it.isNotBlank() },
        apiVersion = kotlinTarget.apiVersion.takeIf { it.isNotBlank() },
        associates = kotlinTarget.associatedTargetsList.map { it.toWorkspaceTargetKey() },
        moduleName = kotlinTarget.moduleName.takeIf { it.isNotBlank() },
        kotlincOptions = kotlinTarget.toKotlincOptArguments(server, localRepositories),
        stdlibHardLinkedJars = SourceFileCollectionBuilder.build(server.outFileHardLinks.createOutputFileHardLinks(stdlibJars)),
        stdlibInferredSourceJars = SourceFileCollectionBuilder.build(server.outFileHardLinks.createOutputFileHardLinks(inferredSourceJars)),
        exportedCompilerPluginTargetsList = kotlinTarget.exportedCompilerPluginTargetsList.map { it.toWorkspaceTargetKey() }
      ),
    )
  }

  private fun IntellijIdeInfo.KotlinTargetInfo.toKotlincOptArguments(server: BazelServerFacade, localRepositories: LocalRepositoryMapping): List<String> =
    kotlincOptsList + additionalKotlinOpts(server, localRepositories)

  private fun IntellijIdeInfo.KotlinTargetInfo.additionalKotlinOpts(server: BazelServerFacade, localRepositories: LocalRepositoryMapping): List<String> =
    toKotlincPluginClasspathArguments(server, localRepositories) + toKotlincPluginOptionArguments()

  private fun IntellijIdeInfo.KotlinTargetInfo.toKotlincPluginOptionArguments(): List<String> =
    kotlincPluginInfosList
      .flatMap { it.kotlincPluginOptionsList }
      .flatMap { listOf("-P", "plugin:${it.pluginId}:${it.optionValue}") }

  private fun IntellijIdeInfo.KotlinTargetInfo.toKotlincPluginClasspathArguments(server: BazelServerFacade, localRepositories: LocalRepositoryMapping): List<String> =
    kotlincPluginInfosList
      .flatMap { it.pluginJarsList }
      .map { "-Xplugin=${server.bazelPathsResolver.resolve(it, localRepositories)}" }
}
