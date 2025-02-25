package org.jetbrains.bazel.server.sync.languages.kotlin

import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.dependencygraph.DependencyGraph
import org.jetbrains.bazel.server.paths.BazelPathsResolver
import org.jetbrains.bazel.server.sync.languages.LanguagePlugin
import org.jetbrains.bazel.server.sync.languages.SourceRootAndData
import org.jetbrains.bazel.server.sync.languages.java.JavaLanguagePlugin
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetIdentifier
import org.jetbrains.bsp.protocol.KotlinBuildTarget
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.toPath

class KotlinLanguagePlugin(private val javaLanguagePlugin: JavaLanguagePlugin, private val bazelPathsResolver: BazelPathsResolver) :
  LanguagePlugin<KotlinModule>() {
  override fun applyModuleData(moduleData: KotlinModule, buildTarget: BuildTarget) {
    val kotlinBuildTarget = toKotlinBuildTarget(moduleData)
    buildTarget.data = kotlinBuildTarget
  }

  fun toKotlinBuildTarget(kotlinModule: KotlinModule): KotlinBuildTarget {
    val kotlinBuildTarget =
      with(kotlinModule) {
        KotlinBuildTarget(
          languageVersion = languageVersion,
          apiVersion = apiVersion,
          kotlincOptions = kotlincOptions,
          associates = associates.map { BuildTargetIdentifier(it.toString()) }.distinct(),
        )
      }
    kotlinModule.javaModule?.let { javaLanguagePlugin.toJvmBuildTarget(it) }?.let {
      kotlinBuildTarget.jvmBuildTarget = it
    }
    return kotlinBuildTarget
  }

  override fun resolveModule(targetInfo: BspTargetInfo.TargetInfo): KotlinModule? {
    if (!targetInfo.hasKotlinTargetInfo()) return null

    val kotlinTargetInfo = targetInfo.kotlinTargetInfo

    return KotlinModule(
      languageVersion = kotlinTargetInfo.languageVersion,
      apiVersion = kotlinTargetInfo.apiVersion,
      associates = kotlinTargetInfo.associatesList.map { Label.parse(it) },
      kotlincOptions = kotlinTargetInfo.toKotlincOptArguments(),
      javaModule = javaLanguagePlugin.resolveModule(targetInfo),
    )
  }

  private fun BspTargetInfo.KotlinTargetInfo.toKotlincOptArguments(): List<String> = kotlincOptsList + additionalKotlinOpts()

  private fun BspTargetInfo.KotlinTargetInfo.additionalKotlinOpts(): List<String> =
    toKotlincPluginClasspathArguments() + toKotlincPluginOptionArguments()

  private fun BspTargetInfo.KotlinTargetInfo.toKotlincPluginOptionArguments(): List<String> =
    kotlincPluginInfosList
      .flatMap { it.kotlincPluginOptionsList }
      .flatMap { listOf("-P", "plugin:${it.pluginId}:${it.optionValue}") }

  private fun BspTargetInfo.KotlinTargetInfo.toKotlincPluginClasspathArguments(): List<String> =
    kotlincPluginInfosList
      .flatMap { it.pluginJarsList }
      .map { "-Xplugin=${bazelPathsResolver.resolveUri(it).toPath()}" }

  override fun dependencySources(targetInfo: BspTargetInfo.TargetInfo, dependencyGraph: DependencyGraph): Set<URI> =
    javaLanguagePlugin.dependencySources(targetInfo, dependencyGraph)

  override fun calculateSourceRootAndAdditionalData(source: Path): SourceRootAndData =
    javaLanguagePlugin.calculateSourceRootAndAdditionalData(source)
}
