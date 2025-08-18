package org.jetbrains.bazel.sync.workspace.languages

import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.sync.workspace.model.VoidLangaugeData
import org.jetbrains.bsp.protocol.VoidBuildTarget

object EmptyLanguagePlugin : LanguagePlugin<VoidLangaugeData, VoidBuildTarget> {
  override fun getSupportedLanguages(): Set<LanguageClass> = setOf()

  override fun createIntermediateModel(targetInfo: BspTargetInfo.TargetInfo): VoidLangaugeData? = VoidLangaugeData

  override fun createBuildTargetData(context: LanguagePluginContext, ir: VoidLangaugeData): VoidBuildTarget? = VoidBuildTarget
}
