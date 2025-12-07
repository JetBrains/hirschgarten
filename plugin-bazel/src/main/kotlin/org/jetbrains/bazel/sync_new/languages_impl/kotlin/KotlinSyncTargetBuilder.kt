package org.jetbrains.bazel.sync_new.languages_impl.kotlin

import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync_new.flow.SyncContext
import org.jetbrains.bazel.sync_new.graph.impl.BazelPath
import org.jetbrains.bazel.sync_new.lang.SyncLanguageDataBuilder
import org.jetbrains.bsp.protocol.RawAspectTarget
import kotlin.collections.plus

class KotlinSyncTargetBuilder : SyncLanguageDataBuilder<KotlinSyncTargetData> {
  override suspend fun init(ctx: SyncContext) {
    /* noop */
  }

  override suspend fun buildTargetData(
    ctx: SyncContext,
    target: RawAspectTarget,
  ): KotlinSyncTargetData? {
    val target = target.target
    if (!target.hasKotlinTargetInfo()) {
      return null
    }
    val kotlinTarget = target.kotlinTargetInfo
    return KotlinSyncTargetData(
      languageVersion = kotlinTarget.languageVersion,
      apiVersion = kotlinTarget.apiVersion,
      associates = kotlinTarget.associatesList.map { Label.parse(it) },
      kotlincOptions = kotlinTarget.toKotlincOptArguments(ctx),
      stdlibJars = kotlinTarget.stdlibsList.map { BazelPath.fromFileLocation(it) },
    )
  }

  private fun BspTargetInfo.KotlinTargetInfo.toKotlincOptArguments(ctx: SyncContext): List<String> {
    return kotlincOptsList + toKotlincPluginClasspathArguments(ctx) + toKotlincPluginOptionArguments()
  }

  private fun BspTargetInfo.KotlinTargetInfo.toKotlincPluginOptionArguments(): List<String> {
    return kotlincPluginInfosList
      .flatMap { it.kotlincPluginOptionsList }
      .flatMap { listOf("-P", "plugin:${it.pluginId}:${it.optionValue}") }
  }

  private fun BspTargetInfo.KotlinTargetInfo.toKotlincPluginClasspathArguments(ctx: SyncContext): List<String> {
    return kotlincPluginInfosList
      .flatMap { it.pluginJarsList }
      .map { "-Xplugin=${ctx.pathsResolver.resolve(it)}" }
  }
}
