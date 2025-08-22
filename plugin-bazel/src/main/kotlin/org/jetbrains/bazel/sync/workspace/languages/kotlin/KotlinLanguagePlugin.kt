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
  LanguagePlugin<KotlinBuildTarget> {
  override suspend fun createBuildTargetData(context: LanguagePluginContext, target: BspTargetInfo.TargetInfo): KotlinBuildTarget? {
    if (!target.hasKotlinTargetInfo()) {
      return null
    }
    val kotlinTarget = target.kotlinTargetInfo
    return KotlinBuildTarget(
      languageVersion = kotlinTarget.languageVersion,
      apiVersion = kotlinTarget.apiVersion,
      associates = kotlinTarget.associatesList.map { Label.parse(it) },
      kotlincOptions = kotlinTarget.toKotlincOptArguments(),
      jvmBuildTarget = javaLanguagePlugin.createBuildTargetData(context, target),
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
      .map { "-Xplugin=${bazelPathsResolver.resolve(it)}" }

  override fun getSupportedLanguages(): Set<LanguageClass> = setOf(LanguageClass.KOTLIN)

  override fun resolveJvmPackagePrefix(source: Path): String? = javaLanguagePlugin.resolveJvmPackagePrefix(source)
}
