package org.jetbrains.bazel.server.sync.languages.kotlin

import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.dependencygraph.DependencyGraph
import org.jetbrains.bazel.server.model.Module
import org.jetbrains.bazel.server.paths.BazelPathsResolver
import org.jetbrains.bazel.server.sync.languages.LanguagePlugin
import org.jetbrains.bazel.server.sync.languages.java.JavaLanguagePlugin
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.FastBuildCommand
import org.jetbrains.bsp.protocol.FastBuildParams
import org.jetbrains.bsp.protocol.KotlinBuildTarget
import java.nio.file.Path
import kotlin.io.path.name
import kotlin.io.path.pathString

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
          associates = associates.distinct(),
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
      outputJar = kotlinTargetInfo.outputJarsList.map { Path.of(it) }
    )
  }

  override fun resolveBuilderPath(targetInfo: BspTargetInfo.TargetInfo): String? {
    if (targetInfo.hasKotlinTargetInfo()) {
      return targetInfo.kotlinTargetInfo.builderScript
    }
    return null
  }

  override fun resolveBuilderArgs(targetInfo: BspTargetInfo.TargetInfo): List<String> {
    if (targetInfo.hasKotlinTargetInfo()) {
      return targetInfo.kotlinTargetInfo.builderArgsList
    }
    return emptyList()
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
      .map { "-Xplugin=${bazelPathsResolver.resolve(it)}" }

  override fun dependencySources(targetInfo: BspTargetInfo.TargetInfo, dependencyGraph: DependencyGraph): Set<Path> =
    javaLanguagePlugin.dependencySources(targetInfo, dependencyGraph)

  override fun calculateJvmPackagePrefix(source: Path): String? = javaLanguagePlugin.calculateJvmPackagePrefix(source)

  override fun prepareFastBuild(module: Module, params: FastBuildParams): FastBuildCommand? {
    val languageData = module.languageData
    if (languageData is KotlinModule) {
      val targetJar = bazelPathsResolver.resolve(languageData.outputJar.first())
      val targetParams = targetJar.parent.resolve(targetJar.name + "-0.params")

      val buildOutputJar = params.tempDir.resolve("build.jar")
      val buildParams = KotlinManifestUtil.updateAndWriteCompileParams(targetParams, params.tempDir, buildOutputJar, params.file, bazelPathsResolver.workspaceRoot(), targetJar)

      val builder = bazelPathsResolver.resolve(module.builderPath ?: TODO())
      val args = module.builderArgs
        .plus("--flagfile=${buildParams.pathString}")

      return FastBuildCommand(builder.path, args, buildOutputJar)
    }
    return null
  }
}
