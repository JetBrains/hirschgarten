package org.jetbrains.bazel.sync.workspace.languages.ultimate

import com.intellij.openapi.diagnostic.logger
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.sync.workspace.languages.LanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.LanguagePluginContext
import org.jetbrains.bsp.protocol.UltimateBuildTarget

private val log = logger<UltimateLanguagePlugin>()

class UltimateLanguagePlugin : LanguagePlugin<UltimateBuildTarget> {
  override fun getSupportedLanguages(): Set<LanguageClass> = setOf(LanguageClass.ULTIMATE)

  override suspend fun createBuildTargetData(context: LanguagePluginContext, target: BspTargetInfo.TargetInfo): UltimateBuildTarget? {
    if (!target.hasUltimateTargetInfo()) return null
    val ultimateInfo = target.ultimateTargetInfo
    if (!ultimateInfo.hasResourceGroup()) return null
    val resources = ultimateInfo.resourceGroup
    val stripPrefix = resources.stripPrefixList.firstOrNull()
    if (resources.stripPrefixList.size > 1) {
      log.warn("Multiple strip prefixes present ${resources.stripPrefixList} but single is expected! Using the first one: ${resources.stripPrefixList[0]}")
    }
    return UltimateBuildTarget(
      resources = resources.filesList.map { context.pathsResolver.resolve(it) },
      stripPrefix = stripPrefix?.let { context.pathsResolver.resolve(it) },
      addPrefix = resources.addPrefix.ifEmpty { null },
    )
  }
}
