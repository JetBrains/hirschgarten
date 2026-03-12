package org.jetbrains.bazel.sync.workspace.languages.kotlin

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.BzlmodRepoMapping
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.LocalRepositoryMapping
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.commons.getLocalRepositories
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.languages.LanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.LanguagePluginContext
import org.jetbrains.bazel.sync.workspace.languages.java.JavaLanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.jvm.JVMPackagePrefixResolver
import org.jetbrains.bsp.protocol.KotlinBuildTarget
import org.jetbrains.bsp.protocol.SourceItem
import java.nio.file.Path

@ApiStatus.Internal
class KotlinLanguagePlugin(private val javaLanguagePlugin: JavaLanguagePlugin, private val bazelPathsResolver: BazelPathsResolver) :
  LanguagePlugin<KotlinBuildTarget>,
  JVMPackagePrefixResolver {
  override suspend fun createBuildTargetData(context: LanguagePluginContext, target: BspTargetInfo.TargetInfo, repoMapping: RepoMapping): KotlinBuildTarget? {
    if (!target.hasKotlinTargetInfo()) {
      return null
    }
    val kotlinTarget = target.kotlinTargetInfo
    val localRepositories = repoMapping.getLocalRepositories()
    return KotlinBuildTarget(
      languageVersion = kotlinTarget.languageVersion.takeIf { it.isNotBlank() },
      apiVersion = kotlinTarget.apiVersion.takeIf { it.isNotBlank() },
      associates = kotlinTarget.associatesList.map { Label.parse(it) },
      moduleName = kotlinTarget.moduleName.takeIf { it.isNotBlank() },
      kotlincOptions = kotlinTarget.toKotlincOptArguments(localRepositories),
      jvmBuildTarget = javaLanguagePlugin.createBuildTargetData(context, target, repoMapping),
    )
  }

  private fun BspTargetInfo.KotlinTargetInfo.toKotlincOptArguments(localRepositories : LocalRepositoryMapping): List<String> = kotlincOptsList + additionalKotlinOpts(localRepositories)

  private fun BspTargetInfo.KotlinTargetInfo.additionalKotlinOpts(localRepositories : LocalRepositoryMapping): List<String> =
    toKotlincPluginClasspathArguments(localRepositories) + toKotlincPluginOptionArguments()

  private fun BspTargetInfo.KotlinTargetInfo.toKotlincPluginOptionArguments(): List<String> =
    kotlincPluginInfosList
      .flatMap { it.kotlincPluginOptionsList }
      .flatMap { listOf("-P", "plugin:${it.pluginId}:${it.optionValue}") }

  private fun BspTargetInfo.KotlinTargetInfo.toKotlincPluginClasspathArguments(localRepositories : LocalRepositoryMapping): List<String> =
    kotlincPluginInfosList
      .flatMap { it.pluginJarsList }
      .map { "-Xplugin=${bazelPathsResolver.resolve(it, localRepositories)}" }

  override fun getSupportedLanguages(): Set<LanguageClass> = setOf(LanguageClass.KOTLIN)

  override fun resolveJvmPackagePrefix(source: Path): String? = javaLanguagePlugin.resolveJvmPackagePrefix(source)

  override fun transformSources(sources: List<SourceItem>): List<SourceItem> = javaLanguagePlugin.transformSources(sources)
}
