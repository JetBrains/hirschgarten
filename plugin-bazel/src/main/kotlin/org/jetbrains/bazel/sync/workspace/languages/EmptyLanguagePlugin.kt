package org.jetbrains.bazel.sync.workspace.languages

import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bsp.protocol.VoidBuildTarget

object EmptyLanguagePlugin : LanguagePlugin<VoidBuildTarget> {
  override fun getSupportedLanguages(): Set<LanguageClass> = setOf()

  override fun createBuildTargetData(context: LanguagePluginContext, target: BspTargetInfo.TargetInfo): VoidBuildTarget? = VoidBuildTarget
}
