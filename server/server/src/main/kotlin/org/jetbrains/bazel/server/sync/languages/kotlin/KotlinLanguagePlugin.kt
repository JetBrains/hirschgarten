package org.jetbrains.bazel.server.sync.languages.kotlin

import org.jetbrains.bazel.info.KotlinTargetInfo
import org.jetbrains.bazel.info.TargetInfo
import org.jetbrains.bazel.server.dependencygraph.DependencyGraph
import org.jetbrains.bazel.server.paths.BazelPathsResolver
import org.jetbrains.bazel.server.sync.languages.LanguagePlugin
import org.jetbrains.bazel.server.sync.languages.java.JavaLanguagePlugin
import org.jetbrains.bsp.protocol.KotlinBuildTarget
import org.jetbrains.bsp.protocol.RawBuildTarget
import java.nio.file.Path

class KotlinLanguagePlugin(private val javaLanguagePlugin: JavaLanguagePlugin, private val bazelPathsResolver: BazelPathsResolver) :
  LanguagePlugin<KotlinModule>() {
  override fun applyModuleData(moduleData: KotlinModule, buildTarget: RawBuildTarget) {
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
          associates = associates.distinct(),
        )
      }
    kotlinModule.javaModule?.let { javaLanguagePlugin.toJvmBuildTarget(it) }?.let {
      kotlinBuildTarget.jvmBuildTarget = it
    }
    return kotlinBuildTarget
  }

  override fun resolveModule(targetInfo: TargetInfo): KotlinModule? {
    val kotlinTargetInfo = targetInfo.kotlinTargetInfo ?: return null

    return KotlinModule(
      languageVersion = kotlinTargetInfo.languageVersion,
      apiVersion = kotlinTargetInfo.apiVersion,
      associates = kotlinTargetInfo.associates,
      kotlincOptions = kotlinTargetInfo.toKotlincOptArguments(),
      javaModule = javaLanguagePlugin.resolveModule(targetInfo),
    )
  }

  private fun KotlinTargetInfo.toKotlincOptArguments(): List<String> = kotlincOpts + additionalKotlinOpts()

  private fun KotlinTargetInfo.additionalKotlinOpts(): List<String> = toKotlincPluginClasspathArguments() + toKotlincPluginOptionArguments()

  private fun KotlinTargetInfo.toKotlincPluginOptionArguments(): List<String> =
    kotlincPluginInfos
      .flatMap { it.kotlincPluginOptions }
      .flatMap { listOf("-P", "plugin:${it.pluginId}:${it.optionValue}") }

  private fun KotlinTargetInfo.toKotlincPluginClasspathArguments(): List<String> =
    kotlincPluginInfos
      .flatMap { it.pluginJars }
      .map { "-Xplugin=${bazelPathsResolver.resolve(it)}" }

  override fun dependencySources(targetInfo: TargetInfo, dependencyGraph: DependencyGraph): Set<Path> =
    javaLanguagePlugin.dependencySources(targetInfo, dependencyGraph)

  override fun calculateJvmPackagePrefix(source: Path): String? = javaLanguagePlugin.calculateJvmPackagePrefix(source)
}
