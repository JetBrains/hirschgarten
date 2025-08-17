package org.jetbrains.bazel.sync.workspace.languages.kotlin

import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.languages.LanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.LanguagePluginContext
import org.jetbrains.bazel.sync.workspace.languages.java.JavaLanguagePlugin
import org.jetbrains.bsp.protocol.KotlinBuildTarget
import java.nio.file.Path

class KotlinLanguagePlugin(private val javaLanguagePlugin: JavaLanguagePlugin, private val bazelPathsResolver: BazelPathsResolver) :
  LanguagePlugin<KotlinModule, KotlinBuildTarget> {

  override fun createIntermediateModel(targetInfo: BspTargetInfo.TargetInfo): KotlinModule? {
    if (!targetInfo.hasKotlinTargetInfo()) return null

    val kotlinTargetInfo = targetInfo.kotlinTargetInfo

    return KotlinModule(
      languageVersion = kotlinTargetInfo.languageVersion,
      apiVersion = kotlinTargetInfo.apiVersion,
      associates = kotlinTargetInfo.associatesList.map { Label.parse(it) },
      kotlincOptions = kotlinTargetInfo.toKotlincOptArguments(),
      javaModule = javaLanguagePlugin.createIntermediateModel(targetInfo),
    )
  }

  override fun createBuildTargetData(
    context: LanguagePluginContext,
    ir: KotlinModule,
  ): KotlinBuildTarget {
    return with(ir) {
        KotlinBuildTarget(
          languageVersion = languageVersion,
          apiVersion = apiVersion,
          kotlincOptions = kotlincOptions,
          associates = associates.distinct(),
          jvmBuildTarget = ir.javaModule?.let { javaLanguagePlugin.createBuildTargetData(context, it) }
        )
      }
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

  override fun getSupportedLanguages(): Set<LanguageClass> = setOf(LanguageClass.KOTLIN)

  override fun calculateJvmPackagePrefix(source: Path): String? = javaLanguagePlugin.calculateJvmPackagePrefix(source)
}
